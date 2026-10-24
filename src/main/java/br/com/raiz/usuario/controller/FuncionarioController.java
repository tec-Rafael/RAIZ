package br.com.raiz.usuario.controller;

import br.com.raiz.usuario.dto.FuncionarioDto.*;
import br.com.raiz.usuario.service.FuncionarioService;
import br.com.raiz.usuario.service.SessaoService;
import br.com.raiz.usuario.service.ConsumoPerfilService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/funcionario")
@Tag(name="Funcionário", description="Login por cookie, sessão e cadastro com autorização por cargo")
public class FuncionarioController {
    private final FuncionarioService service;
    private final SessaoService sessoes;
    private final ConsumoPerfilService consumo;
    private final boolean cookieSecure;
    public FuncionarioController(FuncionarioService service, SessaoService sessoes,
        ConsumoPerfilService consumo, @Value("${raiz.auth.cookie-secure:false}") boolean cookieSecure) {
        this.service=service; this.sessoes=sessoes; this.consumo=consumo; this.cookieSecure=cookieSecure;
    }
    @PostMapping("/login")
    public SessaoService.Usuario login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var login = sessoes.login(request);
        cookie(response,login.cookie(),SessaoService.DURACAO);
        return login.usuario();
    }
    @GetMapping("/sessao") public SessaoService.Usuario sessao() { return sessoes.atual(); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request,HttpServletResponse response) {
        sessoes.logout(SessaoService.cookie(request)); cookie(response,"",Duration.ZERO);
    }
    private void cookie(HttpServletResponse response,String value,Duration duration) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("usuario",value)
            .httpOnly(true).secure(cookieSecure).sameSite("Lax").path("/").maxAge(duration).build().toString());
    }
    @GetMapping("/me/consumo-ia")
    public ConsumoPerfilService.Consumo consumo(@RequestParam(defaultValue="5") String periodo) {
        return consumo.obter(sessoes.atual().employeeId(),periodo);
    }
    @PostMapping("/criar") @ResponseStatus(HttpStatus.CREATED)
    public Response criar(@Valid @RequestBody CriarRequest request) { return service.criar(request); }
    @GetMapping("/listar") public List<Response> listar() { return service.listar(); }
    @GetMapping("/buscar/{id}") public Response buscar(@PathVariable String id) { return service.buscarPorId(id); }
    @DeleteMapping("/deletar/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable String id) { service.deletar(id); }
}
