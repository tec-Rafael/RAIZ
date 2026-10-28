package br.com.raiz.usuario.config;

import br.com.raiz.usuario.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerMapping;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

@Configuration
public class AutenticacaoConfig implements WebMvcConfigurer {
    private final SessaoService sessoes;
    private final Set<String> origens;
    public AutenticacaoConfig(SessaoService sessoes,
        @Value("${raiz.auth.origens:http://127.0.0.1:3000,http://localhost:3000,http://127.0.0.1:8080,http://localhost:8080}") String origens) {
        this.sessoes = sessoes; this.origens = Set.copyOf(Arrays.asList(origens.split(",")));
    }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor()).addPathPatterns("/api/**","/score/**");
    }
    HandlerInterceptor interceptor() {
        return new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                res.setHeader("Cache-Control","no-store");
                // Navegações e leituras são permitidas; mutações de outra origem são recusadas.
                String origin = req.getHeader("Origin");
                if (!Set.of("GET","HEAD","OPTIONS").contains(req.getMethod()) &&
                    ("cross-site".equals(req.getHeader("Sec-Fetch-Site")) || (origin != null && !origens.contains(origin))))
                    throw SessaoService.proibido();
                String path = req.getRequestURI();
                if (path.equals("/api/funcionario/login") && req.getMethod().equals("POST")) return true;
                // Sair também funciona quando a sessão já expirou; só revoga o token recebido.
                if (path.equals("/api/funcionario/logout") && req.getMethod().equals("POST")) return true;
                req.setAttribute("usuarioAutenticado", sessoes.validar(SessaoService.cookie(req)));
                Object variables = req.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                if (variables instanceof Map<?,?> map && map.get("employeeId") instanceof String id) sessoes.exigirPessoa(id);
                return true;
            }
        };
    }
}
