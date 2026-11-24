package ma.translogix.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ma.translogix.dao.ClientDao;
import ma.translogix.model.Client;

/**
 * Clients in MySQL (table clients, see db/schema.sql). Same contract as InMemoryClientDao.
 * Every query is a PreparedStatement with ? parameters, names its columns, and goes through
 * SqlErreurs so database errors reach the user in French.
 */
public class JdbcClientDao implements ClientDao {

    private static final String COLONNES =
            "id, nom, ice, telephone, email, adresse, delai_paiement_jours";
    private static final String SELECT_TOUS = "SELECT " + COLONNES + " FROM clients ORDER BY id";
    private static final String SELECT_PAR_ID = "SELECT " + COLONNES + " FROM clients WHERE id = ?";
    /** Uses the UNIQUE index; a null ICE never matches. */
    private static final String SELECT_PAR_ICE = "SELECT " + COLONNES + " FROM clients WHERE ice = ?";
    private static final String INSERT = "INSERT INTO clients"
            + " (nom, ice, telephone, email, adresse, delai_paiement_jours)"
            + " VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE clients SET nom = ?, ice = ?, telephone = ?, email = ?,"
            + " adresse = ?, delai_paiement_jours = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM clients WHERE id = ?";

    private final ConnexionBase base;

    public JdbcClientDao(ConnexionBase base) {
        this.base = base;
    }

    @Override
    public List<Client> findAll() {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(SELECT_TOUS);
                 ResultSet rs = ps.executeQuery()) {
                List<Client> resultat = new ArrayList<>();
                while (rs.next()) {
                    resultat.add(lire(rs));
                }
                return resultat;
            }
        });
    }

    @Override
    public Optional<Client> findById(Long id) {
        return SqlErreurs.executer(() -> unSeul(SELECT_PAR_ID, ps -> ps.setLong(1, id)));
    }

    @Override
    public Optional<Client> findByIce(String ice) {
        return SqlErreurs.executer(() -> {
            if (ice == null || ice.isBlank()) {
                return Optional.empty();
            }
            return unSeul(SELECT_PAR_ICE, ps -> ps.setString(1, ice));
        });
    }

    @Override
    public Client save(Client client) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                remplir(ps, client);
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    client.setId(cles.getLong(1));
                }
                return client;
            }
        });
    }

    /** @throws IllegalArgumentException if no row has this id (same as the in-memory DAO) */
    @Override
    public void update(Client client) {
        int lignes = SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(UPDATE)) {
                remplir(ps, client);
                ps.setLong(7, client.getId());
                return ps.executeUpdate();
            }
        });
        if (lignes == 0) {
            throw new IllegalArgumentException("Client inconnu : " + client.getId());
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

    private Optional<Client> unSeul(String sql, Parametres parametres) throws SQLException {
        try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            parametres.appliquer(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lire(rs)) : Optional.empty();
            }
        }
    }

    /** Parameters 1 to 6, in the column order of INSERT and UPDATE. */
    private static void remplir(PreparedStatement ps, Client client) throws SQLException {
        ps.setString(1, client.getNom());
        ps.setString(2, client.getIce());
        ps.setString(3, client.getTelephone());
        ps.setString(4, client.getEmail());
        ps.setString(5, client.getAdresse());
        ps.setInt(6, client.getDelaiPaiementJours());
    }

    private static Client lire(ResultSet rs) throws SQLException {
        return new Client(rs.getLong("id"), rs.getString("nom"), rs.getString("ice"),
                rs.getString("telephone"), rs.getString("email"), rs.getString("adresse"),
                rs.getInt("delai_paiement_jours"));
    }
}
