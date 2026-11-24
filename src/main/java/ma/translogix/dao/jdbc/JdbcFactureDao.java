package ma.translogix.dao.jdbc;

import java.math.BigDecimal;
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
import java.util.Optional;
import ma.translogix.dao.CritereFacture;
import ma.translogix.dao.FactureDao;
import ma.translogix.model.Facture;
import ma.translogix.model.StatutFacture;

/**
 * Invoices in MySQL (table factures, see db/schema.sql). Same contract as InMemoryFactureDao.
 * Search builds one PreparedStatement: only '?' placeholders are appended to the SQL text, every value
 * stays a parameter. Errors are translated to French by SqlErreurs.
 */
public class JdbcFactureDao implements FactureDao {

    private static final String COLONNES = "id, numero, client_id, mission_id, reference_trajet,"
            + " montant_ht, taux_tva, montant_tva, montant_ttc, date_emission, date_echeance, statut";
    private static final String SELECT_TOUS = "SELECT " + COLONNES + " FROM factures ORDER BY id";
    private static final String SELECT_PAR_ID = "SELECT " + COLONNES + " FROM factures WHERE id = ?";
    private static final String SELECT_PAR_CLIENT =
            "SELECT " + COLONNES + " FROM factures WHERE client_id = ? ORDER BY id";
    private static final String SELECT_PAR_MISSION =
            "SELECT " + COLONNES + " FROM factures WHERE mission_id = ?";
    private static final String INSERT = "INSERT INTO factures (numero, client_id, mission_id,"
            + " reference_trajet, montant_ht, taux_tva, montant_tva, montant_ttc,"
            + " date_emission, date_echeance, statut) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE factures SET numero = ?, client_id = ?, mission_id = ?,"
            + " reference_trajet = ?, montant_ht = ?, taux_tva = ?, montant_tva = ?, montant_ttc = ?,"
            + " date_emission = ?, date_echeance = ?, statut = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM factures WHERE id = ?";

    private final ConnexionBase base;

    public JdbcFactureDao(ConnexionBase base) {
        this.base = base;
    }

    @Override
    public List<Facture> findAll() {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(SELECT_TOUS);
                 ResultSet rs = ps.executeQuery()) {
                List<Facture> resultat = new ArrayList<>();
                while (rs.next()) {
                    resultat.add(lire(rs));
                }
                return resultat;
            }
        });
    }

    @Override
    public Optional<Facture> findById(Long id) {
        return SqlErreurs.executer(() -> unSeul(SELECT_PAR_ID, ps -> ps.setLong(1, id)));
    }

    @Override
    public List<Facture> findByClient(Long clientId) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(SELECT_PAR_CLIENT)) {
                ps.setLong(1, clientId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Facture> resultat = new ArrayList<>();
                    while (rs.next()) {
                        resultat.add(lire(rs));
                    }
                    return resultat;
                }
            }
        });
    }

    @Override
    public Optional<Facture> findByMission(Long missionId) {
        if (missionId == null) {
            return Optional.empty();
        }
        return SqlErreurs.executer(() -> unSeul(SELECT_PAR_MISSION, ps -> ps.setLong(1, missionId)));
    }

    @Override
    public List<Facture> search(CritereFacture c) {
        return SqlErreurs.executer(() -> {
            CritereFacture critere = c == null ? CritereFacture.TOUT : c;
            StringBuilder sql = new StringBuilder("SELECT ").append(COLONNES)
                    .append(" FROM factures WHERE 1 = 1");
            List<Parametre> parametres = new ArrayList<>();

            String motCle = critere.motCle();
            if (motCle != null && !motCle.isBlank()) {
                String motif = motif(motCle);
                sql.append(" AND (numero LIKE ? ESCAPE '\\\\' OR reference_trajet LIKE ? ESCAPE '\\\\'");
                parametres.add((ps, index) -> ps.setString(index, motif));
                parametres.add((ps, index) -> ps.setString(index, motif));
                List<Long> idsClients = critere.idsClientsMotes() == null
                        ? List.of() : critere.idsClientsMotes();
                if (!idsClients.isEmpty()) {
                    sql.append(" OR client_id IN (");
                    for (int i = 0; i < idsClients.size(); i++) {
                        if (i > 0) {
                            sql.append(", ");
                        }
                        sql.append('?');
                        long idClient = idsClients.get(i);
                        parametres.add((ps, index) -> ps.setLong(index, idClient));
                    }
                    sql.append(')');
                }
                sql.append(')');
            }

            if (critere.clientId() != null) {
                sql.append(" AND client_id = ?");
                long idClient = critere.clientId();
                parametres.add((ps, index) -> ps.setLong(index, idClient));
            }
            if (critere.statut() != null) {
                sql.append(" AND statut = ?");
                String statut = codeStatut(critere.statut());
                parametres.add((ps, index) -> ps.setString(index, statut));
            }
            if (critere.du() != null) {
                sql.append(" AND date_emission >= ?");
                LocalDate du = critere.du();
                parametres.add((ps, index) -> ps.setDate(index, Date.valueOf(du)));
            }
            if (critere.au() != null) {
                sql.append(" AND date_emission <= ?");
                LocalDate au = critere.au();
                parametres.add((ps, index) -> ps.setDate(index, Date.valueOf(au)));
            }
            if (critere.montantMin() != null) {
                sql.append(" AND montant_ttc >= ?");
                BigDecimal min = critere.montantMin();
                parametres.add((ps, index) -> ps.setBigDecimal(index, min));
            }
            if (critere.montantMax() != null) {
                sql.append(" AND montant_ttc <= ?");
                BigDecimal max = critere.montantMax();
                parametres.add((ps, index) -> ps.setBigDecimal(index, max));
            }
            sql.append(" ORDER BY id");

            try (Connection co = base.ouvrir(); PreparedStatement ps = co.prepareStatement(sql.toString())) {
                for (int i = 0; i < parametres.size(); i++) {
                    parametres.get(i).appliquer(ps, i + 1);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<Facture> resultat = new ArrayList<>();
                    while (rs.next()) {
                        resultat.add(lire(rs));
                    }
                    return resultat;
                }
            }
        });
    }

    @Override
    public Facture save(Facture facture) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                remplir(ps, facture);
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    facture.setId(cles.getLong(1));
                }
                return facture;
            }
        });
    }

    /** @throws IllegalArgumentException if no row has this id (same as the in-memory DAO) */
    @Override
    public void update(Facture facture) {
        int lignes = SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(UPDATE)) {
                remplir(ps, facture);
                ps.setLong(12, facture.getId());
                return ps.executeUpdate();
            }
        });
        if (lignes == 0) {
            throw new IllegalArgumentException("Facture inconnue : " + facture.getId());
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

    @FunctionalInterface
    private interface Parametres {
        void appliquer(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    private interface Parametre {
        void appliquer(PreparedStatement ps, int index) throws SQLException;
    }

    private Optional<Facture> unSeul(String sql, Parametres parametres) throws SQLException {
        try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            parametres.appliquer(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lire(rs)) : Optional.empty();
            }
        }
    }

    /** Parameters 1 to 11, in the column order of INSERT and UPDATE. */
    private static void remplir(PreparedStatement ps, Facture facture) throws SQLException {
        ps.setString(1, facture.getNumero());
        ps.setLong(2, facture.getClientId());
        setLongNullable(ps, 3, facture.getMissionId());
        ps.setString(4, facture.getReferenceTrajet());
        ps.setBigDecimal(5, facture.getMontantHT());
        ps.setBigDecimal(6, facture.getTauxTva());
        ps.setBigDecimal(7, facture.getMontantTva());
        ps.setBigDecimal(8, facture.getMontantTtc());
        setDate(ps, 9, facture.getDateEmission());
        setDate(ps, 10, facture.getDateEcheance());
        ps.setString(11, facture.getStatut().name());
    }

    private static Facture lire(ResultSet rs) throws SQLException {
        Facture facture = new Facture();
        facture.setId(rs.getLong("id"));
        facture.setNumero(rs.getString("numero"));
        facture.setClientId(rs.getLong("client_id"));
        long missionId = rs.getLong("mission_id");
        facture.setMissionId(rs.wasNull() ? null : missionId);
        facture.setReferenceTrajet(rs.getString("reference_trajet"));
        facture.setMontantHT(rs.getBigDecimal("montant_ht"));
        facture.setTauxTva(rs.getBigDecimal("taux_tva"));
        facture.setMontantTva(rs.getBigDecimal("montant_tva"));
        facture.setMontantTtc(rs.getBigDecimal("montant_ttc"));
        Date emission = rs.getDate("date_emission");
        facture.setDateEmission(emission == null ? null : emission.toLocalDate());
        Date echeance = rs.getDate("date_echeance");
        facture.setDateEcheance(echeance == null ? null : echeance.toLocalDate());
        facture.setStatut(StatutFacture.valueOf(rs.getString("statut")));
        return facture;
    }

    /** LIKE treats % and _ as wildcards; the in-memory DAO uses String.contains, so escape them. */
    private static String motif(String motCle) {
        String echappe = motCle.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + echappe + "%";
    }

    /** CritereFacture carries the French label; the database stores the enum name(). */
    private static String codeStatut(String libelle) {
        for (StatutFacture statut : StatutFacture.values()) {
            if (statut.getLibelle().equals(libelle)) {
                return statut.name();
            }
        }
        return ""; // unknown label: no stored status can match, like the in-memory DAO
    }

    private static void setLongNullable(PreparedStatement ps, int index, Long valeur) throws SQLException {
        if (valeur == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, valeur);
        }
    }

    private static void setDate(PreparedStatement ps, int index, LocalDate date) throws SQLException {
        if (date == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(date));
        }
    }
}
