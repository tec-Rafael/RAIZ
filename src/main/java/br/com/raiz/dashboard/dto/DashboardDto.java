package br.com.raiz.dashboard.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class DashboardDto {
    private DashboardDto() { }

    public record Pessoa(String id, String nome, String companyId, String empresa,
                         String departmentId, String departamento, String posicao) { }
    public record Opcao(String id, String nome) { }
    public record Periodo(int id, LocalDate inicio, LocalDate fim, int dias, boolean parcial) { }
    public record Filtros(Pessoa usuario, String perfil, List<Periodo> periodos,
                          List<Opcao> areas, List<Pessoa> pessoas, List<Opcao> ferramentas,
                          LocalDate ultimaData, boolean demonstracao) { }
    public record Metricas(long requisicoes, long tokens, long registros, long registrosComLatencia,
                           Double sucesso, Double latencia, long issues, long prs,
                           Double cicloIssuesHoras, Double cicloPrsHoras,
                           long issuesComCiclo, long prsComCiclo) { }
    public record Evolucao(Periodo periodo, Metricas metricas) { }
    public record Distribuicao(String id, String nome, long requisicoes, long tokens) { }
    public record Resumo(Periodo periodo, Metricas metricas, Metricas anterior,
                         List<Evolucao> evolucao, List<Distribuicao> ferramentas,
                         Map<String, Double> scores, boolean scoresIndividuais,
                         LocalDate ultimaData) { }
    public record Linha(String id, String nome, long pessoas, long requisicoes, long tokens,
                        long issues, long prs) { }
    public record Pagina(List<Linha> content, long totalElements, int page, int size) { }
}
