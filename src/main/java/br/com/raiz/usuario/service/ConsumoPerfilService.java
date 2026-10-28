package br.com.raiz.usuario.service;

import br.com.raiz.dashboard.service.DashboardService;
import br.com.raiz.dashboard.repository.DashboardRepository;
import br.com.raiz.dashboard.dto.DashboardDto.Periodo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.time.LocalDate;

@Service
public class ConsumoPerfilService {
    private final JdbcTemplate jdbc;
    private final DashboardRepository dashboard;
    public ConsumoPerfilService(JdbcTemplate jdbc,DashboardRepository dashboard) { this.jdbc=jdbc; this.dashboard=dashboard; }
    public record Ferramenta(String id,String nome,long entrada,long saida,long total) { }
    public record Consumo(String employeeId,String periodo,List<Periodo> periodos,List<Ferramenta> ferramentas,long total) { }
    public Consumo obter(String employeeId,String periodo) {
        var periodos=DashboardService.periodos(dashboard.primeiraData());
        LocalDate inicio=null,fim=null;
        if (!periodo.equals("historico")) {
            int id;
            try { id=Integer.parseInt(periodo); } catch (NumberFormatException ex) { throw invalido(); }
            if(id<1 || id>5) throw invalido();
            if(!periodos.isEmpty()) { inicio=periodos.get(id-1).inicio(); fim=periodos.get(id-1).fim().plusDays(1); }
        }
        var ferramentas=jdbc.query("""
            SELECT t.ai_tool_id,t.tool_name,coalesce(sum(l.input_tokens),0) AS entrada,
                coalesce(sum(l.output_tokens),0) AS saida
            FROM ai_tools t LEFT JOIN ai_usage_logs l ON l.ai_tool_id=t.ai_tool_id AND l.employee_id=?
                AND (cast(? AS date) IS NULL OR l.timestamp>=cast(? AS date))
                AND (cast(? AS date) IS NULL OR l.timestamp<cast(? AS date))
            GROUP BY t.ai_tool_id,t.tool_name ORDER BY t.tool_name
            """,(rs,n)->new Ferramenta(rs.getString(1),rs.getString(2),rs.getLong(3),rs.getLong(4),rs.getLong(3)+rs.getLong(4)),
            employeeId,inicio,inicio,fim,fim);
        return new Consumo(employeeId,periodo,periodos,ferramentas,ferramentas.stream().mapToLong(Ferramenta::total).sum());
    }
    private static ResponseStatusException invalido() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Período inválido. Use 1 a 5 ou historico."); }
}
