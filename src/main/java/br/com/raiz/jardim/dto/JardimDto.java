package br.com.raiz.jardim.dto;

import br.com.raiz.dashboard.dto.DashboardDto.Periodo;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public final class JardimDto {
    private JardimDto(){ }
    public record Avancar(@Min(1) @Max(5) int periodo,@Min(1) @Max(7) int dias,@NotNull @PositiveOrZero Long versao){ }
    public record Aplicar(@Min(1) @Max(5) int periodo,@NotNull @PositiveOrZero Long versao){ }
    public record Adubar(@Min(1) @Max(5) int periodo,@NotNull @PositiveOrZero Long versao,@Min(1) @Max(16) Integer quantidade,@Min(0) @Max(90) Integer dia){
        public Adubar(int periodo,Long versao,Integer quantidade){this(periodo,versao,quantidade,null);}
    }
    public record Comprar(@NotBlank @Size(max=30) String item,@Min(1) @Max(20) int quantidade,@NotNull @PositiveOrZero Long versao){ }
    public record Equipar(@NotBlank @Size(max=30) String item,@NotNull @PositiveOrZero Long versao){ }
    public record Carteira(int moedas,int adubos,long versao,String decoracao){ }
    public record Arvore(Double ams,String nivel,int estagio,int adubosAplicados,int baseAdubo,int metaAdubo,
        int registradas,int aplicaveis,String oportunidade,Double anterior,Double faltamProximoNivel){ }
    public record PeriodoJogo(int periodo,int dia,int dias,int estagio,boolean encerrado,Double ams,String nivel){ }
    public record Conquista(String codigo,String nome,String descricao,boolean conquistada){ }
    public record Item(String id,String nome,String descricao,int preco,boolean adquirido){ }
    public record Estado(String nome,String employeeId,String companyId,String departmentId,String perfil,
        List<Periodo> periodos,int periodoAtual,Periodo periodo,int dia,int diaLiberado,LocalDate data,
        boolean previa,boolean encerrado,boolean podeAvancar,boolean podeAdubar,Carteira carteira,Arvore arvore,
        List<PeriodoJogo> jardim,List<Conquista> conquistas,List<Item> loja){ }
    public record Passo(int dia,LocalDate data,Double ams,String nivel,int registradas,int aplicaveis,int moedas){ }
    public record Atualizacao(Estado estado,List<Passo> passos,int moedasRecebidas,String mensagem){ }
    public record Opcao(String id,String nome){ }
    public record Posicao(int posicao,String nome,Double ams,Double evolucao,String nivel,int estagio,boolean eu){ }
    public record Floresta(int pessoas,int comDados,int semDados,Double ams,String nivel,List<Posicao> arvores){ }
    public record Ranking(String escopo,String ordem,int periodo,int dia,String criterio,List<Posicao> destaques,
        Posicao minhaPosicao,Floresta floresta,List<Opcao> empresas,List<Opcao> areas,String companyId,String departmentId){ }
}
