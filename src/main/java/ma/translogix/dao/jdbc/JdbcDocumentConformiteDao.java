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
import ma.translogix.dao.DocumentConformiteDao;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.TypeDocument;

/**
 * Compliance documents in MySQL (table documents_conformite, see db/schema.sql). Same contract as
 * InMemoryDocumentConformiteDao. The status is never stored: ConformiteService computes it from the expiry date.
 */
public class JdbcDocumentConformiteDao implements DocumentConformiteDao {

    private static final String COLONNES = "id, employe_id, type_document, numero, categorie, date_expiration";
    private static final String SELECT_TOUS = "SELECT " + COLONNES + " FROM documents_conformite ORDER BY id";
    private static final String SELECT_PAR_ID = "SELECT " + COLONNES + " FROM documents_conformite WHERE id = ?";
    private static final String SELECT_PAR_EMPLOYE =
            "SELECT " + COLONNES + " FROM documents_conformite WHERE employe_id = ? ORDER BY id";
    private static final String INSERT = "INSERT INTO documents_conformite"
            + " (employe_id, type_document, numero, categorie, date_expiration) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE documents_conformite SET employe_id = ?, type_document = ?,"
            + " numero = ?, categorie = ?, date_expiration = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM documents_conformite WHERE id = ?";

    private final ConnexionBase base;

    public JdbcDocumentConformiteDao(ConnexionBase base) {
        this.base = base;
    }

    @Override
    public List<DocumentConformite> findAll() {
        return SqlErreurs.executer(() -> plusieurs(SELECT_TOUS, ps -> { }));
    }

    @Override
    public Optional<DocumentConformite> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return SqlErreurs.executer(() -> unSeul(SELECT_PAR_ID, ps -> ps.setLong(1, id)));
    }

    @Override
    public List<DocumentConformite> findByEmploye(Long employeId) {
        if (employeId == null) {
            return List.of();
        }
        return SqlErreurs.executer(() -> plusieurs(SELECT_PAR_EMPLOYE, ps -> ps.setLong(1, employeId)));
    }

    @Override
    public DocumentConformite save(DocumentConformite document) {
        return SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir();
                 PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                remplir(ps, document);
                ps.executeUpdate();
                try (ResultSet cles = ps.getGeneratedKeys()) {
                    cles.next();
                    document.setId(cles.getLong(1));
                }
                return document;
            }
        });
    }

    /** @throws IllegalArgumentException if no row has this id (same as the in-memory DAO) */
    @Override
    public void update(DocumentConformite document) {
        Long id = document.getId();
        if (id == null) {
            throw new IllegalArgumentException("Document inconnu : null");
        }
        int lignes = SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(UPDATE)) {
                remplir(ps, document);
                ps.setLong(6, id);
                return ps.executeUpdate();
            }
        });
        if (lignes == 0) {
            throw new IllegalArgumentException("Document inconnu : " + id);
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

    private Optional<DocumentConformite> unSeul(String sql, Parametres parametres) throws SQLException {
        try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            parametres.appliquer(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lire(rs)) : Optional.empty();
            }
        }
    }

    private List<DocumentConformite> plusieurs(String sql, Parametres parametres) throws SQLException {
        try (Connection c = base.ouvrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            parametres.appliquer(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<DocumentConformite> resultat = new ArrayList<>();
                while (rs.next()) {
                    resultat.add(lire(rs));
                }
                return resultat;
            }
        }
    }

    /** Parameters 1 to 5, in the column order of INSERT and UPDATE. */
    private static void remplir(PreparedStatement ps, DocumentConformite document) throws SQLException {
        setNullableLong(ps, 1, document.getEmployeId());
        TypeDocument type = document.getType();
        setNullableString(ps, 2, type == null ? null : type.name());
        setNullableString(ps, 3, document.getNumero());
        setNullableString(ps, 4, document.getCategorie());
        setDate(ps, 5, document.getDateExpiration());
    }

    private static DocumentConformite lire(ResultSet rs) throws SQLException {
        return new DocumentConformite(rs.getLong("id"), rs.getLong("employe_id"),
                TypeDocument.valueOf(rs.getString("type_document")), rs.getString("numero"),
                rs.getString("categorie"), rs.getDate("date_expiration").toLocalDate());
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
