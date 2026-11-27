package ma.translogix.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ma.translogix.dao.BonCarburantDao;
import ma.translogix.model.BonCarburant;
import ma.translogix.model.StatutBonCarburant;

/**
 * Fuel vouchers in MySQL (table bons_carburant). Every value is bound as a PreparedStatement parameter.
 * Not plugged into AppContext: that shared composition root is outside the Dispatcher iteration.
 */
public class JdbcBonCarburantDao implements BonCarburantDao {

    private static final String COLONNES = "id, reference, mission_id, vehicule_id, chauffeur_id,"
            + " station, litres, montant, date_bon, statut";
    private static final String FROM = " FROM bons_carburant";
    private static final String SELECT_TOUS = "SELECT " + COLONNES + FROM + " ORDER BY date_bon DESC, reference DESC";
    private static final String SELECT_PAR_ID = "SELECT " + COLONNES + FROM + " WHERE id = ?";
    private static final String SELECT_PAR_MISSION =
            "SELECT " + COLONNES + FROM + " WHERE mission_id = ? ORDER BY date_bon DESC, reference DESC";
    private static final String INSERT = "INSERT INTO bons_carburant (reference, mission_id, vehicule_id,"
            + " chauffeur_id, station, litres, montant, date_bon, statut) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE bons_carburant SET reference = ?, mission_id = ?, vehicule_id = ?,"
            + " chauffeur_id = ?, station = ?, litres = ?, montant = ?, date_bon = ?, statut = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM bons_carburant WHERE id = ?";

    private final ConnexionBase base;

    public JdbcBonCarburantDao(ConnexionBase base) {
        this.base = base;
    }

    @Override
    public List<BonCarburant> findAll() {
        return lister(SELECT_TOUS, List.of());
    }

    @Override
    public Optional<BonCarburant> findById(Long id) {
        return lister(SELECT_PAR_ID, List.of(id)).stream().findFirst();
    }

    @Override
    public List<BonCarburant> findByMission(Long missionId) {
        return lister(SELECT_PAR_MISSION, List.of(missionId));
    }

    @Override
    public BonCarburant save(BonCarburant bon) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                remplir(ps, bon);
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    bon.setId(cles.getLong(1));
                }
                return bon;
            }
        });
    }

    @Override
    public void update(BonCarburant bon) {
        int lignes = SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(UPDATE)) {
                remplir(ps, bon);
                ps.setLong(10, bon.getId());
                return ps.executeUpdate();
            }
        });
        if (lignes == 0) {
            throw new IllegalArgumentException("Bon de carburant inconnu : " + bon.getId());
        }
    }

    @Override
    public void delete(Long id) {
        SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(DELETE)) {
                ps.setLong(1, id);
                return ps.executeUpdate();
            }
        });
    }

    private List<BonCarburant> lister(String sql, List<Object> valeurs) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < valeurs.size(); i++) {
                    ps.setObject(i + 1, valeurs.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<BonCarburant> resultat = new ArrayList<>();
                    while (rs.next()) {
                        resultat.add(new BonCarburant(rs.getLong("id"), rs.getString("reference"),
                                rs.getLong("mission_id"), rs.getLong("vehicule_id"), rs.getLong("chauffeur_id"),
                                rs.getString("station"), rs.getBigDecimal("litres"), rs.getBigDecimal("montant"),
                                rs.getObject("date_bon", LocalDate.class),
                                StatutBonCarburant.valueOf(rs.getString("statut"))));
                    }
                    return resultat;
                }
            }
        });
    }

    /** Parameters 1 to 9, in the column order of INSERT and UPDATE. */
    private static void remplir(PreparedStatement ps, BonCarburant bon) throws SQLException {
        ps.setString(1, bon.getReference());
        ps.setObject(2, bon.getMissionId());
        ps.setObject(3, bon.getVehiculeId());
        ps.setObject(4, bon.getChauffeurId());
        ps.setString(5, bon.getStation());
        ps.setBigDecimal(6, bon.getLitres());
        ps.setBigDecimal(7, bon.getMontant());
        ps.setObject(8, bon.getDate());
        ps.setString(9, bon.getStatut() == null ? null : bon.getStatut().name());
    }
}
