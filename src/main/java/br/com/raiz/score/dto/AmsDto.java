package br.com.raiz.score.dto;

import br.com.raiz.dashboard.dto.DashboardDto.Periodo;
import java.util.List;

public final class AmsDto {
    private AmsDto() { }
    public record Dimensao(String id, String nome, Double registrada, Double utilizada,
                           double pesoInicial, double pesoEfetivo, Double contribuicao, String estado) { }
    public record Topico(String id, String nome, Double notaIndividual, Double notaRelativa,
                         double pesoInicial, double pesoIndividual, double pesoRelativo) { }
    public record Calculo(boolean desenvolvedor, Double nota, List<Dimensao> dimensoes,
                          List<Topico> delivery, int notasAusentes, int dimensoesAplicaveis) { }
    public record Pessoa(String id, String nome, String companyId, String empresa,
                         String departmentId, String departamento, String posicao, String cargo) { }
    public record Filtros(Pessoa usuario, List<Pessoa> pessoas, List<Periodo> periodos) { }
    public record Media(String id, String nome, Double nota, int validos, int ausentes, int zerosLiteracy) { }
    public record Destaque(String id, String nome, Double nota, int empatados) { }
    public record Agregado(Double nota, int pessoas, int validos, int semAms, int notasAusentes,
                            int dimensoesAplicaveis, List<Media> dimensoes, Destaque melhor) { }
    public record Ponto(Periodo periodo, Agregado resumo, Calculo pessoal) { }
    public record Grupo(String id, String nome, List<Ponto> evolucao) { }
    public record Linha(Pessoa pessoa, Calculo calculo) { }
    public record Individual(Periodo periodo, Pessoa pessoa, Calculo calculo) { }
    public record Pagina(List<Linha> content, int totalElements, int page, int size) { }
    public record Faixa(String nome, int quantidade) { }
    public record Painel(Periodo periodo, Agregado resumo, Calculo pessoal, List<Ponto> evolucao,
                         List<Grupo> empresas, List<Grupo> areas, List<Ponto> evolucaoDestaque,
                         Pagina integrantes, List<Faixa> distribuicao, String criterio) { }
}
