package br.com.raiz.jardim.service;

import br.com.raiz.jardim.dto.JardimDto.*;
import br.com.raiz.jardim.repository.JardimRepository;
import br.com.raiz.jardim.repository.JardimRepository.Saldo;
import br.com.raiz.jardim.repository.JardimRepository.Progresso;
import br.com.raiz.usuario.service.SessaoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static br.com.raiz.jardim.service.JardimRegras.*;
import static br.com.raiz.jardim.service.JardimAmsService.erro;

@Service
public class JardimService {
    private final JardimRepository repo;
    private final JardimAmsService ams;
    private final SessaoService sessoes;
    public JardimService(JardimRepository repo,JardimAmsService ams,SessaoService sessoes){this.repo=repo;this.ams=ams;this.sessoes=sessoes;}
    @Transactional(readOnly=true)
    public Estado estado(Integer periodo,Integer dia){
        var u=sessoes.atual();var saldo=repo.carteira(u.employeeId());
        int numero=periodo==null?saldo.periodoAtual():periodo;
        var p=ams.periodo(numero);var progresso=repo.progresso(u.employeeId(),numero);
        int d=dia==null?progresso.dia():dia;
        if(d<0||d>p.dias())throw erro("Dia fora do período.");
        var nota=ams.minha(u.employeeId(),numero,d);
        boolean previa=d!=progresso.dia()||numero!=saldo.periodoAtual();
        int usados=repo.adubosAte(u.employeeId(),numero,d);
        Double anterior=null;
        if(numero>1&&d>0&&nota!=null&&nota.nivel()!=null){
            var anteriorNota=ams.minha(u.employeeId(),numero-1,Math.min(d,ams.periodo(numero-1).dias()));
            if(anteriorNota!=null&&anteriorNota.nivel()!=null)anterior=anteriorNota.ams();
        }
        Double faltam=nota==null||nota.nivel()==null?null:nota.ams()<60?60-nota.ams():nota.ams()<70?70-nota.ams():nota.ams()<85?85-nota.ams():0;
        var arvore=new Arvore(nota==null?null:nota.ams(),nota==null?null:nota.nivel(),estagio(usados),usados,baseAdubo(usados),metaAdubo(usados),
            nota==null?0:nota.registradas(),nota==null?0:nota.aplicaveis(),nota==null?null:nota.oportunidade(),anterior,faltam);
        var registros=repo.progressos(u.employeeId());
        var jardim=ams.periodos().stream().map(j->{
            var r=registros.stream().filter(x->x.periodo()==j.id()).findFirst().orElse(new Progresso(j.id(),0,0,false,null,null));
            return new PeriodoJogo(j.id(),r.dia(),j.dias(),estagio(r.adubos()),r.encerrado(),r.ams(),r.nivel()==null?null:nivel(r.ams()));
        }).toList();
        return new Estado(u.nome(),u.employeeId(),u.companyId(),u.departmentId(),u.perfil(),ams.periodos(),saldo.periodoAtual(),p,d,progresso.dia(),
            d==0?null:p.inicio().plusDays(d-1),previa,progresso.encerrado(),!previa&&!progresso.encerrado()&&d<p.dias(),
            saldo.adubos()>0&&usados<16,
            new Carteira(saldo.moedas(),saldo.adubos(),saldo.versao(),saldo.decoracao()),arvore,jardim,conquistas(repo.conquistas(u.employeeId())),loja(repo.itens(u.employeeId())));
    }
    @Transactional
    public Atualizacao avancar(Avancar request){
        var u=sessoes.atual();var saldo=bloquear(request.versao());
        var p=ams.periodo(request.periodo());var progresso=atual(saldo,request.periodo());
        repo.garantirPeriodo(u.employeeId(),p.id());
        int fim=Math.min(p.dias(),progresso.dia()+request.dias()),ganho=0;
        List<Passo> passos=new ArrayList<>();String anterior=repo.ultimoNivel(u.employeeId());
        for(int d=progresso.dia()+1;d<=fim;d++){
            var n=ams.minha(u.employeeId(),p.id(),d);int recompensa=moedas(n.nivel());
            var data=p.inicio().plusDays(d-1);
            boolean nova=repo.recompensar(u.employeeId(),p.id(),d,data,recompensa,n.ams(),n.nivel());
            if(nova)ganho+=recompensa;
            passos.add(new Passo(d,data,n.ams(),n.nivel(),n.registradas(),n.aplicaveis(),nova?recompensa:0));
            if("excelente".equals(n.nivel()))repo.conquistar(u.employeeId(),"primeiro_verao");
            if(n.registradas()==n.aplicaveis())repo.conquistar(u.employeeId(),"todas_dimensoes");
            if("baixo".equals(anterior)&&n.nivel()!=null&&!"baixo".equals(n.nivel()))repo.conquistar(u.employeeId(),"recuperacao");
            if(n.nivel()!=null)anterior=n.nivel();
        }
        if(fim>progresso.dia())repo.avancar(u.employeeId(),p.id(),fim,ganho);
        return new Atualizacao(estado(p.id(),null),passos,ganho,passos.isEmpty()?"Todos os dias deste período já foram percorridos.":"Você percorreu "+passos.size()+" dias históricos e recebeu "+ganho+" moedas.");
    }
    @Transactional
    public Atualizacao comprar(Comprar request){
        var saldo=bloquear(request.versao());String item=request.item();int preco=preco(item);
        if(preco==0)throw erro("Item inexistente.");
        if(!item.equals("adubo")&&(request.quantidade()!=1||repo.itens(sessoes.atual().employeeId()).contains(item)))throw conflito("Essa decoração já foi adquirida ou a quantidade é inválida.");
        int custo=preco*request.quantidade();
        if(saldo.moedas()<custo)throw conflito("Moedas insuficientes.");
        repo.comprar(sessoes.atual().employeeId(),item,request.quantidade(),custo);
        return resposta("Compra realizada. Adubo altera apenas o crescimento, nunca o AMS.");
    }
    @Transactional
    public Atualizacao aplicar(Aplicar request){return aplicar(new Adubar(request.periodo(),request.versao(),1));}
    @Transactional
    public Atualizacao aplicar(Adubar request){
        var saldo=bloquear(request.versao());var periodo=ams.periodo(request.periodo());
        var p=repo.progresso(sessoes.atual().employeeId(),request.periodo());
        repo.garantirPeriodo(sessoes.atual().employeeId(),request.periodo());
        int dia=request.dia()==null?p.dia():request.dia();
        if(dia<0||dia>periodo.dias())throw erro("Dia fora do período.");
        int quantidade=request.quantidade()==null?1:request.quantidade();
        if(quantidade<1||quantidade>16)throw erro("Quantidade de adubo inválida.");
        if(saldo.adubos()<quantidade)throw conflito("Você não tem adubo no inventário.");
        if(p.adubos()+quantidade>16)throw conflito("Esta árvore já atingiu o estágio máximo.");
        for(int i=0;i<quantidade;i++)repo.aplicar(sessoes.atual().employeeId(),p.periodo(),dia);
        boolean cresceu=estagio(p.adubos()+quantidade)>estagio(p.adubos());
        if(cresceu)repo.conquistar(sessoes.atual().employeeId(),"primeiro_crescimento");
        return new Atualizacao(estado(request.periodo(),dia),List.of(),0,cresceu?"Sua árvore cresceu! A saúde continua determinada pelo AMS.":"Adubo aplicado. Você está mais perto do próximo estágio.");
    }
    @Transactional
    public Atualizacao zerar(Aplicar request){
        bloquear(request.versao());ams.periodo(request.periodo());
        repo.zerar(sessoes.atual().employeeId(),request.periodo());
        return new Atualizacao(estado(request.periodo(),null),List.of(),0,"Progresso reiniciado. Seu saldo foi mantido e você pode ganhar mais moedas ao percorrer o período novamente.");
    }
    @Transactional
    public Atualizacao equipar(Equipar request){
        bloquear(request.versao());
        if(!request.item().equals("nenhuma")&&!repo.itens(sessoes.atual().employeeId()).contains(request.item()))throw conflito("Adquira essa decoração antes de equipar.");
        repo.equipar(sessoes.atual().employeeId(),request.item());return resposta("Decoração atualizada.");
    }
    @Transactional
    public Atualizacao encerrar(Aplicar request){
        var saldo=bloquear(request.versao());var p=atual(saldo,request.periodo());var periodo=ams.periodo(p.periodo());
        if(p.dia()!=periodo.dias())throw conflito("Percorra todos os dias antes de guardar esta árvore.");
        var nota=ams.minha(sessoes.atual().employeeId(),p.periodo(),p.dia());
        repo.encerrar(sessoes.atual().employeeId(),p.periodo(),nota.ams(),nota.nivel());
        repo.conquistar(sessoes.atual().employeeId(),"primeira_arvore");
        var concluidos=repo.progressos(sessoes.atual().employeeId()).stream().filter(Progresso::encerrado).toList();
        if(concluidos.size()>=3){
            var ultimos=concluidos.subList(concluidos.size()-3,concluidos.size());
            if(ultimos.stream().allMatch(x->x.nivel()!=null)&&ultimos.get(0).ams()<ultimos.get(1).ams()&&ultimos.get(1).ams()<ultimos.get(2).ams())repo.conquistar(sessoes.atual().employeeId(),"tres_periodos");
        }
        return resposta(p.periodo()==5?"Seu jardim histórico está completo. Você pode revisitar suas árvores.":"Árvore guardada no jardim. Uma nova muda espera no próximo período.");
    }
    private Saldo bloquear(Long versao){
        var saldo=repo.bloquear(sessoes.atual().employeeId());
        if(versao==null||versao!=saldo.versao())throw conflito("Seu jardim foi atualizado em outra ação. Atualize a página antes de continuar.");
        return saldo;
    }
    private Progresso atual(Saldo saldo,int periodo){
        if(periodo!=saldo.periodoAtual())throw conflito("Revisitar períodos não libera recompensas nem altera árvores arquivadas.");
        var p=repo.progresso(sessoes.atual().employeeId(),periodo);
        if(p.encerrado())throw conflito("Esta árvore já está guardada no jardim.");
        return p;
    }
    private Atualizacao resposta(String mensagem){return new Atualizacao(estado(null,null),List.of(),0,mensagem);}
    private static ResponseStatusException conflito(String msg){return new ResponseStatusException(HttpStatus.CONFLICT,msg);}
    private static List<Item> loja(Set<String> adquiridos){return List.of(
        new Item("adubo","Adubo","Avança o crescimento; não altera o AMS.",10,false),
        new Item("flores","Canteiro de flores","Flores pixeladas para o seu jardim.",30,adquiridos.contains("flores")),
        new Item("pedras","Caminho de pedras","Pequenas pedras ao redor da árvore.",20,adquiridos.contains("pedras")),
        new Item("cogumelos","Cogumelos","Um toque de cor para o seu jardim.",40,adquiridos.contains("cogumelos")),
        new Item("cerca","Cerca de madeira","Uma cerca rústica para emoldurar seu jardim.",35,adquiridos.contains("cerca")),
        new Item("colmeia","Colmeia do jardim","Uma pequena casa para as abelhas.",50,adquiridos.contains("colmeia")),
        new Item("placa","Placa do jardim","Um detalhe acolhedor ao lado da árvore.",25,adquiridos.contains("placa")));}
    private static List<Conquista> conquistas(Set<String> obtidas){return List.of(
        new Conquista("primeiro_crescimento","Criando raízes","Alcance o segundo estágio de uma árvore.",obtidas.contains("primeiro_crescimento")),
        new Conquista("primeiro_verao","Primeiro verão","Alcance AMS excelente em um dia com cobertura suficiente.",obtidas.contains("primeiro_verao")),
        new Conquista("todas_dimensoes","Visão completa","Tenha notas registradas em todas as dimensões aplicáveis.",obtidas.contains("todas_dimensoes")),
        new Conquista("recuperacao","Novos brotos","Saia do nível baixo para uma faixa superior.",obtidas.contains("recuperacao")),
        new Conquista("primeira_arvore","Meu primeiro jardim","Guarde uma árvore após percorrer o período.",obtidas.contains("primeira_arvore")),
        new Conquista("tres_periodos","Sempre em evolução","Conclua três períodos consecutivos com AMS crescente e cobertura suficiente.",obtidas.contains("tres_periodos")));}
}
