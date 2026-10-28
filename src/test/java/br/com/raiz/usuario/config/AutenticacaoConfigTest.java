package br.com.raiz.usuario.config;

import br.com.raiz.usuario.service.SessaoService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutenticacaoConfigTest {
    SessaoService sessoes=mock(SessaoService.class);
    AutenticacaoConfig config=new AutenticacaoConfig(sessoes,"http://localhost:3000");
    @Test void loginDeOutraOrigemERecusadoAntesDeCriarSessao() {
        var req=new MockHttpServletRequest("POST","/api/funcionario/login");req.addHeader("Origin","https://outro.site");
        assertEquals(403,assertThrows(ResponseStatusException.class,()->config.interceptor().preHandle(req,new MockHttpServletResponse(),new Object())).getStatusCode().value());
        verifyNoInteractions(sessoes);
    }
    @Test void logoutCrossSiteNaoPodeRevogarSessao() {
        var req=new MockHttpServletRequest("POST","/api/funcionario/logout");req.addHeader("Sec-Fetch-Site","cross-site");
        assertThrows(ResponseStatusException.class,()->config.interceptor().preHandle(req,new MockHttpServletResponse(),new Object()));
        verifyNoInteractions(sessoes);
    }
    @Test void logoutMesmoExpiradoPodeLimparCookie() throws Exception {
        var req=new MockHttpServletRequest("POST","/api/funcionario/logout");
        assertTrue(config.interceptor().preHandle(req,new MockHttpServletResponse(),new Object()));
        verifyNoInteractions(sessoes);
    }
    @Test void requisicaoDiretaSemCookieERecusada() {
        when(sessoes.validar(null)).thenThrow(SessaoService.naoAutorizado());
        var req=new MockHttpServletRequest("GET","/api/dashboard/ams");
        assertEquals(401,assertThrows(ResponseStatusException.class,()->config.interceptor().preHandle(req,new MockHttpServletResponse(),new Object())).getStatusCode().value());
    }
    @Test void ScoreComParametroDeMatrizAindaValidaPermissaoDaPessoa() throws Exception {
        var req=new MockHttpServletRequest("GET","/score;parametro=x/ams/C02-E0001");
        req.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,Map.of("employeeId","C02-E0001"));
        config.interceptor().preHandle(req,new MockHttpServletResponse(),new Object());
        verify(sessoes).exigirPessoa("C02-E0001");
    }
}
