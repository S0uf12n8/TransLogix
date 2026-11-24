package ma.translogix.dao.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ma.translogix.dao.DepenseDao;
import ma.translogix.model.Depense;
import ma.translogix.model.StatutDepense;
import ma.translogix.model.TypeDepense;

/**
 * Driver expenses in MySQL (table depenses, see db/schema.sql). Same contract as InMemoryDepenseDao.
 * Every query is a PreparedStatement with ? parameters, names its columns, and goes through
 * SqlErreurs so database errors reach the user in French.
 */
public class JdbcDepenseDao implements DepenseDao {

    private static final String COLONNES =
            "id, mission_id, chauffeur_id, type, station, montant, litres, date, statut, motif_refus";
    private static final String SELECT_TOUS = "SELECT " + COLONNES + " FROM depenses ORDER BY id";
    private static final String SELECT_PAR_ID = "SELECT " + COLONNES + " FROM depenses WHERE id = ?";
    private static final String INSERT = "INSERT INTO depenses (mission_id, chauffeur_id, type, station,"
            + " montant, litres, date, statut, motif_refus) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE depenses SET mission_id = ?, chauffeur_id = ?, type = ?,"
            + " station = ?, montant = ?, litres = ?, date = ?, statut = ?, motif_refus = ? WHERE id = ?";

    private final ConnexionBase base;

    public JdbcDepenseDao(ConnexionBase base) {
        this.base = base;
    }

    @Override
    public Depense save(Depense depense) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                remplir(ps, depense);
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    depense.setId(cles.getLong(1));
                }
                return depense;
            }
        });
    }

    /** @throws IllegalArgumentException if no row has this id (same as the in-memory DAO) */
    @Override
    public void update(Depense depense) {
        int lignes = SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(UPDATE)) {
                remplir(ps, depense);
                ps.setLong(10, depense.getId());
                return ps.executeUpdate();
            }
        });
        if (lignes == 0) {
            throw new IllegalArgumentException("Dépense inconnue : " + depense.getId());
        }
    }

    @Override
    public List<Depense> findAll() {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(SELECT_TOUS);
                 ResultSet rs = ps.executeQuery()) {
                List<Depense> resultat = new ArrayList<>();
                while (rs.next()) {
                    resultat.add(lire(rs));
                }
                return resultat;
            }
        });
    }

    @Override
    public Optional<Depense> findById(Long id) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(SELECT_PAR_ID)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(lire(rs)) : Optional.empty();
                }
            }
        });
    }

    /** Parameters 1 to 9, in the column order of INSERT and UPDATE. */
    private static void remplir(PreparedStatement ps, Depense d) throws SQLException {
        ps.setLong(1, d.getMissionId());
        ps.setLong(2, d.getChauffeurId());
        ps.setString(3, d.getType().name());
        ps.setString(4, d.getStation());
        ps.setBigDecimal(5, d.getMontant());
        if (d.getLitres() == null) {
            ps.setNull(6, Types.DECIMAL);
        } else {
            ps.setBigDecimal(6, d.getLitres());
        }
        ps.setDate(7, Date.valueOf(d.getDate()));
        ps.setString(8, d.getStatut().name());
        if (d.getMotifRefus() == null) {
            ps.setNull(9, Types.VARCHAR);
        } else {
            ps.setString(9, d.getMotifRefus());
        }
    }

    private static Depense lire(ResultSet rs) throws SQLException {
        Depense d = new Depense();
        d.setId(rs.getLong("id"));
        d.setMissionId(rs.getLong("mission_id"));
        d.setChauffeurId(rs.getLong("chauffeur_id"));
        d.setType(TypeDepense.valueOf(rs.getString("type")));
        d.setStation(rs.getString("station"));
        d.setMontant(rs.getBigDecimal("montant"));
        BigDecimal litres = rs.getBigDecimal("litres");
        d.setLitres(litres);
        Date date = rs.getDate("date");
        d.setDate(date == null ? null : date.toLocalDate());
        d.setStatut(StatutDepense.valueOf(rs.getString("statut")));
        d.setMotifRefus(rs.getString("motif_refus"));
        return d;
    }
}
