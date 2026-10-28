package br.com.raiz.jardim.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import br.com.raiz.jardim.dto.JardimDto.*;
import br.com.raiz.jardim.service.JardimRegras;
import java.time.LocalDate;
import java.util.*;

@Repository
public class JardimRepository {
    private final JdbcTemplate jdbc;
    public JardimRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Saldo(int moedas,int adubos,long versao,int periodoAtual,String decoracao){ }
    public record Progresso(int periodo,int dia,int adubos,boolean encerrado,Double ams,String nivel){ }
    public Saldo carteira(String id){
        var rows=jdbc.query("SELECT moedas,adubos,versao,periodo_atual,decoracao FROM jardim_carteiras WHERE employee_id=?",
            (r,n)->new Saldo(r.getInt(1),r.getInt(2),r.getLong(3),r.getInt(4),r.getString(5)),id);
        return rows.isEmpty()?new Saldo(0,0,0,1,"nenhuma"):rows.get(0);
    }
    public Saldo bloquear(String id){
        jdbc.update("INSERT INTO jardim_carteiras(employee_id) VALUES (?) ON CONFLICT DO NOTHING",id);
        jdbc.queryForObject("SELECT versao FROM jardim_carteiras WHERE employee_id=? FOR UPDATE",Long.class,id);
        return carteira(id);
    }
    public Progresso progresso(String id,int periodo){
        return progressos(id).stream().filter(p->p.periodo()==periodo).findFirst().orElse(new Progresso(periodo,0,0,false,null,null));
    }
    public List<Progresso> progressos(String id){
        return jdbc.query("SELECT periodo,dia,adubos_aplicados,encerrado,ams_final,nivel_final FROM jardim_periodos WHERE employee_id=? ORDER BY periodo",
            (r,n)->new Progresso(r.getInt(1),r.getInt(2),r.getInt(3),r.getBoolean(4),r.getObject(5)==null?null:r.getDouble(5),r.getString(6)),id);
    }
    public void garantirPeriodo(String id,int periodo){jdbc.update("INSERT INTO jardim_periodos(employee_id,periodo) VALUES (?,?) ON CONFLICT DO NOTHING",id,periodo);}
    public boolean recompensar(String id,int periodo,int dia,LocalDate data,int moedas,Double ams,String nivel){
        return jdbc.update("INSERT INTO jardim_recompensas(employee_id,data,periodo,dia,moedas,ams,nivel) VALUES (?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",id,data,periodo,dia,moedas,ams,nivel)==1;
    }
    public void avancar(String id,int periodo,int dia,int moedas){
        jdbc.update("UPDATE jardim_periodos SET dia=? WHERE employee_id=? AND periodo=?",dia,id,periodo);
        jdbc.update("UPDATE jardim_carteiras SET moedas=moedas+?,versao=versao+1 WHERE employee_id=?",moedas,id);
    }
    public void comprar(String id,String item,int quantidade,int custo){
        jdbc.update("UPDATE jardim_carteiras SET moedas=moedas-?,adubos=adubos+?,versao=versao+1 WHERE employee_id=?",custo,item.equals("adubo")?quantidade:0,id);
        if(!item.equals("adubo"))jdbc.update("INSERT INTO jardim_itens(employee_id,item) VALUES (?,?)",id,item);
    }
    public void aplicar(String id,int periodo,int dia){
        jdbc.update("UPDATE jardim_carteiras SET adubos=adubos-1,versao=versao+1 WHERE employee_id=?",id);
        jdbc.update("UPDATE jardim_periodos SET adubos_aplicados=adubos_aplicados+1 WHERE employee_id=? AND periodo=?",id,periodo);
        jdbc.update("INSERT INTO jardim_adubacoes(employee_id,periodo,dia,quantidade) VALUES (?,?,?,1) ON CONFLICT(employee_id,periodo,dia) DO UPDATE SET quantidade=jardim_adubacoes.quantidade+1",id,periodo,dia);
    }
    public int adubosAte(String id,int periodo,int dia){return jdbc.queryForObject("SELECT coalesce(sum(quantidade),0)::integer FROM jardim_adubacoes WHERE employee_id=? AND periodo=? AND dia<=?",Integer.class,id,periodo,dia);}
    public void equipar(String id,String item){
        Set<String> ativos=new TreeSet<>(Arrays.asList(carteira(id).decoracao().split(",")));ativos.remove("nenhuma");
        if(item.equals("nenhuma"))ativos.clear();else if(!ativos.remove(item))ativos.add(item);
        jdbc.update("UPDATE jardim_carteiras SET decoracao=?,versao=versao+1 WHERE employee_id=?",ativos.isEmpty()?"nenhuma":String.join(",",ativos),id);
    }
    public void zerar(String id,int periodo){
        garantirPeriodo(id,periodo);
        jdbc.update("DELETE FROM jardim_adubacoes WHERE employee_id=? AND periodo=?",id,periodo);
        jdbc.update("DELETE FROM jardim_recompensas WHERE employee_id=? AND periodo=?",id,periodo);
        jdbc.update("UPDATE jardim_periodos SET dia=0,adubos_aplicados=0,encerrado=FALSE,ams_final=NULL,nivel_final=NULL WHERE employee_id=? AND periodo=?",id,periodo);
        jdbc.update("UPDATE jardim_carteiras SET periodo_atual=?,versao=versao+1 WHERE employee_id=?",periodo,id);
    }
    public void encerrar(String id,int periodo,Double ams,String nivel){
        jdbc.update("UPDATE jardim_periodos SET encerrado=TRUE,ams_final=?,nivel_final=? WHERE employee_id=? AND periodo=?",ams,nivel,id,periodo);
        jdbc.update("UPDATE jardim_carteiras SET periodo_atual=?,versao=versao+1 WHERE employee_id=?",Math.min(5,periodo+1),id);
    }
    public Set<String> itens(String id){return new HashSet<>(jdbc.queryForList("SELECT item FROM jardim_itens WHERE employee_id=?",String.class,id));}
    public Set<String> conquistas(String id){return new HashSet<>(jdbc.queryForList("SELECT codigo FROM jardim_conquistas WHERE employee_id=?",String.class,id));}
    public void conquistar(String id,String codigo){jdbc.update("INSERT INTO jardim_conquistas(employee_id,codigo) VALUES (?,?) ON CONFLICT DO NOTHING",id,codigo);}
    public String ultimoNivel(String id){
        var rows=jdbc.queryForList("SELECT nivel FROM jardim_recompensas WHERE employee_id=? AND nivel IS NOT NULL ORDER BY data DESC LIMIT 1",String.class,id);
        return rows.isEmpty()?null:rows.get(0);
    }
    public Set<String> participantes(){return new HashSet<>(jdbc.queryForList("SELECT employee_id FROM employees WHERE active=TRUE AND coalesce(lower(posicao),'')<>'administrador'",String.class));}
    public Map<String,Integer> estagios(int periodo,int dia){
        Map<String,Integer> result=new HashMap<>();
        jdbc.query("SELECT employee_id,sum(quantidade)::integer FROM jardim_adubacoes WHERE periodo=? AND dia<=? GROUP BY employee_id",r->{result.put(r.getString(1),JardimRegras.estagio(r.getInt(2)));},periodo,dia);
        return result;
    }
    public List<Opcao> empresas(){return jdbc.query("SELECT company_id,company_name FROM companies ORDER BY company_name",(r,n)->new Opcao(r.getString(1),r.getString(2)));}
    public List<Opcao> areas(String company){return jdbc.query("SELECT department_id,department_name FROM departments WHERE company_id=? ORDER BY department_name",(r,n)->new Opcao(r.getString(1),r.getString(2)),company);}
}
