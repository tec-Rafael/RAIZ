package br.com.raiz.usuario.config;

import br.com.raiz.usuario.controller.FuncionarioController;
import br.com.raiz.usuario.domain.*;
import br.com.raiz.usuario.repository.FuncionarioRepository;
import br.com.raiz.usuario.service.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Aceitação com controllers, serviços e permissões reais; somente persistência e hash isolados. */
class FuncionarioRotasBddTest {
    FuncionarioRepository repo;
    PasswordEncoder encoder;
    MockMvc mvc;
    static Funcionario pessoa(String id,String empresa,String area,Posicao cargo) {
        return new Funcionario(id,empresa,area,"Developer","Senior",null,"CC",true,
            "Pessoa "+id,id.toLowerCase()+"@raiz.test","hash",cargo);
    }
    @BeforeEach void dadoRepositorioIsolado() {
        repo=mock(FuncionarioRepository.class); encoder=mock(PasswordEncoder.class);
        var sessoes=spy(new SessaoService(repo,encoder,mock(JdbcTemplate.class)));
        // Apenas a validação do token é substituída. As regras de autorização são reais.
        willAnswer(inv->{
            String token=inv.getArgument(0);
            if(token==null) throw SessaoService.naoAutorizado();
            return RotasBddTest.usuario(Posicao.valueOf(token));
        }).given(sessoes).validar(any());
        var service=new FuncionarioService(repo,encoder,sessoes);
        mvc=MockMvcBuilders.standaloneSetup(new FuncionarioController(service,sessoes,mock(ConsumoPerfilService.class),false))
            .addInterceptors(new AutenticacaoConfig(sessoes,"http://localhost:3000").interceptor()).build();
        given(repo.findById("E1")).willReturn(Optional.of(pessoa("E1","C1","D1",Posicao.GESTOR)));
        given(repo.findById("E2")).willReturn(Optional.of(pessoa("E2","C1","D1",Posicao.COLABORADOR)));
        given(repo.findById("E3")).willReturn(Optional.of(pessoa("E3","C1","D2",Posicao.COLABORADOR)));
        given(repo.findById("E4")).willReturn(Optional.of(pessoa("E4","C2","D1",Posicao.COLABORADOR)));
        given(repo.save(any())).willAnswer(inv->inv.getArgument(0));
        given(encoder.encode("123456")).willReturn("hash-seguro");
        // O construtor da sessão prepara um hash sentinela; não é efeito da requisição testada.
        clearInvocations(encoder);
    }
    Cookie cookie(String cargo){return new Cookie("usuario",cargo);}
    String cadastro(String gestor,String email,String cargo) {
        return """
            {"gestorId":"%s","nome":"Nova Pessoa","email":"%s","senha":"123456",
             "posicao":"%s","companyId":"FORJADA","departmentId":"FORJADO"}
            """.formatted(gestor,email,cargo);
    }
    @ParameterizedTest @CsvSource({
        "COLABORADOR,E1,200","COLABORADOR,E2,403","GESTOR,E2,200",
        "GESTOR,E3,403","GESTOR,E4,403","ADMINISTRADOR,E3,200","ADMINISTRADOR,E4,200"})
    @DisplayName("Dado cargo e alvo, quando busca funcionário, então respeita identidade, empresa e departamento")
    void buscaRespeitaEscopo(String cargo,String id,int status) throws Exception {
        var result=mvc.perform(get("/api/funcionario/buscar/"+id).cookie(cookie(cargo))).andExpect(status().is(status));
        if(status==200) result.andExpect(jsonPath("$.employeeId").value(id)).andExpect(jsonPath("$.senha").doesNotExist());
    }
    @Test @DisplayName("Dado gestor, quando cadastra com área forjada, então herda departamento e empresa do gestor")
    void cadastroHerdaDepartamento() throws Exception {
        mvc.perform(post("/api/funcionario/criar").cookie(cookie("GESTOR")).contentType(MediaType.APPLICATION_JSON)
            .content(cadastro("E1","nova@raiz.test","COLABORADOR")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.companyId").value("C1"))
            .andExpect(jsonPath("$.departmentId").value("D1")).andExpect(jsonPath("$.senha").doesNotExist());
        var salvo=ArgumentCaptor.forClass(Funcionario.class);
        then(repo).should().save(salvo.capture());
        assertEquals("hash-seguro",salvo.getValue().getSenha());
        assertEquals("C1-E0001",salvo.getValue().getEmployeeId());
    }
    @ParameterizedTest @CsvSource({
        "COLABORADOR,E1,nova@raiz.test,COLABORADOR",
        "GESTOR,E2,nova@raiz.test,COLABORADOR",
        "GESTOR,E1,nova@raiz.test,ADMINISTRADOR",
        "ADMINISTRADOR,E1,raiz@gmail.com,COLABORADOR",
        "ADMINISTRADOR,E1,nova@raiz.test,ADMINISTRADOR"})
    @DisplayName("Dado cadastro sem autorização ou admin reservado, quando cria, então 403 sem persistir")
    void cadastroProibido(String cargo,String gestor,String email,String posicao) throws Exception {
        mvc.perform(post("/api/funcionario/criar").cookie(cookie(cargo)).contentType(MediaType.APPLICATION_JSON)
            .content(cadastro(gestor,email,posicao))).andExpect(status().isForbidden());
        then(repo).should(never()).save(any()); then(encoder).shouldHaveNoInteractions();
    }
    @Test void emailDuplicadoNaoPersiste() throws Exception {
        given(repo.existsByEmailIgnoreCase("nova@raiz.test")).willReturn(true);
        mvc.perform(post("/api/funcionario/criar").cookie(cookie("GESTOR")).contentType(MediaType.APPLICATION_JSON)
            .content(cadastro("E1","nova@raiz.test","COLABORADOR"))).andExpect(status().isConflict());
        then(repo).should(never()).save(any());
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"gestorId\":\"E1\",\"nome\":\"Pessoa\",\"email\":\"invalido\",\"senha\":\"123456\"}","{\"gestorId\":\"E1\",\"nome\":\"Pessoa\",\"email\":\"a@b.com\",\"senha\":\"123\"}"})
    void cadastroInvalidoNaoPersiste(String body) throws Exception {
        mvc.perform(post("/api/funcionario/criar").cookie(cookie("GESTOR")).contentType(MediaType.APPLICATION_JSON)
            .content(body)).andExpect(status().isBadRequest());
        then(repo).should(never()).save(any());
    }
    @ParameterizedTest @CsvSource({"COLABORADOR,E2","GESTOR,E1","GESTOR,E3","GESTOR,E4","ADMINISTRADOR,ROOT-E0001"})
    void exclusaoProibidaNaoAlteraBanco(String cargo,String id) throws Exception {
        given(repo.findById("ROOT-E0001")).willReturn(Optional.of(pessoa("ROOT-E0001","ROOT","ROOT",Posicao.ADMINISTRADOR)));
        mvc.perform(delete("/api/funcionario/deletar/"+id).cookie(cookie(cargo))).andExpect(status().isForbidden());
        then(repo).should(never()).delete(any());
    }
    @Test void gestorExcluiFuncionarioDoDepartamento() throws Exception {
        mvc.perform(delete("/api/funcionario/deletar/E2").cookie(cookie("GESTOR"))).andExpect(status().isNoContent());
        then(repo).should().delete(argThat(f->f.getEmployeeId().equals("E2"))); then(repo).should().flush();
    }
    @Test void exclusaoComHistoricoRetorna409() throws Exception {
        willThrow(new DataIntegrityViolationException("FK")).given(repo).flush();
        mvc.perform(delete("/api/funcionario/deletar/E2").cookie(cookie("GESTOR"))).andExpect(status().isConflict());
    }
    @ParameterizedTest @CsvSource({"COLABORADOR,1","GESTOR,2","ADMINISTRADOR,4"})
    void listagemFiltraPessoasPeloCargo(String cargo,int quantidade) throws Exception {
        given(repo.findAll()).willReturn(List.of(pessoa("E1","C1","D1",Posicao.GESTOR),
            pessoa("E2","C1","D1",Posicao.COLABORADOR),pessoa("E3","C1","D2",Posicao.COLABORADOR),
            pessoa("E4","C2","D1",Posicao.COLABORADOR)));
        mvc.perform(get("/api/funcionario/listar").cookie(cookie(cargo))).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(quantidade));
    }
}
