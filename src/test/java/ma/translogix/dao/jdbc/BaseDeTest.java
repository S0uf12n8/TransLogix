package ma.translogix.dao.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * The real MySQL database of the integration tests, from DB_URL / DB_USER / DB_PASSWORD.
 * CI sets them (MySQL service of .github/workflows/ci.yml); locally the JDBC tests are skipped unless they are set,
 * e.g. against the compose.yaml container on port 3307. Never point DB_URL at a database with real data:
 * vider() deletes every row of the admin tables.
 */
final class BaseDeTest {

    private static boolean schemaApplique;

    private BaseDeTest() {
    }

    static ConnexionBase connexion() {
        return ConnexionBase.depuisEnvironnement()
                .orElseThrow(() -> new IllegalStateException("DB_URL n'est pas défini"));
    }

    /** Applies db/schema.sql once per test run (it is idempotent), then empties the admin tables. */
    static ConnexionBase prete() {
        ConnexionBase base = connexion();
        try (Connection c = base.ouvrir(); Statement st = c.createStatement()) {
            if (!schemaApplique) {
                for (String instruction : instructions()) {
                    st.execute(instruction);
                }
                schemaApplique = true;
            }
            st.execute("DELETE FROM paiements");
            st.execute("DELETE FROM factures");
            st.execute("DELETE FROM clients");
            st.execute("DELETE FROM audit_evenements");
            st.execute("DELETE FROM utilisateurs");
        } catch (SQLException e) {
            throw new IllegalStateException("Préparation de la base de test impossible", e);
        }
        return base;
    }

    /** Runs one raw statement on the test database, errors translated like in the DAOs. */
    static void executer(String sql) {
        SqlErreurs.executer(() -> {
            try (Connection c = connexion().ouvrir(); Statement st = c.createStatement()) {
                return st.execute(sql);
            }
        });
    }

    /** The statements of db/schema.sql, cut exactly as the app does at startup. */
    static List<String> instructions() {
        return SchemaBase.instructions();
    }
}
