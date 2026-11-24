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
import java.util.Optional;
import ma.translogix.dao.EmployeDao;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;
import ma.translogix.model.TypeContrat;

/**
 * Employees in MySQL (table employes, see db/schema.sql). Same contract as InMemoryEmployeDao.
 * Every query is a PreparedStatement with ? parameters, names its columns, and goes through
 * SqlErreurs so database errors reach the user in French.
 */
public class JdbcEmployeDao implements EmployeDao {

    private static final String COLONNES =
            "id, matricule, nom, prenom, cin, cnss, poste, type_contrat, date_embauche, actif, utilisateur_id";
    private static final String SELECT_TOUS = "SELECT " + COLONNES + " FROM employes ORDER BY id";
    private static final String SELECT_PAR_ID = "SELECT " + COLONNES + " FROM employes WHERE id = ?";
    private static final String SELECT_PAR_POSTE = "SELECT " + COLONNES + " FROM employes WHERE poste = ? ORDER BY id";
    private static final String SELECT_PAR_UTILISATEUR =
            "SELECT " + COLONNES + " FROM employes WHERE utilisateur_id = ? ORDER BY id";
    /** Exact match on the stored (upper-case) CIN; the UNIQUE index makes this a single-row lookup. */
    private static final String SELECT_PAR_CIN = "SELECT " + COLONNES + " FROM employes WHERE cin = ? ORDER BY id";
    private static final String SELECT_PAR_MATRICULE =
            "SELECT " + COLONNES + " FROM employes WHERE matricule = ? ORDER BY id";
    private static final String INSERT = "INSERT INTO employes"
            + " (matricule, nom, prenom, cin, cnss, poste, type_contrat, date_embauche, actif, utilisateur_id)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE employes SET matricule = ?, nom = ?, prenom = ?, cin = ?,"
            + " cnss = ?, poste = ?, type_contrat = ?, date_embauche = ?, actif = ?, utilisateur_id = ?"
            + " WHERE id = ?";
    private static final String DELETE = "DELETE FROM employes WHERE id = ?";

    private final ConnexionBase base;

    public JdbcEmployeDao(ConnexionBase base) {
        this.base = base;
    }

    @Override
    public List<Employe> findAll() {
        return SqlErreurs.executer(() -> plusieurs(SELECT_TOUS, ps -> { }));
    }

    @Override
    public Optional<Employe> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return SqlErreurs.executer(() -> unSeul(SELECT_PAR_ID, ps -> ps.setLong(1, id)));
    }

    @Override
    public List<Employe> findByPoste(Poste poste) {
        if (poste == null) {
            return List.of();
        }
        return SqlErreurs.executer(() -> plusieurs(SELECT_PAR_POSTE, ps -> ps.setString(1, poste.name())));
    }

    @Override
    public Optional<Employe> findByUtilisateurId(Long utilisateurId) {
        if (utilisateurId == null) {
            return Optional.empty();
        }
        return SqlErreurs.executer(() -> unSeul(SELECT_PAR_UTILISATEUR, ps -> ps.setLong(1, utilisateurId)));
    }

    @Override
    public Optional<Employe> findByCin(String cin) {
        if (cin == null) {
            return Optional.empty();
        }
        return SqlErreurs.executer(() -> unSeul(SELECT_PAR_CIN, ps -> ps.setString(1, cin)));
    }

    @Override
    public Optional<Employe> findByMatricule(String matricule) {
        if (matricule == null) {
            return Optional.empty();
        }
        return SqlErreurs.executer(() -> unSeul(SELECT_PAR_MATRICULE, ps -> ps.setString(1, matricule)));
    }

    @Override
    public Employe save(Employe employe) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                remplir(ps, employe);
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    employe.setId(cles.getLong(1));
                }
                return employe;
            }
        });
    }

    /** @throws IllegalArgumentException if no row has this id (same as the in-memory DAO) */
    @Override
    public void update(Employe employe) {
        Long id = employe.getId();
        if (id == null) {
            throw new IllegalArgumentException("Employé inconnu : null");
        }
        int lignes = SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(UPDATE)) {
                remplir(ps, employe);
                ps.setLong(11, id);
                return ps.executeUpdate();
            }
        });
        if (lignes == 0) {
            throw new IllegalArgumentException("Employé inconnu : " + id);
        }
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            return;
        }
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

    private Optional<Employe> unSeul(String sql, Parametres parametres) throws SQLException {
        try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            parametres.appliquer(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lire(rs)) : Optional.empty();
            }
        }
    }

    private List<Employe> plusieurs(String sql, Parametres parametres) throws SQLException {
        try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            parametres.appliquer(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Employe> resultat = new ArrayList<>();
                while (rs.next()) {
                    resultat.add(lire(rs));
                }
                return resultat;
            }
        }
    }

    /** Parameters 1 to 10, in the column order of INSERT and UPDATE. */
    private static void remplir(PreparedStatement ps, Employe employe) throws SQLException {
        ps.setString(1, employe.getMatricule());
        ps.setString(2, employe.getNom());
        ps.setString(3, employe.getPrenom());
        ps.setString(4, employe.getCin());
        setNullableString(ps, 5, employe.getCnss());
        setNullableString(ps, 6, nom(employe.getPoste()));
        setNullableString(ps, 7, nom(employe.getTypeContrat()));
        setDate(ps, 8, employe.getDateEmbauche());
        ps.setBoolean(9, employe.isActif());
        setNullableLong(ps, 10, employe.getUtilisateurId());
    }

    private static Employe lire(ResultSet rs) throws SQLException {
        long utilisateurBrut = rs.getLong("utilisateur_id");
        Long utilisateurId = rs.wasNull() ? null : utilisateurBrut;
        return new Employe(rs.getLong("id"), rs.getString("matricule"), rs.getString("nom"),
                rs.getString("prenom"), rs.getString("cin"), rs.getString("cnss"),
                Poste.valueOf(rs.getString("poste")), TypeContrat.valueOf(rs.getString("type_contrat")),
                rs.getDate("date_embauche").toLocalDate(), rs.getBoolean("actif"), utilisateurId);
    }

    private static String nom(Enum<?> valeur) {
        return valeur == null ? null : valeur.name();
    }

    private static void setNullableString(PreparedStatement ps, int index, String valeur) throws SQLException {
        if (valeur == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, valeur);
        }
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long valeur) throws SQLException {
        if (valeur == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, valeur);
        }
    }

    private static void setDate(PreparedStatement ps, int index, LocalDate valeur) throws SQLException {
        if (valeur == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(valeur));
        }
    }
}
