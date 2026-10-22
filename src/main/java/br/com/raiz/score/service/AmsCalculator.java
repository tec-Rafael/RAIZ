package br.com.raiz.score.service;

import br.com.raiz.score.dto.AmsDto.*;
import java.text.Normalizer;
import java.util.*;

/** Regras puras: mantém a nota registrada separada do zero aplicado a AI Literacy. */
public final class AmsCalculator {
    private AmsCalculator() { }
    public static boolean desenvolvedor(String cargo) {
        if (cargo == null) return false;
        String c = Normalizer.normalize(cargo,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
        return c.contains("desenvolvedor") || c.contains("developer") || c.contains("software engineer")
            || c.contains("engenheiro(a) de software") || c.contains("engenheiro de software");
    }
    public static Calculo calcular(boolean dev, Double seguranca, Double governanca, Double finops,
                                    Double literacy, Double delivery, List<Topico> topicos) {
        var ids = dev ? List.of("security","governance","finops","literacy","delivery")
                      : List.of("security","governance","finops","literacy");
        var nomes = Map.of("security","Segurança","governance","Governança","finops","FinOps",
            "literacy","AI Literacy","delivery","Delivery");
        Map<String,Double> notas = new HashMap<>();
        notas.put("security",validar(seguranca)); notas.put("governance",validar(governanca));
        notas.put("finops",validar(finops)); notas.put("literacy",validar(literacy)); notas.put("delivery",dev ? validar(delivery) : null);
        boolean algumRegistrado = ids.stream().anyMatch(id -> notas.get(id) != null);
        int disponiveis = (int) ids.stream().filter(id -> notas.get(id) != null || id.equals("literacy")).count();
        List<Dimensao> dimensoes = new ArrayList<>(); double total = 0;
        for (String id : ids) {
            Double registrada = notas.get(id);
            Double utilizada = registrada == null && id.equals("literacy") ? Double.valueOf(0) : registrada;
            double peso = utilizada == null || !algumRegistrado ? 0 : 1.0/disponiveis;
            Double contribuicao = peso == 0 ? null : utilizada * peso;
            if (contribuicao != null) total += contribuicao;
            dimensoes.add(new Dimensao(id,nomes.get(id),registrada,utilizada,1.0/ids.size(),peso,contribuicao,
                registrada != null ? "registrada" : id.equals("literacy") ? "zero_por_ausencia" : "sem_nota"));
        }
        return new Calculo(dev,algumRegistrado ? total : null,List.copyOf(dimensoes),dev ? List.copyOf(topicos) : List.of(),
            (int)ids.stream().filter(id -> notas.get(id) == null).count(),ids.size());
    }
    public static Double validar(Double nota) {
        if (nota == null) return null;
        if (!Double.isFinite(nota) || nota < 0 || nota > 100) throw new IllegalArgumentException("Nota deve estar entre 0 e 100.");
        return nota;
    }
}
