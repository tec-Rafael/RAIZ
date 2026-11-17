package br.com.raiz.assistente.service;

import br.com.raiz.assistente.dto.AssistenteDto.Pergunta;
import br.com.raiz.assistente.dto.AssistenteDto.Resposta;
import br.com.raiz.dashboard.service.AmsDashboardService;
import br.com.raiz.dashboard.service.AmsDashboardService.Consulta;
import br.com.raiz.score.dto.AmsDto.Agregado;
import br.com.raiz.score.dto.AmsDto.Linha;
import br.com.raiz.score.dto.AmsDto.Painel;
import br.com.raiz.score.dto.AmsDto.Pessoa;
import br.com.raiz.usuario.service.SessaoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Assistente do dashboard. Monta o recorte com as MESMAS regras do painel AMS (AmsDashboardService) e envia ao
 * agente orquestrador (Python, REST) só o que o usuário pode ver: ids das pessoas do recorte e o resumo do AMS.
 * O orquestrador usa a LLM configurada nos agentes (Claude por API, Gemini ou Ollama local).
 *
 * Modos (campo "modo"), que definem o que a IA pode responder:
 *  - colaborador: os próprios dados + comparação ANÔNIMA com os colegas de área (médias, contagens, cursos e boas
 *    práticas de quem tem AMS maior). Nenhum id, nome ou nota individual de colega vai no texto do prompt.
 *  - gestor_proprio: o gestor no próprio dashboard individual (igual ao colaborador).
 *  - gestor_colaborador: o gestor no dashboard de um colaborador da área: os dados dessa pessoa.
 *  - gestor_equipe: o gestor no dashboard da equipe: médias da equipe + os dados do próprio gestor ("pessoal").
 *    Para dados de uma pessoa específica, a IA orienta a abrir o dashboard dela.
 *  - administrador / administrador_pessoa: recorte agregado, ou a pessoa selecionada no filtro.
 */
@Service
public class AssistenteService {
    private static final Logger log = LoggerFactory.getLogger(AssistenteService.class);
    private static final int TAMANHO_MAXIMO = 500;
    /** Mínimo de colegas para mostrar uma média ou um grupo de referência sem permitir descobrir uma pessoa. */
    static final int MINIMO_ANONIMO = 3;
    /** No máximo duas perguntas ao mesmo tempo (protege a cota da API e o Ollama local). */
    private final Semaphore vagas = new Semaphore(2);
    private final AmsDashboardService ams;
    private final SessaoService sessoes;
    private final ObjectMapper json;
    /** HTTP/1.1 fixo: com o padrão (HTTP/2), o Java tenta um "upgrade" que o uvicorn (FastAPI) não aceita e o corpo
     *  do POST se perde, o que fazia o agente responder 422. */
    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(5)).build();
    private final String url;
    private final String token;

    public AssistenteService(AmsDashboardService ams, SessaoService sessoes, ObjectMapper json,
                             @Value("${raiz.assistente.url:http://localhost:8001}") String url,
                             @Value("${raiz.assistente.token:raiz-agentes-local}") String token) {
        this.ams = ams; this.sessoes = sessoes; this.json = json;
        this.url = url.replaceAll("/+$", ""); this.token = token;
    }

    public Resposta perguntar(Pergunta p) {
        if (p == null || p.pergunta() == null || p.pergunta().isBlank()) throw erro(HttpStatus.BAD_REQUEST, "Escreva uma pergunta.");
        String pergunta = p.pergunta().strip();
        if (pergunta.length() < 2 || pergunta.length() > TAMANHO_MAXIMO)
            throw erro(HttpStatus.BAD_REQUEST, "A pergunta deve ter entre 2 e " + TAMANHO_MAXIMO + " caracteres.");
        int periodo = p.periodo() == null ? 5 : p.periodo();
        var u = sessoes.atual();
        var consulta = new Consulta(u.perfil(), u.employeeId(), p.companyId(), p.gestorId(), p.departmentId(),
            p.employeeId(), periodo, 0, 1, "", "ams");
        Painel painel = ams.painel(consulta);                       // valida período, filtros e permissões (403)
        List<Pessoa> pessoas = ams.pessoasDaConsulta(consulta);
        if (pessoas.isEmpty()) throw erro(HttpStatus.NOT_FOUND, "Não há pessoas neste recorte.");

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("pergunta", pergunta);
        corpo.put("usuario", Map.of("nome", u.nome(), "perfil", u.perfil()));
        corpo.put("periodo", periodo);
        Map<String, Object> escopo = escopo(u.employeeId(), u.perfil(), pessoas, p);
        String modo = modo(u.perfil(), escopo, p);
        corpo.put("modo", modo);
        corpo.put("escopo", escopo);
        corpo.put("ams", resumoAms(painel, periodo));
        if (modo.equals("gestor_equipe")) {           // a equipe e, à parte, os dados do próprio gestor
            var proprio = new Consulta(u.perfil(), u.employeeId(), null, null, null, u.employeeId(), periodo, 0, 1, "", "ams");
            corpo.put("pessoal", Map.of("employeeIds", List.of(u.employeeId()), "ams", resumoAms(ams.painel(proprio), periodo)));
        }
        if (modo.equals("colaborador") || modo.equals("gestor_proprio")) {
            Map<String, Object> equipe = comparacaoAnonima(painel.resumo().nota(), ams.colegasDeArea(u.employeeId(), periodo));
            if (equipe != null) corpo.put("equipe", equipe);
        }
        return enviar(corpo);
    }

    static String modo(String perfil, Map<String, Object> escopo, Pergunta p) {
        boolean pessoal = "pessoal".equals(escopo.get("tipo")), proprio = Boolean.TRUE.equals(escopo.get("proprio"));
        return switch (perfil) {
            case "colaborador" -> "colaborador";
            case "gestor" -> pessoal ? (proprio ? "gestor_proprio" : "gestor_colaborador") : "gestor_equipe";
            default -> pessoal && !proprio ? "administrador_pessoa" : "administrador";
        };
    }

    /**
     * Comparação com os colegas de área sem identificar ninguém: média e médias por dimensão dos colegas, a posição
     * do usuário em relação à média e os ids do grupo de referência (colegas com AMS maior que o do usuário; sem nota,
     * a metade de cima), que os agentes só usam
     * para calcular médias e contar cursos. Com menos de MINIMO_ANONIMO colegas, não há comparação.
     */
    static Map<String, Object> comparacaoAnonima(Double minhaNota, List<Linha> colegas) {
        if (colegas.size() < MINIMO_ANONIMO) return null;
        Agregado media = AmsDashboardService.agregado(colegas);
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("colegas", colegas.size());
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("nota", media.validos() >= MINIMO_ANONIMO ? media.nota() : null);
        a.put("validos", media.validos());
        List<Map<String, Object>> dims = new ArrayList<>();
        for (var d : media.dimensoes()) {
            if (d.nota() == null || d.validos() < MINIMO_ANONIMO) continue;
            dims.add(Map.of("nome", d.nome(), "nota", d.nota()));
        }
        a.put("dimensoes", dims);
        e.put("ams", a);
        List<Linha> comNota = colegas.stream().filter(l -> l.calculo().nota() != null)
            .sorted(Comparator.comparing((Linha l) -> l.calculo().nota()).reversed()).toList();
        List<Linha> referencia = minhaNota == null ? comNota.subList(0, (comNota.size() + 1) / 2)
            : comNota.stream().filter(l -> l.calculo().nota() > minhaNota).toList();
        e.put("referencia", referencia.size() >= MINIMO_ANONIMO ? referencia.stream().map(l -> l.pessoa().id()).toList() : List.of());
        e.put("colegasIds", colegas.stream().map(l -> l.pessoa().id()).toList());
        return e;
    }

    static Map<String, Object> escopo(String usuarioId, String perfil, List<Pessoa> pessoas, Pergunta p) {
        Map<String, Object> e = new LinkedHashMap<>();
        if (pessoas.size() == 1) {
            Pessoa pessoa = pessoas.get(0);
            boolean proprio = pessoa.id().equals(usuarioId);
            e.put("tipo", "pessoal");
            e.put("proprio", proprio);
            e.put("nome", pessoa.nome());
            e.put("descricao", proprio ? "seus dados" : "dados de " + pessoa.nome());
        } else {
            Pessoa primeira = pessoas.get(0);
            boolean umaEmpresa = pessoas.stream().allMatch(x -> Objects.equals(x.companyId(), primeira.companyId()));
            boolean umaArea = umaEmpresa && pessoas.stream().allMatch(x -> Objects.equals(x.departmentId(), primeira.departmentId()));
            String descricao = perfil.equals("gestor") && blank(p.employeeId())
                ? "sua equipe (" + Objects.toString(primeira.departamento(), "departamento") + ")"
                : umaArea ? "área " + Objects.toString(primeira.departamento(), "sem departamento") + " da empresa " + primeira.empresa()
                : umaEmpresa ? "empresa " + primeira.empresa() : "todas as empresas";
            e.put("tipo", "grupo");
            e.put("descricao", descricao + ", " + pessoas.size() + " pessoas");
        }
        e.put("employeeIds", pessoas.stream().map(Pessoa::id).toList());
        // Empresas do recorte: o agente de conhecimento só busca as políticas dessas empresas.
        e.put("empresas", pessoas.stream().map(Pessoa::companyId).filter(Objects::nonNull).distinct().toList());
        return e;
    }

    static Map<String, Object> resumoAms(Painel painel, int periodo) {
        Map<String, Object> a = new LinkedHashMap<>();
        var resumo = painel.resumo();
        a.put("nota", resumo.nota());
        a.put("notaAnterior", periodo > 1 && painel.evolucao().size() >= periodo - 1 ? painel.evolucao().get(periodo - 2).resumo().nota() : null);
        a.put("pessoas", resumo.pessoas());
        a.put("validos", resumo.validos());
        List<Map<String, Object>> dimensoes = new ArrayList<>();
        if (painel.pessoal() != null) {
            for (var d : painel.pessoal().dimensoes()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("nome", d.nome()); m.put("nota", d.utilizada()); m.put("ausente", d.registrada() == null);
                dimensoes.add(m);
            }
        } else {
            for (var d : resumo.dimensoes()) {
                if (d.nota() == null) continue;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("nome", d.nome()); m.put("nota", d.nota()); m.put("ausente", false);
                dimensoes.add(m);
            }
        }
        a.put("dimensoes", dimensoes);
        return a;
    }

    private Resposta enviar(Map<String, Object> corpo) {
        boolean vaga = false;
        try {
            vaga = vagas.tryAcquire(2, TimeUnit.SECONDS);
            if (!vaga) throw erro(HttpStatus.TOO_MANY_REQUESTS, "O assistente está respondendo outras perguntas. Tente de novo em instantes.");
            var req = HttpRequest.newBuilder(URI.create(url + "/perguntar")).timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json").header("X-Agentes-Token", token)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo))).build();
            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 429) {   // limite do provedor da LLM (ex.: camada gratuita do Gemini)
                String detalhe = json.readTree(r.body()).path("detail").asText("");
                throw erro(HttpStatus.TOO_MANY_REQUESTS, detalhe.isBlank() ? "Muitas perguntas em pouco tempo. Aguarde um minuto e tente de novo." : detalhe);
            }
            if (r.statusCode() != 200) {
                log.warn("Agente orquestrador respondeu {}: {}", r.statusCode(), r.body());
                throw erro(HttpStatus.SERVICE_UNAVAILABLE, "O assistente está indisponível no momento. Tente novamente em instantes.");
            }
            JsonNode n = json.readTree(r.body());
            List<String> fontes = new ArrayList<>();
            n.path("fontes").forEach(f -> fontes.add(f.asText()));
            return new Resposta(n.path("resposta").asText(), fontes, n.path("modelo").asText(null), n.path("tempo_ms").asLong());
        } catch (IOException ex) {
            log.warn("Agente orquestrador inacessível em {}: {}", url, ex.toString());
            throw erro(HttpStatus.SERVICE_UNAVAILABLE, "O assistente está indisponível no momento. Verifique se os agentes de IA estão rodando.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw erro(HttpStatus.SERVICE_UNAVAILABLE, "A pergunta foi interrompida.");
        } finally {
            if (vaga) vagas.release();
        }
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static ResponseStatusException erro(HttpStatus status, String mensagem) { return new ResponseStatusException(status, mensagem); }
}
