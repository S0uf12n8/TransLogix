package ma.translogix.dao.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Optional;

/**
 * Where the MySQL database is. Values come from the environment, never from the code or a committed file:
 * DB_URL, DB_USER, DB_PASSWORD (the same names the CI workflow sets). Local Docker (compose.yaml):
 * DB_URL=jdbc:mysql://localhost:3307/translogix_db?useSSL=false&amp;allowPublicKeyRetrieval=true&amp;serverTimezone=UTC
 *
 * One connection per DAO operation, closed right after (try-with-resources). Enough for a desktop app
 * with one user at a time; a pool would be a new dependency.
 */
public final class ConnexionBase {

    private final String url;
    private final String utilisateur;
    private final String motDePasse;

    public ConnexionBase(String url, String utilisateur, String motDePasse) {
        this.url = url;
        this.utilisateur = utilisateur;
        this.motDePasse = motDePasse;
    }

    /** From DB_URL / DB_USER / DB_PASSWORD; empty when DB_URL is not set (the app then stays in memory). */
    public static Optional<ConnexionBase> depuisEnvironnement() {
        return depuis(System.getenv());
    }

    static Optional<ConnexionBase> depuis(Map<String, String> environnement) {
        String url = environnement.get("DB_URL");
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ConnexionBase(url.trim(), environnement.getOrDefault("DB_USER", ""),
                environnement.getOrDefault("DB_PASSWORD", "")));
    }

    /**
     * A new connection, to close by the caller.
     * @throws ma.translogix.dao.DonneesException of type CONNEXION when MySQL cannot be reached
     */
    public Connection ouvrir() {
        return SqlErreurs.executer(() -> DriverManager.getConnection(url, utilisateur, motDePasse));
    }

    /** The URL without credentials, for logs. */
    @Override
    public String toString() {
        return "ConnexionBase[" + url.replaceAll("(?i)(password=)[^&]*", "$1***") + "]";
    }
}
