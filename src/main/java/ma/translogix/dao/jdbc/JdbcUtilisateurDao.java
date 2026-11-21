package ma.translogix.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ma.translogix.dao.UtilisateurDao;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;

/**
 * User accounts in MySQL (table utilisateurs, see db/schema.sql). Same contract as InMemoryUtilisateurDao.
 * Every query is a PreparedStatement with ? parameters (no string concatenation: no SQL injection),
 * names its columns (no SELECT *), and goes through SqlErreurs so errors reach the user in French.
 */
public class JdbcUtilisateurDao implements UtilisateurDao {

    private static final String COLONNES = "id, nom, prenom, email, mot_de_passe_hash, role, actif";
    private static final String SELECT_TOUS = "SELECT " + COLONNES + " FROM utilisateurs ORDER BY id";
    private static final String SELECT_PAR_ID = "SELECT " + COLONNES + " FROM utilisateurs WHERE id = ?";
    /** Uses the UNIQUE index on email; the column collation makes it case-insensitive. */
    private static final String SELECT_PAR_EMAIL = "SELECT " + COLONNES + " FROM utilisateurs WHERE email = ?";
    private static final String INSERT = "INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, role, actif)"
            + " VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE utilisateurs SET nom = ?, prenom = ?, email = ?,"
            + " mot_de_passe_hash = ?, role = ?, actif = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM utilisateurs WHERE id = ?";

    private final ConnexionBase base;

    public JdbcUtilisateurDao(ConnexionBase base) {
        this.base = base;
    }

    @Override
    public List<Utilisateur> findAll() {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(SELECT_TOUS);
                 ResultSet rs = ps.executeQuery()) {
                List<Utilisateur> resultat = new ArrayList<>();
                while (rs.next()) {
                    resultat.add(lire(rs));
                }
                return resultat;
            }
        });
    }

    @Override
    public Optional<Utilisateur> findById(Long id) {
        return SqlErreurs.executer(() -> unSeul(SELECT_PAR_ID, ps -> ps.setLong(1, id)));
    }

    @Override
    public Optional<Utilisateur> findByEmail(String email) {
        return SqlErreurs.executer(() -> unSeul(SELECT_PAR_EMAIL, ps -> ps.setString(1, email)));
    }

    @Override
    public Utilisateur save(Utilisateur u) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                remplir(ps, u);
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    u.setId(cles.getLong(1));
                }
                return u;
            }
        });
    }

    /** @throws IllegalArgumentException if no row has this id (same as the in-memory DAO) */
    @Override
    public void update(Utilisateur u) {
        int lignes = SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(UPDATE)) {
                remplir(ps, u);
                ps.setLong(7, u.getId());
                return ps.executeUpdate();
            }
        });
        if (lignes == 0) {
            throw new IllegalArgumentException("Utilisateur inconnu : " + u.getId());
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

    private Optional<Utilisateur> unSeul(String sql, Parametres parametres) throws SQLException {
        try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            parametres.appliquer(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lire(rs)) : Optional.empty();
            }
        }
    }

    /** Parameters 1 to 6, in the column order of INSERT and UPDATE. */
    private static void remplir(PreparedStatement ps, Utilisateur u) throws SQLException {
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getMotDePasseHash());
        ps.setString(5, u.getRole().name());
        ps.setBoolean(6, u.isActif());
    }

    private static Utilisateur lire(ResultSet rs) throws SQLException {
        return new Utilisateur(rs.getLong("id"), rs.getString("nom"), rs.getString("prenom"), rs.getString("email"),
                rs.getString("mot_de_passe_hash"), Role.valueOf(rs.getString("role")), rs.getBoolean("actif"));
    }
}
