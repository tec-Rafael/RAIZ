package br.com.raiz.assistente.service;

import br.com.raiz.assistente.dto.AssistenteDto.Pergunta;
import br.com.raiz.assistente.dto.AssistenteDto.Resposta;
import br.com.raiz.dashboard.service.AmsDashboardService;
import br.com.raiz.dashboard.service.AmsDashboardService.Consulta;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Assistente do dashboard. Monta o recorte com as MESMAS regras do painel AMS (AmsDashboardService) e envia ao
 * agente orquestrador (Python, REST) só o que o usuário pode ver: ids das pessoas do recorte e o resumo do AMS.
 * O orquestrador usa a LLM configurada nos agentes (Gemini por API ou Ollama local).
 */
@Service
public class AssistenteService {
    private static final Logger log = LoggerFactory.getLogger(AssistenteService.class);
    private static final int TAMANHO_MAXIMO = 500;
    /** No máximo duas perguntas ao mesmo tempo (protege a cota da API e o Ollama local). */
    private final Semaphore vagas = new Semaphore(2);
    private final AmsDashboardService ams;
    private final SessaoService sessoes;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
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
        corpo.put("escopo", escopo(u.employeeId(), u.perfil(), pessoas, p));
        corpo.put("ams", resumoAms(painel, periodo));
        return enviar(corpo);
    }

    static Map<String, Object> escopo(String usuarioId, String perfil, List<Pessoa> pessoas, Pergunta p) {
        Map<String, Object> e = new LinkedHashMap<>();
        if (pessoas.size() == 1) {
            Pessoa pessoa = pessoas.get(0);
            boolean proprio = pessoa.id().equals(usuarioId);
            e.put("tipo", "pessoal");
            e.put("proprio", proprio);
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
