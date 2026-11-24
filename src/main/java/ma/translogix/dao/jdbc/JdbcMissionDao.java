package ma.translogix.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import ma.translogix.dao.CritereMission;
import ma.translogix.dao.MissionDao;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;

/**
 * Missions in MySQL (table missions, see the dispatcher schema). Same contract as InMemoryMissionDao.
 * Every query is a PreparedStatement with ? parameters: the multi-criteria search appends its WHERE
 * clauses conditionally, but never concatenates user input into SQL.
 */
public class JdbcMissionDao implements MissionDao {

    private static final String COLONNES = "id, reference, client_id, vehicule_id, chauffeur_id,"
            + " ville_depart, ville_arrivee, cargaison, poids_tonnes, date_debut, date_fin, statut";
    private static final String FROM = " FROM missions";
    private static final String SELECT_TOUS = "SELECT " + COLONNES + FROM + " ORDER BY reference";
    private static final String SELECT_PAR_ID = "SELECT " + COLONNES + FROM + " WHERE id = ?";
    private static final String SELECT_PAR_CHAUFFEUR =
            "SELECT " + COLONNES + FROM + " WHERE chauffeur_id = ? ORDER BY reference";
    private static final String SELECT_PAR_VEHICULE =
            "SELECT " + COLONNES + FROM + " WHERE vehicule_id = ? ORDER BY reference";
    private static final String INSERT = "INSERT INTO missions (reference, client_id, vehicule_id, chauffeur_id,"
            + " ville_depart, ville_arrivee, cargaison, poids_tonnes, date_debut, date_fin, statut)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE missions SET reference = ?, client_id = ?, vehicule_id = ?,"
            + " chauffeur_id = ?, ville_depart = ?, ville_arrivee = ?, cargaison = ?, poids_tonnes = ?,"
            + " date_debut = ?, date_fin = ?, statut = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM missions WHERE id = ?";
    private static final String COMPTAGE_STATUTS =
            "SELECT statut, COUNT(*) AS total FROM missions GROUP BY statut";
    private static final String VEHICULES_EN_MISSION =
            "SELECT DISTINCT vehicule_id FROM missions WHERE statut = ?";

    private final ConnexionBase base;

    public JdbcMissionDao(ConnexionBase base) {
        this.base = base;
    }

    @Override
    public List<Mission> findAll() {
        return lister(SELECT_TOUS, List.of());
    }

    @Override
    public Optional<Mission> findById(Long id) {
        return lister(SELECT_PAR_ID, parametres(id)).stream().findFirst();
    }

    @Override
    public List<Mission> findByChauffeur(Long chauffeurId) {
        return lister(SELECT_PAR_CHAUFFEUR, parametres(chauffeurId));
    }

    @Override
    public List<Mission> findByVehicule(Long vehiculeId) {
        return lister(SELECT_PAR_VEHICULE, parametres(vehiculeId));
    }

    /**
     * Reference, driver ids and cities are independent optional filters. Each one adds exactly one
     * parameterized clause; an empty driver-id list closes the search instead of building "IN ()".
     */
    @Override
    public List<Mission> search(CritereMission c) {
        CritereMission critere = c == null ? CritereMission.TOUT : c;
        StringBuilder sql = new StringBuilder("SELECT ").append(COLONNES).append(FROM).append(" WHERE 1 = 1");
        List<Object> valeurs = new ArrayList<>();

        String reference = texte(critere.reference());
        if (!reference.isEmpty()) {
            sql.append(" AND LOWER(reference) LIKE LOWER(?)");
            valeurs.add("%" + reference + "%");
        }

        String chauffeur = texte(critere.chauffeur());
        if (!chauffeur.isEmpty()) {
            List<Long> ids = critere.idsChauffeursMotes() == null
                    ? List.of() : critere.idsChauffeursMotes();
            if (ids.isEmpty()) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND chauffeur_id IN (")
                        .append(String.join(",", Collections.nCopies(ids.size(), "?"))).append(')');
                valeurs.addAll(ids);
            }
        }

        String ville = texte(critere.ville());
        if (!ville.isEmpty()) {
            sql.append(" AND (LOWER(ville_depart) LIKE LOWER(?) OR LOWER(ville_arrivee) LIKE LOWER(?))");
            valeurs.add("%" + ville + "%");
            valeurs.add("%" + ville + "%");
        }
        sql.append(" ORDER BY reference");
        return lister(sql.toString(), valeurs);
    }

    @Override
    public Map<StatutMission, Long> compterParStatut() {
        return SqlErreurs.executer(() -> {
            Map<StatutMission, Long> comptages = new EnumMap<>(StatutMission.class);
            for (StatutMission statut : StatutMission.values()) {
                comptages.put(statut, 0L);
            }
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(COMPTAGE_STATUTS);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String statut = rs.getString("statut");
                    if (statut != null) {
                        try {
                            comptages.merge(StatutMission.valueOf(statut), rs.getLong("total"), Long::sum);
                        } catch (IllegalArgumentException ignore) {
                            // A status unknown to this version is not shown by the dispatch charts.
                        }
                    }
                }
            }
            return comptages;
        });
    }

    @Override
    public Set<Long> findVehiculesEnMission() {
        return SqlErreurs.executer(() -> {
            Set<Long> ids = new LinkedHashSet<>();
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(VEHICULES_EN_MISSION)) {
                ps.setString(1, StatutMission.EN_COURS.name());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("vehicule_id");
                        if (!rs.wasNull()) {
                            ids.add(id);
                        }
                    }
                }
            }
            return ids;
        });
    }

    @Override
    public Mission save(Mission mission) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                remplir(ps, mission);
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    mission.setId(cles.getLong(1));
                }
                return mission;
            }
        });
    }

    @Override
    public void update(Mission mission) {
        int lignes = SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(UPDATE)) {
                remplir(ps, mission);
                ps.setLong(12, mission.getId());
                return ps.executeUpdate();
            }
        });
        if (lignes == 0) {
            throw new IllegalArgumentException("Mission inconnue : " + mission.getId());
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

    private List<Mission> lister(String sql, List<Object> valeurs) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < valeurs.size(); i++) {
                    ps.setObject(i + 1, valeurs.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<Mission> resultat = new ArrayList<>();
                    while (rs.next()) {
                        resultat.add(lire(rs));
                    }
                    return resultat;
                }
            }
        });
    }

    /** Parameters 1 to 11, in the column order of INSERT and UPDATE. */
    private static void remplir(PreparedStatement ps, Mission m) throws SQLException {
        ps.setString(1, m.getReference());
        ps.setObject(2, m.getClientId());
        ps.setObject(3, m.getVehiculeId());
        ps.setObject(4, m.getChauffeurId());
        ps.setString(5, m.getVilleDepart());
        ps.setString(6, m.getVilleArrivee());
        ps.setString(7, m.getCargaison());
        ps.setBigDecimal(8, m.getPoidsTonnes());
        ps.setObject(9, m.getDateDebut());
        ps.setObject(10, m.getDateFin());
        ps.setString(11, m.getStatut() == null ? null : m.getStatut().name());
    }

    private static Mission lire(ResultSet rs) throws SQLException {
        return new Mission(rs.getLong("id"), rs.getString("reference"), rs.getLong("client_id"),
                rs.getLong("vehicule_id"), rs.getLong("chauffeur_id"), rs.getString("ville_depart"),
                rs.getString("ville_arrivee"), rs.getString("cargaison"), rs.getBigDecimal("poids_tonnes"),
                rs.getObject("date_debut", LocalDateTime.class),
                rs.getObject("date_fin", LocalDateTime.class),
                StatutMission.valueOf(rs.getString("statut")));
    }

    private static List<Object> parametres(Object... valeurs) {
        return Arrays.asList(valeurs);
    }

    private static String texte(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }
}
