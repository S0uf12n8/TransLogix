package ma.translogix.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ma.translogix.dao.IncidentDao;
import ma.translogix.model.GraviteIncident;
import ma.translogix.model.Incident;
import ma.translogix.model.StatutIncident;
import ma.translogix.model.TypeIncident;

/**
 * Road incidents in MySQL (table incidents). Every value is bound as a PreparedStatement parameter.
 * Not plugged into AppContext: that shared composition root is outside the Dispatcher iteration.
 */
public class JdbcIncidentDao implements IncidentDao {

    private static final String COLONNES = "id, reference, mission_id, type, gravite, description,"
            + " localisation, date_signalement, statut";
    private static final String FROM = " FROM incidents";
    private static final String SELECT_TOUS = "SELECT " + COLONNES + FROM + " ORDER BY date_signalement DESC";
    private static final String SELECT_PAR_ID = "SELECT " + COLONNES + FROM + " WHERE id = ?";
    private static final String SELECT_PAR_MISSION =
            "SELECT " + COLONNES + FROM + " WHERE mission_id = ? ORDER BY date_signalement DESC";
    private static final String INSERT = "INSERT INTO incidents (reference, mission_id, type, gravite,"
            + " description, localisation, date_signalement, statut) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE incidents SET reference = ?, mission_id = ?, type = ?,"
            + " gravite = ?, description = ?, localisation = ?, date_signalement = ?, statut = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM incidents WHERE id = ?";

    private final ConnexionBase base;

    public JdbcIncidentDao(ConnexionBase base) {
        this.base = base;
    }

    @Override
    public List<Incident> findAll() {
        return lister(SELECT_TOUS, List.of());
    }

    @Override
    public Optional<Incident> findById(Long id) {
        return lister(SELECT_PAR_ID, List.of(id)).stream().findFirst();
    }

    @Override
    public List<Incident> findByMission(Long missionId) {
        return lister(SELECT_PAR_MISSION, List.of(missionId));
    }

    @Override
    public Incident save(Incident incident) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                remplir(ps, incident);
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    incident.setId(cles.getLong(1));
                }
                return incident;
            }
        });
    }

    @Override
    public void update(Incident incident) {
        int lignes = SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(UPDATE)) {
                remplir(ps, incident);
                ps.setLong(9, incident.getId());
                return ps.executeUpdate();
            }
        });
        if (lignes == 0) {
            throw new IllegalArgumentException("Incident inconnu : " + incident.getId());
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

    private List<Incident> lister(String sql, List<Object> valeurs) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < valeurs.size(); i++) {
                    ps.setObject(i + 1, valeurs.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<Incident> resultat = new ArrayList<>();
                    while (rs.next()) {
                        resultat.add(new Incident(rs.getLong("id"), rs.getString("reference"),
                                rs.getLong("mission_id"), TypeIncident.valueOf(rs.getString("type")),
                                GraviteIncident.valueOf(rs.getString("gravite")), rs.getString("description"),
                                rs.getString("localisation"), rs.getObject("date_signalement", LocalDateTime.class),
                                StatutIncident.valueOf(rs.getString("statut"))));
                    }
                    return resultat;
                }
            }
        });
    }

    /** Parameters 1 to 8, in the column order of INSERT and UPDATE. */
    private static void remplir(PreparedStatement ps, Incident incident) throws SQLException {
        ps.setString(1, incident.getReference());
        ps.setObject(2, incident.getMissionId());
        ps.setString(3, incident.getType() == null ? null : incident.getType().name());
        ps.setString(4, incident.getGravite() == null ? null : incident.getGravite().name());
        ps.setString(5, incident.getDescription());
        ps.setString(6, incident.getLocalisation());
        ps.setObject(7, incident.getDateSignalement());
        ps.setString(8, incident.getStatut() == null ? null : incident.getStatut().name());
    }
}
