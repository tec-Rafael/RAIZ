package br.com.raiz.usuario.config;

import br.com.raiz.dashboard.controller.*;
import br.com.raiz.dashboard.dto.DashboardDto;
import br.com.raiz.dashboard.service.*;
import br.com.raiz.ia.controller.IaController;
import br.com.raiz.ia.dto.UtilizacaoIADto;
import br.com.raiz.ia.service.UtilizacaoIAService;
import br.com.raiz.jardim.controller.JardimController;
import br.com.raiz.jardim.dto.JardimDto;
import br.com.raiz.jardim.service.*;
import br.com.raiz.score.controller.AiMaturityScoreController;
import br.com.raiz.score.dto.*;
import br.com.raiz.score.service.*;
import br.com.raiz.usuario.controller.FuncionarioController;
import br.com.raiz.usuario.domain.Posicao;
import br.com.raiz.usuario.dto.FuncionarioDto;
import br.com.raiz.usuario.service.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** BDD executável: Dado (given), Quando (MockMvc), Então (assertivas/then).
 * Controllers, interceptor e Bean Validation reais; serviços substituídos por mocks.
 * A autorização nos serviços é exercitada nas suítes próprias, não simulada como cobertura aqui.
 */
class RotasBddTest {
    MockMvc mvc;
    SessaoService sessoes;
    FuncionarioService funcionarios;
    ConsumoPerfilService consumo;
    UtilizacaoIAService ia;
    DashboardService dashboard;
    AmsDashboardService painel;
    JardimService jardim;
    JardimRankingService ranking;
    AiMaturityScoreService ams;
    AiLiteracyService literacy;
    GovernanceService governance;
    SecurityIncidentService security;
    FinOpsService finops;
    DeliveryMetricsService delivery;

    static SessaoService.Usuario usuario(Posicao cargo) {
        return new SessaoService.Usuario("E1", "Pessoa", "pessoa@raiz.test", cargo,
            "C1", "D1", "Developer", Instant.parse("2030-01-01T00:00:00Z"));
    }
    @BeforeEach void dadoApiIsoladaComInterceptorReal() {
        sessoes=mock(SessaoService.class); funcionarios=mock(FuncionarioService.class);
        consumo=mock(ConsumoPerfilService.class); ia=mock(UtilizacaoIAService.class);
        dashboard=mock(DashboardService.class); painel=mock(AmsDashboardService.class);
        jardim=mock(JardimService.class); ranking=mock(JardimRankingService.class);
        ams=mock(AiMaturityScoreService.class); literacy=mock(AiLiteracyService.class);
        governance=mock(GovernanceService.class); security=mock(SecurityIncidentService.class);
        finops=mock(FinOpsService.class); delivery=mock(DeliveryMetricsService.class);
        given(sessoes.validar(any())).willAnswer(inv -> {
            String token=inv.getArgument(0);
            if (token==null || token.equals("expirado")) throw SessaoService.naoAutorizado();
            return usuario(switch(token) {
                case "admin" -> Posicao.ADMINISTRADOR;
                case "gestor" -> Posicao.GESTOR;
                default -> Posicao.COLABORADOR;
            });
        });
        // Usa o usuário colocado pelo interceptor no request, sem ThreadLocal próprio do teste.
        willCallRealMethod().given(sessoes).atual();
        willCallRealMethod().given(sessoes).exigirAdmin();
        mvc=MockMvcBuilders.standaloneSetup(
            new FuncionarioController(funcionarios,sessoes,consumo,false),
            new IaController(ia,sessoes), new DashboardController(dashboard,sessoes),
            new AmsDashboardController(painel,sessoes), new JardimController(jardim,ranking),
            new AiMaturityScoreController(literacy,governance,security,finops,delivery,ams))
            .addInterceptors(new AutenticacaoConfig(sessoes,"http://localhost:3000").interceptor()).build();
    }
    MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder req,String perfil) {
        return req.cookie(new Cookie("usuario",perfil));
    }
    MockHttpServletRequestBuilder json(String rota,String body) {
        return post(rota).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    record Rota(String metodo,String caminho,String corpo,int sucesso) {
        MockHttpServletRequestBuilder request() {
            var req=org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .request(HttpMethod.valueOf(metodo),caminho);
            return corpo==null?req:req.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        @Override public String toString(){return metodo+" "+caminho;}
    }
    // Manifesto explícito: cada rota tem um cenário autenticado e outro sem cookie.
    static Stream<Rota> rotasProtegidas() {
        var r=new ArrayList<Rota>();
        for(String path:List.of("/api/funcionario/sessao","/api/funcionario/me/consumo-ia",
            "/api/funcionario/listar","/api/funcionario/buscar/E1","/api/utilizacao-ia/listar",
            "/api/utilizacao-ia/destaques","/api/utilizacao-ia/buscar/U1","/api/dashboard/usuarios",
            "/api/dashboard/filtros","/api/dashboard/resumo","/api/dashboard/integrantes",
            "/api/dashboard/ams/usuarios","/api/dashboard/ams/filtros","/api/dashboard/ams",
            "/api/jardim/estado","/api/jardim/ranking?periodo=1&dia=1")) r.add(new Rota("GET",path,null,200));
        for(String dimensao:List.of("ams","ai-literacy","governance","security-incidents","finops","delivery"))
            r.add(new Rota("GET","/score/"+dimensao+"/E1?periodo=1",null,200));
        r.add(new Rota("POST","/api/funcionario/criar","{\"gestorId\":\"E1\",\"nome\":\"Novo\",\"email\":\"novo@raiz.test\",\"senha\":\"123456\"}",201));
        r.add(new Rota("DELETE","/api/funcionario/deletar/E2",null,204));
        r.add(new Rota("POST","/api/utilizacao-ia/criar","{\"usageId\":\"U1\"}",201));
        r.add(new Rota("DELETE","/api/utilizacao-ia/deletar/U1",null,204));
        for(String operacao:List.of("avancar","comprar","adubar","zerar","equipar","encerrar")) {
            String body=switch(operacao) {
                case "avancar" -> "{\"periodo\":1,\"dias\":1,\"versao\":0}";
                case "comprar" -> "{\"item\":\"adubo\",\"quantidade\":1,\"versao\":0}";
                case "equipar" -> "{\"item\":\"pedras\",\"versao\":0}";
                default -> "{\"periodo\":1,\"versao\":0}";
            };
            r.add(new Rota("POST","/api/jardim/"+operacao,body,200));
        }
        return r.stream();
    }
    void dadoRetornosDosServicos() {
        given(ia.buscarPorId("U1")).willReturn(new UtilizacaoIADto.Response("U1",null,"E1",null,null,null,null,null,1,2,3,null,null,null,null));
        given(dashboard.filtros(anyString(),anyString())).willReturn(new DashboardDto.Filtros(null,"administrador",List.of(),List.of(),List.of(),List.of(),null,true));
        given(painel.filtros(anyString(),anyString())).willReturn(new AmsDto.Filtros(null,List.of(),List.of()));
        var periodo=new br.com.raiz.score.domain.Periodo(LocalDate.of(2025,1,1),LocalDate.of(2025,3,31));
        given(ams.obterPeriodo(1)).willReturn(periodo);
        given(literacy.calcularAiLiteracy("E1",periodo)).willReturn(new AiLiteracyScoreDto(80.0,80.0,80.0,80.0));
        given(governance.calcularGovernanceDetalhado("E1",periodo,1)).willReturn(new GovernanceScoreDto("E1",new PeriodoResponseDto(1,periodo),10L,8L,80.0));
        given(finops.calcularFinOpsScore("E1",periodo)).willReturn(new FinOpsScoreDto(80.0,80.0,80.0));
        given(delivery.calcularDeliveryScoreFuncionario("E1",periodo)).willReturn(new DeliveryScoreDto("E1","Developer",periodo.getInicio(),periodo.getFim(),null,null,80.0));
    }
    @ParameterizedTest(name="Dado visitante, quando {0}, então 401 sem acessar negócio")
    @MethodSource("rotasProtegidas")
    void visitanteNaoExecutaRotaProtegida(Rota rota) throws Exception {
        mvc.perform(rota.request()).andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control","no-store"));
        then(funcionarios).shouldHaveNoInteractions(); then(ia).shouldHaveNoInteractions();
        then(dashboard).shouldHaveNoInteractions(); then(painel).shouldHaveNoInteractions();
        then(jardim).shouldHaveNoInteractions(); then(ranking).shouldHaveNoInteractions();
        then(ams).shouldHaveNoInteractions(); then(consumo).shouldHaveNoInteractions();
    }
    @ParameterizedTest(name="Dado admin autenticado, quando {0}, então contrato HTTP é atendido")
    @MethodSource("rotasProtegidas")
    void rotasRegistradasAceitamRequisicaoValida(Rota rota) throws Exception {
        dadoRetornosDosServicos();
        mvc.perform(como(rota.request(),"admin")).andExpect(status().is(rota.sucesso()));
        String path=rota.caminho().split("\\?")[0];
        switch(path) {
            case "/api/funcionario/sessao" -> then(sessoes).should(atLeastOnce()).atual();
            case "/api/funcionario/me/consumo-ia" -> then(consumo).should().obter("E1","5");
            case "/api/funcionario/listar" -> then(funcionarios).should().listar();
            case "/api/funcionario/buscar/E1" -> then(funcionarios).should().buscarPorId("E1");
            case "/api/funcionario/criar" -> then(funcionarios).should().criar(any());
            case "/api/funcionario/deletar/E2" -> then(funcionarios).should().deletar("E2");
            case "/api/utilizacao-ia/listar" -> then(ia).should().listar(any());
            case "/api/utilizacao-ia/destaques" -> then(ia).should().destaques(null,5);
            case "/api/utilizacao-ia/buscar/U1" -> { then(ia).should().buscarPorId("U1"); then(sessoes).should().exigirPessoa("E1"); }
            case "/api/utilizacao-ia/criar" -> then(ia).should().criar(any());
            case "/api/utilizacao-ia/deletar/U1" -> then(ia).should().deletar("U1");
            case "/api/dashboard/usuarios", "/api/dashboard/filtros" -> then(dashboard).should().filtros("administrador","E1");
            case "/api/dashboard/resumo" -> then(dashboard).should().resumo(new DashboardService.Consulta("administrador","E1","pessoal",null,null,null,5));
            case "/api/dashboard/integrantes" -> then(dashboard).should().integrantes(any(),eq("pessoa"),eq(""),eq("requisicoes"),eq(0),eq(10));
            case "/api/dashboard/ams/usuarios", "/api/dashboard/ams/filtros" -> then(painel).should().filtros("administrador","E1");
            case "/api/dashboard/ams" -> then(painel).should().painel(new AmsDashboardService.Consulta("administrador","E1",null,null,null,null,5,0,10,"","ams"));
            case "/api/jardim/estado" -> then(jardim).should().estado(null,null);
            case "/api/jardim/avancar" -> then(jardim).should().avancar(new JardimDto.Avancar(1,1,0L));
            case "/api/jardim/comprar" -> then(jardim).should().comprar(new JardimDto.Comprar("adubo",1,0L));
            case "/api/jardim/adubar" -> then(jardim).should().aplicar(new JardimDto.Adubar(1,0L,null,null));
            case "/api/jardim/zerar" -> then(jardim).should().zerar(new JardimDto.Aplicar(1,0L));
            case "/api/jardim/equipar" -> then(jardim).should().equipar(new JardimDto.Equipar("pedras",0L));
            case "/api/jardim/encerrar" -> then(jardim).should().encerrar(new JardimDto.Aplicar(1,0L));
            case "/api/jardim/ranking" -> then(ranking).should().ranking(1,1,"empresa","ams","todos",null,null);
            case "/score/ams/E1" -> then(ams).should().calcularAMS("E1",1);
            case "/score/ai-literacy/E1" -> then(literacy).should().calcularAiLiteracy(eq("E1"),any());
            case "/score/governance/E1" -> then(governance).should().calcularGovernanceDetalhado(eq("E1"),any(),eq(1));
            case "/score/security-incidents/E1" -> then(security).should().calcularSecurityDetalhado(eq("E1"),any(),eq(1));
            case "/score/finops/E1" -> then(finops).should().calcularFinOpsScore(eq("E1"),any());
            case "/score/delivery/E1" -> then(delivery).should().calcularDeliveryScoreFuncionario(eq("E1"),any());
            default -> fail("Rota sem verificação de delegação: "+path);
        }
    }
    @Test @DisplayName("Dado inventário dos controllers, então toda rota de negócio possui cenário HTTP")
    void todasAsRotasEstaoNoManifesto() {
        var mapping=mvc.getDispatcherServlet().getWebApplicationContext()
            .getBean(org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.class);
        var reais=new TreeSet<String>();
        mapping.getHandlerMethods().forEach((info,handler)-> {
            for(var method:info.getMethodsCondition().getMethods())
                for(var path:info.getPatternValues()) reais.add(method+" "+path);
        });
        var cobertas=new TreeSet<String>();
        rotasProtegidas().forEach(r->{
            String path=r.caminho().split("\\?")[0]
                .replaceAll("/score/([^/]+)/E1$","/score/$1/{employeeId}")
                .replaceAll("/(buscar|deletar)/(E1|E2|U1)$","/$1/{id}");
            cobertas.add(r.metodo()+" "+path);
        });
        cobertas.add("POST /api/funcionario/login"); cobertas.add("POST /api/funcionario/logout");
        assertEquals(reais,cobertas,"Adicione cenários ao criar ou alterar uma rota");
    }
    @Test @DisplayName("Dado login válido, quando autentica, então cookie HttpOnly/Lax dura 8 horas e não expõe token no JSON")
    void loginGeraCookie() throws Exception {
        var credenciais=new FuncionarioDto.LoginRequest("pessoa@raiz.test","123456");
        given(sessoes.login(credenciais)).willReturn(new SessaoService.Login(usuario(Posicao.COLABORADOR),"token-secreto"));
        mvc.perform(json("/api/funcionario/login","{\"email\":\"pessoa@raiz.test\",\"senha\":\"123456\"}"))
            .andExpect(status().isOk()).andExpect(header().string("Set-Cookie",allOf(containsString("usuario=token-secreto"),containsString("HttpOnly"),containsString("SameSite=Lax"),containsString("Max-Age=28800"),containsString("Path=/"))))
            .andExpect(jsonPath("$.employeeId").value("E1"))
            .andExpect(content().string(not(containsString("token-secreto"))));
        then(sessoes).should().login(credenciais);
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"email\":\"invalido\",\"senha\":\"x\"}","{\"email\":\"a@b.com\",\"senha\":\"\"}"})
    @DisplayName("Dadas credenciais malformadas, quando login, então 400 antes do serviço")
    void loginInvalido(String body) throws Exception {
        mvc.perform(json("/api/funcionario/login",body)).andExpect(status().isBadRequest());
        then(sessoes).should(never()).login(any());
    }
    @Test @DisplayName("Dado logout mesmo com sessão expirada, quando sai, então revoga token e apaga cookie")
    void logoutExpirado() throws Exception {
        mvc.perform(como(post("/api/funcionario/logout"),"expirado")).andExpect(status().isNoContent())
            .andExpect(header().string("Set-Cookie",allOf(containsString("Max-Age=0"),containsString("HttpOnly"))));
        then(sessoes).should().logout("expirado"); then(sessoes).should(never()).validar(any());
    }
    @Test @DisplayName("Dada sessão expirada, quando consulta sessão, então 401")
    void sessaoExpirada() throws Exception {
        mvc.perform(como(get("/api/funcionario/sessao"),"expirado")).andExpect(status().isUnauthorized());
    }
    @ParameterizedTest @ValueSource(strings={"/api/funcionario/login","/api/funcionario/logout","/api/jardim/adubar"})
    @DisplayName("Dada origem externa, quando altera estado, então 403 antes de autenticar ou executar")
    void origemExterna(String path) throws Exception {
        mvc.perform(como(json(path,"{}"),"admin").header("Origin","https://externo.test"))
            .andExpect(status().isForbidden());
        then(sessoes).shouldHaveNoInteractions(); then(jardim).shouldHaveNoInteractions();
    }
    @Test @DisplayName("Dado Sec-Fetch-Site cross-site sem Origin, quando logout, então 403")
    void crossSiteSemOrigin() throws Exception {
        mvc.perform(post("/api/funcionario/logout").header("Sec-Fetch-Site","cross-site")).andExpect(status().isForbidden());
        then(sessoes).shouldHaveNoInteractions();
    }
    @Test @DisplayName("Dado perfil, quando consulta consumo, então usa somente identidade da sessão")
    void consumoDoProprioUsuario() throws Exception {
        given(consumo.obter("E1","historico")).willReturn(new ConsumoPerfilService.Consumo("E1","historico",List.of(),List.of(new ConsumoPerfilService.Ferramenta("IA1","IA",20,30,50)),50));
        mvc.perform(como(get("/api/funcionario/me/consumo-ia?periodo=historico&employeeId=E2"),"colaborador"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value("E1"))
            .andExpect(jsonPath("$.total").value(50)).andExpect(jsonPath("$.ferramentas[0].total").value(50));
        then(consumo).should().obter("E1","historico");
    }
    @Test @DisplayName("Dado gestor e perfil forjado na URL, quando resumo, então identidade vem da sessão")
    void dashboardNaoConfiaEmPerfilDaUrl() throws Exception {
        mvc.perform(como(get("/api/dashboard/resumo?perfil=administrador&usuarioId=E2&companyId=C2&escopo=equipe&periodo=2"),"gestor")).andExpect(status().isOk());
        then(dashboard).should().resumo(new DashboardService.Consulta("gestor","E1","equipe",null,null,null,2));
    }
    @Test @DisplayName("Dado gestor, quando consulta painel AMS com filtros, então repassa filtros com identidade real")
    void filtrosAms() throws Exception {
        mvc.perform(como(get("/api/dashboard/ams?perfil=administrador&usuarioId=E2&periodo=3&page=2&size=7&busca=Ana&ordem=nome&departmentId=D1"),"gestor")).andExpect(status().isOk());
        then(painel).should().painel(new AmsDashboardService.Consulta("gestor","E1",null,null,"D1",null,3,2,7,"Ana","nome"));
    }
    @Test @DisplayName("Dado admin, quando escolhe empresa inexistente, então 400 sem calcular resumo")
    void empresaInexistente() throws Exception {
        given(dashboard.usuarios()).willReturn(List.of());
        mvc.perform(como(get("/api/dashboard/resumo?companyId=ausente"),"admin")).andExpect(status().isBadRequest());
        then(dashboard).should(never()).resumo(any());
    }
    @Test @DisplayName("Dado tabela filtrada, quando consulta integrantes, então preserva filtros, busca e paginação")
    void integrantes() throws Exception {
        given(dashboard.integrantes(any(),anyString(),anyString(),anyString(),anyInt(),anyInt()))
            .willReturn(new DashboardDto.Pagina(List.of(new DashboardDto.Linha("E1","Pessoa",1,50,100,2,3)),1,0,10));
        mvc.perform(como(get("/api/dashboard/integrantes?escopo=equipe&aiToolId=IA1&busca=Pessoa"),"gestor"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].tokens").value(100));
        then(dashboard).should().integrantes(new DashboardService.Consulta("gestor","E1","equipe",null,null,"IA1",5),"pessoa","Pessoa","requisicoes",0,10);
    }
    @ParameterizedTest @CsvSource({"-1,0,0,1","2,500,2,100","0,50,0,50"})
    @DisplayName("Dada paginação de uso de IA, quando lista, então limita tamanho e ordena por data")
    void paginacaoIa(int page,int size,int esperadoPage,int esperadoSize) throws Exception {
        given(ia.listar(any())).willAnswer(inv->new SliceImpl<>(List.of(),inv.getArgument(0),false));
        mvc.perform(como(get("/api/utilizacao-ia/listar?page="+page+"&size="+size),"colaborador"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        then(ia).should().listar(PageRequest.of(esperadoPage,esperadoSize,Sort.by(Sort.Direction.DESC,"timestamp")));
    }
    @ParameterizedTest @ValueSource(strings={"gestor","colaborador"})
    @DisplayName("Dado não administrador, quando altera uso de IA ou busca destaques, então 403")
    void iaRestritaAAdmin(String perfil) throws Exception {
        mvc.perform(como(json("/api/utilizacao-ia/criar","{\"usageId\":\"U1\"}"),perfil)).andExpect(status().isForbidden());
        mvc.perform(como(delete("/api/utilizacao-ia/deletar/U1"),perfil)).andExpect(status().isForbidden());
        mvc.perform(como(get("/api/utilizacao-ia/destaques"),perfil)).andExpect(status().isForbidden());
        then(ia).shouldHaveNoInteractions();
    }
    @ParameterizedTest @CsvSource({"0,1","100,20","5,5"})
    void destaquesLimitados(int limite,int esperado) throws Exception {
        mvc.perform(como(get("/api/utilizacao-ia/destaques?companyId=C1&limite="+limite),"admin")).andExpect(status().isOk());
        then(ia).should().destaques("C1",esperado);
    }
    @ParameterizedTest @ValueSource(strings={"ams","ai-literacy","governance","security-incidents","finops","delivery"})
    @DisplayName("Dado funcionário fora do escopo, quando acessa dimensão, então 403 antes de calcular")
    void scoreForaDoEscopo(String dimensao) throws Exception {
        willThrow(SessaoService.proibido()).given(sessoes).exigirPessoa("E2");
        mvc.perform(como(get("/score/"+dimensao+"/E2?periodo=1"),"colaborador")).andExpect(status().isForbidden());
        then(sessoes).should().exigirPessoa("E2"); then(ams).shouldHaveNoInteractions();
        then(literacy).shouldHaveNoInteractions(); then(governance).shouldHaveNoInteractions();
        then(security).shouldHaveNoInteractions(); then(finops).shouldHaveNoInteractions(); then(delivery).shouldHaveNoInteractions();
    }
    @ParameterizedTest @ValueSource(strings={"ams","ai-literacy","governance","security-incidents","finops","delivery"})
    @DisplayName("Dado período ausente, quando acessa dimensão, então 400")
    void scoreExigePeriodo(String dimensao) throws Exception {
        mvc.perform(como(get("/score/"+dimensao+"/E1"),"colaborador")).andExpect(status().isBadRequest());
        then(ams).shouldHaveNoInteractions();
    }
    @ParameterizedTest @CsvSource({"ai-literacy,aiLiteracyScore","governance,governanceScore","finops,finOpsScore","delivery,deliveryScore"})
    void dimensoesSerializamNotaEPeriodo(String dimensao,String campo) throws Exception {
        dadoRetornosDosServicos();
        mvc.perform(como(get("/score/"+dimensao+"/E1?periodo=1"),"colaborador"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value("E1"))
            .andExpect(jsonPath("$.periodo.numero").value(1)).andExpect(jsonPath("$."+campo).value(80.0));
        then(sessoes).should().exigirPessoa("E1");
    }
    @Test void securityEAMSRetornamDados() throws Exception {
        dadoRetornosDosServicos();
        given(security.calcularSecurityDetalhado(eq("E1"),any(),eq(1))).willReturn(new SecurityIncidentScoreDto("E1",null,2,15.0,85.0));
        given(ams.calcularAMS("E1",1)).willReturn(new AmsDto.Individual(null,null,new AmsDto.Calculo(true,80.0,List.of(),List.of(),0,5)));
        mvc.perform(como(get("/score/security-incidents/E1?periodo=1"),"colaborador")).andExpect(jsonPath("$.securityScore").value(85.0));
        mvc.perform(como(get("/score/ams/E1?periodo=1"),"colaborador")).andExpect(jsonPath("$.calculo.dimensoesAplicaveis").value(5));
    }
    @Test @DisplayName("Dado adubo aplicado no dia zero, quando envia, então mantém dia explícito e quantidade")
    void adubarDiaZero() throws Exception {
        given(jardim.aplicar(any(JardimDto.Adubar.class))).willReturn(new JardimDto.Atualizacao(null,List.of(),0,"Árvore adubada"));
        mvc.perform(como(json("/api/jardim/adubar","{\"periodo\":1,\"versao\":3,\"quantidade\":2,\"dia\":0}"),"colaborador"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.moedasRecebidas").value(0));
        then(jardim).should().aplicar(new JardimDto.Adubar(1,3L,2,0));
        then(jardim).should(never()).avancar(any());
    }
    @Test @DisplayName("Dado avanço de um dia, quando avança, então devolve moedas calculadas pelo serviço")
    void avancarPremia() throws Exception {
        given(jardim.avancar(new JardimDto.Avancar(1,1,3L))).willReturn(new JardimDto.Atualizacao(null,List.of(),8,"Dia avançado"));
        mvc.perform(como(json("/api/jardim/avancar","{\"periodo\":1,\"dias\":1,\"versao\":3}"),"colaborador"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.moedasRecebidas").value(8));
        then(jardim).should().avancar(new JardimDto.Avancar(1,1,3L));
    }
    @Test void zerarEncaminhaPeriodoEVersao() throws Exception {
        mvc.perform(como(json("/api/jardim/zerar","{\"periodo\":2,\"versao\":3}"),"colaborador")).andExpect(status().isOk());
        then(jardim).should().zerar(new JardimDto.Aplicar(2,3L)); then(jardim).shouldHaveNoMoreInteractions();
    }
    static Stream<Arguments> jardimInvalido() {
        return Stream.of(
            Arguments.of("avancar","{\"periodo\":1,\"dias\":8,\"versao\":0}"),
            Arguments.of("avancar","{\"periodo\":0,\"dias\":1,\"versao\":0}"),
            Arguments.of("avancar","{\"periodo\":1,\"dias\":0,\"versao\":0}"),
            Arguments.of("comprar","{\"item\":\"adubo\",\"quantidade\":21,\"versao\":0}"),
            Arguments.of("comprar","{\"item\":\"\",\"quantidade\":1,\"versao\":0}"),
            Arguments.of("adubar","{\"periodo\":1,\"quantidade\":17,\"versao\":0}"),
            Arguments.of("adubar","{\"periodo\":1,\"quantidade\":0,\"versao\":0}"),
            Arguments.of("adubar","{\"periodo\":1,\"dia\":91,\"versao\":0}"),
            Arguments.of("adubar","{\"periodo\":1,\"dia\":-1,\"versao\":0}"),
            Arguments.of("zerar","{\"periodo\":6,\"versao\":0}"),
            Arguments.of("zerar","{\"periodo\":1}"),
            Arguments.of("encerrar","{\"periodo\":1,\"versao\":-1}"),
            Arguments.of("equipar","{\"item\":\"\",\"versao\":0}"));
    }
    @ParameterizedTest @MethodSource("jardimInvalido")
    @DisplayName("Dados limites inválidos, quando altera jardim, então 400 sem mutação")
    void jardimValidaAntesDoServico(String operacao,String body) throws Exception {
        mvc.perform(como(json("/api/jardim/"+operacao,body),"colaborador")).andExpect(status().isBadRequest());
        then(jardim).shouldHaveNoInteractions();
    }
    @ParameterizedTest @ValueSource(ints={400,403,409})
    @DisplayName("Dado erro de negócio no jardim, quando opera, então preserva status e detalhe ProblemDetail")
    void jardimPreservaErro(int codigo) throws Exception {
        given(jardim.comprar(any())).willThrow(new ResponseStatusException(HttpStatus.valueOf(codigo),"Operação recusada"));
        mvc.perform(como(json("/api/jardim/comprar","{\"item\":\"adubo\",\"quantidade\":1,\"versao\":0}"),"colaborador"))
            .andExpect(status().is(codigo)).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(codigo)).andExpect(jsonPath("$.detail").value("Operação recusada"));
    }
    @Test void rankingUsaFiltrosEPadroes() throws Exception {
        mvc.perform(como(get("/api/jardim/ranking?periodo=2&dia=10"),"gestor")).andExpect(status().isOk());
        then(ranking).should().ranking(2,10,"empresa","ams","todos",null,null);
    }
    @Test void estadoAceitaPrevia() throws Exception {
        mvc.perform(como(get("/api/jardim/estado?periodo=2&dia=0"),"colaborador")).andExpect(status().isOk());
        then(jardim).should().estado(2,0);
    }
    @Test void iaInexistenteRetorna404() throws Exception {
        given(ia.buscarPorId("ausente")).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(como(get("/api/utilizacao-ia/buscar/ausente"),"admin")).andExpect(status().isNotFound());
    }
    @Test void cadastroDuplicadoRetorna409() throws Exception {
        given(funcionarios.criar(any())).willThrow(new ResponseStatusException(HttpStatus.CONFLICT));
        mvc.perform(como(json("/api/funcionario/criar","{\"gestorId\":\"E1\",\"nome\":\"Pessoa\",\"email\":\"a@b.com\",\"senha\":\"123456\"}"),"gestor")).andExpect(status().isConflict());
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"usageId\":\"\"}","{\"usageId\":\"U1\",\"requestCount\":-1}","{\"usageId\":\"U1\",\"inputTokens\":-1}","{\"usageId\":\"U1\",\"outputTokens\":-1}","{\"usageId\":\"U1\",\"latencyMs\":-1}"})
    @DisplayName("Dado registro de IA sem ID ou com contagem negativa, quando cadastra, então 400 sem persistir")
    void cadastroIaRejeitaDadosInvalidos(String body) throws Exception {
        mvc.perform(como(json("/api/utilizacao-ia/criar",body),"admin")).andExpect(status().isBadRequest());
        then(ia).shouldHaveNoInteractions();
    }
    @Test void cadastroIaAceitaZeroECamposOpcionais() throws Exception {
        mvc.perform(como(json("/api/utilizacao-ia/criar","{\"usageId\":\"U1\",\"requestCount\":0,\"inputTokens\":0,\"outputTokens\":0,\"latencyMs\":0}"),"admin"))
            .andExpect(status().isCreated());
        then(ia).should().criar(argThat(r->r.usageId().equals("U1") && r.requestCount()==0 && r.latencyMs()==0));
    }
    @Test void cadastroIaRejeitaIdMaiorQueColuna() throws Exception {
        mvc.perform(como(json("/api/utilizacao-ia/criar","{\"usageId\":\""+"x".repeat(65)+"\"}"),"admin"))
            .andExpect(status().isBadRequest());
        then(ia).shouldHaveNoInteractions();
    }
    @Test void buscaIaVerificaDonoAntesDeExporDados() throws Exception {
        given(ia.buscarPorId("U2")).willReturn(new UtilizacaoIADto.Response("U2",null,"E2",null,null,null,null,null,1,2,3,null,null,null,null));
        willThrow(SessaoService.proibido()).given(sessoes).exigirPessoa("E2");
        mvc.perform(como(get("/api/utilizacao-ia/buscar/U2"),"colaborador")).andExpect(status().isForbidden());
        then(sessoes).should().exigirPessoa("E2");
    }
    @Test void loginComSenhaIncorretaNaoEmiteCookie() throws Exception {
        given(sessoes.login(any())).willThrow(SessaoService.naoAutorizado());
        mvc.perform(json("/api/funcionario/login","{\"email\":\"a@b.com\",\"senha\":\"errada\"}"))
            .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Set-Cookie"));
    }
}
