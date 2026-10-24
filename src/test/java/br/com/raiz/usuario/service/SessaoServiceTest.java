package br.com.raiz.usuario.service;

import br.com.raiz.usuario.domain.Funcionario;
import br.com.raiz.usuario.domain.Posicao;
import br.com.raiz.usuario.dto.FuncionarioDto.LoginRequest;
import br.com.raiz.usuario.repository.FuncionarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessaoServiceTest {
    FuncionarioRepository repository;
    JdbcTemplate jdbc;
    PasswordEncoder encoder;
    SessaoService service;
    Funcionario pessoa;
    @BeforeEach void preparar() {
        repository=mock(FuncionarioRepository.class);jdbc=mock(JdbcTemplate.class);encoder=mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("hash-ficticio");
        service=new SessaoService(repository,encoder,jdbc);
        pessoa=pessoa("C01-E0012","C01","D1",Posicao.COLABORADOR,true);
    }
    @AfterEach void limpar() { RequestContextHolder.resetRequestAttributes(); }
    @Test void loginGeraTokenAleatorioEExpiraEmOitoHoras() {
        when(repository.findByEmailIgnoreCase("pessoa@raiz.local")).thenReturn(Optional.of(pessoa));
        when(encoder.matches("123456","hash")).thenReturn(true);
        var inicio=Instant.now();
        var a=service.login(new LoginRequest(" PESSOA@RAIZ.LOCAL ","123456"));
        var b=service.login(new LoginRequest("pessoa@raiz.local","123456"));
        assertTrue(a.cookie().matches("[a-f0-9]{128}C01-E0012"));
        assertNotEquals(a.cookie(),b.cookie());
        assertFalse(a.usuario().expiraEm().isBefore(inicio.plusSeconds(28800)));
        assertTrue(a.usuario().expiraEm().isBefore(Instant.now().plusSeconds(28801)));
        verify(jdbc).update(anyString(),eq(a.cookie().substring(0,128)),any(Timestamp.class),eq("C01-E0012"));
    }
    @Test void senhaIncorretaNaoCriaSessao() {
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(pessoa));
        assertEquals(401,assertThrows(ResponseStatusException.class,()->service.login(new LoginRequest("pessoa@raiz.local","errada"))).getStatusCode().value());
        verifyNoInteractions(jdbc);
    }
    @Test void contaInativaNaoCriaSessao() {
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(pessoa("C01-E0012","C01","D1",Posicao.COLABORADOR,false)));
        when(encoder.matches(anyString(),anyString())).thenReturn(true);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->service.login(new LoginRequest("pessoa@raiz.local","123456"))).getStatusCode().value());
        verifyNoInteractions(jdbc);
    }
    @Test void rejeitaCookieIncompletoSemConsultarBanco() {
        assertThrows(ResponseStatusException.class,()->service.validar("abc123"));
        assertThrows(ResponseStatusException.class,()->service.validar(null));
        verifyNoInteractions(jdbc);
    }
    @SuppressWarnings("unchecked")
    void tokenNoBanco(String token,Instant expira) {
        when(jdbc.query(anyString(),any(RowMapper.class),eq("C01-E0012"))).thenAnswer(invocation->{
            ResultSet rs=mock(ResultSet.class);when(rs.getString(1)).thenReturn(token);
            when(rs.getTimestamp(2)).thenReturn(Timestamp.from(expira));
            return List.of(((RowMapper<?>)invocation.getArgument(1)).mapRow(rs,0));
        });
        when(repository.findById("C01-E0012")).thenReturn(Optional.of(pessoa));
    }
    @Test void preservaIdAlfanumericoNaValidacao() {
        String token="a".repeat(128); tokenNoBanco(token,Instant.now().plusSeconds(60));
        assertEquals("C01-E0012",service.validar(token+"C01-E0012").employeeId());
    }
    @Test void rejeitaTokenSubstituidoOuExpirado() {
        String token="a".repeat(128);
        tokenNoBanco("b".repeat(128),Instant.now().plusSeconds(60));
        assertThrows(ResponseStatusException.class,()->service.validar(token+"C01-E0012"));
        tokenNoBanco(token,Instant.now().minusSeconds(1));
        assertThrows(ResponseStatusException.class,()->service.validar(token+"C01-E0012"));
    }
    @Test void logoutSoRevogaTokenRecebidoNaoOutraSessao() {
        String token="a".repeat(128);service.logout(token+"C01-E0012");
        verify(jdbc).update(contains("AND token = ?"),eq("C01-E0012"),eq(token));
    }
    @Test void gestorLimitadoAoProprioDepartamentoEEmpresa() {
        autenticar(Posicao.GESTOR);
        assertTrue(service.permite(pessoa));
        assertFalse(service.permite(pessoa("C01-E0099","C01","D2",Posicao.COLABORADOR,true)));
        assertFalse(service.permite(pessoa("C02-E0012","C02","D1",Posicao.COLABORADOR,true)));
    }
    @Test void colaboradorSoPodeVerSiMesmo() {
        autenticar(Posicao.COLABORADOR);
        assertTrue(service.permite(pessoa));
        assertFalse(service.permite(pessoa("C01-E0099","C01","D1",Posicao.COLABORADOR,true)));
        assertThrows(ResponseStatusException.class,service::exigirGestao);
    }
    @Test void administradorPodeVerTodasEmpresas() {
        autenticar(Posicao.ADMINISTRADOR);
        assertTrue(service.permite(pessoa("C02-E0012","C02","D1",Posicao.COLABORADOR,true)));
        assertDoesNotThrow(service::exigirAdmin);
    }
    private void autenticar(Posicao posicao) {
        var request=new MockHttpServletRequest();
        request.setAttribute("usuarioAutenticado",new SessaoService.Usuario("C01-E0012","Pessoa","pessoa@raiz.local",posicao,"C01","D1","Cargo",Instant.now().plusSeconds(60)));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
    private Funcionario pessoa(String id,String company,String dept,Posicao posicao,boolean active) {
        return new Funcionario(id,company,dept,"Cargo","Senior",LocalDate.now(),"CC",active,"Pessoa","pessoa@raiz.local","hash",posicao);
    }
}
