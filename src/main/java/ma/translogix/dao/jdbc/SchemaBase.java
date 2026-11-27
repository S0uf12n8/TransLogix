package ma.translogix.dao.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates the tables of db/schema.sql. The script is idempotent (CREATE TABLE IF NOT EXISTS, indexes inline),
 * so the app runs it at every start in MySQL mode: a fresh database gets its tables, an existing one is untouched.
 */
public final class SchemaBase {

    static final String SCRIPT = "/db/schema.sql";

    private SchemaBase() {
    }

    /**
     * Runs every statement of db/schema.sql on one connection.
     * @throws ma.translogix.dao.DonneesException when MySQL is unreachable or refuses a statement
     */
    public static void appliquer(ConnexionBase base) {
        List<String> instructions = instructions();
        SqlErreurs.executer(() -> {
            try (Connection c = base.ouvrir(); Statement st = c.createStatement()) {
                for (String instruction : instructions) {
                    st.execute(instruction);
                }
            }
            return null;
        });
    }

    /** The statements of db/schema.sql: comment lines removed, split on ';' at the end of a line. */
    public static List<String> instructions() {
        try (InputStream in = SchemaBase.class.getResourceAsStream(SCRIPT)) {
            if (in == null) {
                throw new IllegalStateException(SCRIPT + " introuvable");
            }
            return decouper(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<String> decouper(String script) {
        String sansCommentaires = script.lines()
                .filter(l -> !l.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
        return Arrays.stream(sansCommentaires.split(";\\s*(\\n|$)"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
