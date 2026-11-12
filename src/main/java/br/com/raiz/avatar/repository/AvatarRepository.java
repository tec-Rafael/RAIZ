package br.com.raiz.avatar.repository;

import br.com.raiz.avatar.dto.AvatarDto.Avatar;
import br.com.raiz.avatar.service.AvatarCatalogo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class AvatarRepository {
    private final JdbcTemplate jdbc;
    public AvatarRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Registro(Avatar avatar, String origem) { }
    private static final String COLUNAS = "genero,pele,olhos,cabelo,cor_cabelo,facial,sardas,camisa,calca";
    private static final RowMapper<Avatar> AVATAR = (r, n) -> new Avatar(2, r.getString(1), r.getString(2), r.getString(3), r.getString(4),
        r.getString(5), r.getString(6), r.getBoolean(7), r.getString(8), r.getString(9));

    public Optional<Registro> buscar(String employeeId) {
        return jdbc.query("SELECT " + COLUNAS + ",origem FROM avatares WHERE employee_id=?",
            (r, n) -> new Registro(AVATAR.mapRow(r, n), r.getString(10)), employeeId).stream().findFirst();
    }
    /** Grava ou substitui o avatar do colaborador. */
    public void salvar(String employeeId, Avatar a, String origem) {
        jdbc.update("INSERT INTO avatares (employee_id," + COLUNAS + ",origem,atualizado_em) VALUES (?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP) " +
            "ON CONFLICT (employee_id) DO UPDATE SET genero=EXCLUDED.genero,pele=EXCLUDED.pele,olhos=EXCLUDED.olhos,cabelo=EXCLUDED.cabelo," +
            "cor_cabelo=EXCLUDED.cor_cabelo,facial=EXCLUDED.facial,sardas=EXCLUDED.sardas,camisa=EXCLUDED.camisa,calca=EXCLUDED.calca," +
            "origem=EXCLUDED.origem,atualizado_em=CURRENT_TIMESTAMP",
            employeeId, a.genero(), a.pele(), a.olhos(), a.cabelo(), a.corCabelo(), a.facial(), a.sardas(), a.camisa(), a.calca(), origem);
    }
    /** Insere só se o colaborador ainda não tiver avatar (não sobrescreve uma escolha feita em paralelo). */
    public boolean inserirSeAusente(String employeeId, Avatar a, String origem) {
        return jdbc.update("INSERT INTO avatares (employee_id," + COLUNAS + ",origem) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (employee_id) DO NOTHING",
            employeeId, a.genero(), a.pele(), a.olhos(), a.cabelo(), a.corCabelo(), a.facial(), a.sardas(), a.camisa(), a.calca(), origem) == 1;
    }
    /** Chaves (combinações) já usadas por algum colaborador. */
    public Set<String> chavesEmUso() {
        return new HashSet<>(jdbc.query("SELECT " + COLUNAS + " FROM avatares", (r, n) -> AvatarCatalogo.chave(AVATAR.mapRow(r, n))));
    }
    public List<String> funcionariosSemAvatar() {
        return jdbc.queryForList("SELECT e.employee_id FROM employees e WHERE NOT EXISTS (SELECT 1 FROM avatares a WHERE a.employee_id=e.employee_id) ORDER BY e.employee_id", String.class);
    }
    public boolean funcionarioExiste(String employeeId) {
        Long n = jdbc.queryForObject("SELECT count(*) FROM employees WHERE employee_id=?", Long.class, employeeId);
        return n != null && n > 0;
    }
}
