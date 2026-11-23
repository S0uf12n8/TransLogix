package ma.translogix.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ma.translogix.dao.AuditDao;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.ResultatAudit;

/**
 * Audit log in MySQL (table audit_evenements, see db/schema.sql). Append-only like the interface:
 * this class has no UPDATE or DELETE statement. Parameterized queries only, errors through SqlErreurs.
 */
public class JdbcAuditDao implements AuditDao {

    private static final String INSERT = "INSERT INTO audit_evenements (horodatage, utilisateur, module, action, resultat)"
            + " VALUES (?, ?, ?, ?, ?)";
    /** Insertion order, as the interface promises (the primary key follows insertion). */
    private static final String SELECT_TOUS = "SELECT id, horodatage, utilisateur, module, action, resultat"
            + " FROM audit_evenements ORDER BY id";

    private final ConnexionBase base;

    public JdbcAuditDao(ConnexionBase base) {
        this.base = base;
    }

    @Override
    public EvenementAudit save(EvenementAudit e) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setObject(1, e.getHorodatage());
                ps.setString(2, e.getUtilisateur());
                ps.setString(3, e.getModule());
                ps.setString(4, e.getAction());
                ps.setString(5, e.getResultat().name());
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    e.setId(cles.getLong(1));
                }
                return e;
            }
        });
    }

    @Override
    public List<EvenementAudit> findAll() {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(SELECT_TOUS);
                 ResultSet rs = ps.executeQuery()) {
                List<EvenementAudit> resultat = new ArrayList<>();
                while (rs.next()) {
                    resultat.add(new EvenementAudit(rs.getLong("id"), rs.getObject("horodatage", LocalDateTime.class),
                            rs.getString("utilisateur"), rs.getString("module"), rs.getString("action"),
                            ResultatAudit.valueOf(rs.getString("resultat"))));
                }
                return resultat;
            }
        });
    }
}
