package ma.translogix.dao.jdbc;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import ma.translogix.dao.PaiementDao;
import ma.translogix.model.ModePaiement;
import ma.translogix.model.Paiement;

/**
 * Payments in MySQL (table paiements, see db/schema.sql). Append-only, like InMemoryPaiementDao:
 * a recorded payment is never edited or deleted. Every query is a PreparedStatement with ? parameters
 * and named columns, and errors are translated to French by SqlErreurs.
 */
public class JdbcPaiementDao implements PaiementDao {

    private static final String COLONNES = "id, facture_id, mode, reference, date";
    private static final String SELECT_TOUS = "SELECT " + COLONNES + " FROM paiements ORDER BY id";
    private static final String SELECT_PAR_FACTURE =
            "SELECT " + COLONNES + " FROM paiements WHERE facture_id = ? ORDER BY id";
    private static final String INSERT =
            "INSERT INTO paiements (facture_id, mode, reference, date) VALUES (?, ?, ?, ?)";

    private final ConnexionBase base;

    public JdbcPaiementDao(ConnexionBase base) {
        this.base = base;
    }

    @Override
    public Paiement save(Paiement paiement) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, paiement.getFactureId());
                ps.setString(2, paiement.getMode().name());
                ps.setString(3, paiement.getReference());
                if (paiement.getDate() == null) {
                    ps.setNull(4, Types.DATE);
                } else {
                    ps.setDate(4, Date.valueOf(paiement.getDate()));
                }
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    paiement.setId(cles.getLong(1));
                }
                return paiement;
            }
        });
    }

    @Override
    public List<Paiement> findAll() {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(SELECT_TOUS);
                 ResultSet rs = ps.executeQuery()) {
                return lireTous(rs);
            }
        });
    }

    @Override
    public List<Paiement> findByFacture(Long factureId) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(SELECT_PAR_FACTURE)) {
                ps.setLong(1, factureId);
                try (ResultSet rs = ps.executeQuery()) {
                    return lireTous(rs);
                }
            }
        });
    }

    private static List<Paiement> lireTous(ResultSet rs) throws SQLException {
        List<Paiement> resultat = new ArrayList<>();
        while (rs.next()) {
            Date date = rs.getDate("date");
            LocalDate localDate = date == null ? null : date.toLocalDate();
            resultat.add(new Paiement(rs.getLong("id"), rs.getLong("facture_id"),
                    ModePaiement.valueOf(rs.getString("mode")), rs.getString("reference"), localDate));
        }
        return resultat;
    }
}
