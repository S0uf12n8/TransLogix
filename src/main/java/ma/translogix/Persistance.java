package ma.translogix;

import java.util.List;
import java.util.Optional;
import ma.translogix.dao.DonneesException;
import ma.translogix.dao.jdbc.ConnexionBase;
import ma.translogix.dao.jdbc.SchemaBase;

/**
 * Where the app keeps its data, decided once at startup:
 * <ul>
 *   <li>{@link Mode#MYSQL}: DB_URL is set and MySQL answered; db/schema.sql was applied (idempotent).</li>
 *   <li>{@link Mode#MEMOIRE}: DB_URL is not set; everything stays in memory (development, demo without Docker).</li>
 *   <li>{@link Mode#MEMOIRE_SECOURS}: DB_URL is set but MySQL could not be reached. The app still starts, in memory,
 *       tells the user why and shows that nothing will be saved, instead of crashing on the login screen.</li>
 * </ul>
 * Only the tables that have a JDBC DAO are in MySQL; AppContext keeps the other modules in memory until their
 * JDBC DAOs are delivered.
 */
public final class Persistance {

    private static final System.Logger JOURNAL = System.getLogger(Persistance.class.getName());

    public enum Mode { MYSQL, MEMOIRE, MEMOIRE_SECOURS }

    /**
     * @param base     the database, only in MYSQL mode
     * @param probleme the French message of the connection error, only in MEMOIRE_SECOURS mode
     */
    public record Etat(Mode mode, ConnexionBase base, String probleme) {

        public Optional<ConnexionBase> connexion() {
            return Optional.ofNullable(base);
        }

        /** True when changes survive a restart. */
        public boolean enregistre() {
            return mode == Mode.MYSQL;
        }

        /** Short French label for the shell sidebar. */
        public String libelle() {
            return switch (mode) {
                case MYSQL -> "Base MySQL connectée";
                case MEMOIRE -> "Mode démonstration · non enregistré";
                case MEMOIRE_SECOURS -> "MySQL indisponible · non enregistré";
            };
        }

        /** What is (not) saved, in French, for the sidebar tooltip. */
        public String detail() {
            return switch (mode) {
                case MYSQL -> "Enregistrés dans MySQL : " + String.join(", ", MODULES_EN_BASE) + ". Les autres "
                        + "données restent en mémoire jusqu'à la livraison de leur stockage MySQL.";
                case MEMOIRE -> "DB_URL n'est pas défini : toutes les données sont en mémoire et repartent des "
                        + "données de démonstration au prochain démarrage.";
                case MEMOIRE_SECOURS -> probleme + " L'application fonctionne en mode démonstration : les "
                        + "modifications ne seront pas enregistrées. Démarrez MySQL puis relancez l'application.";
            };
        }
    }

    /** What AppContext keeps in MySQL today; add a module here when it switches its DAOs to JDBC. */
    static final List<String> MODULES_EN_BASE = List.of("comptes utilisateurs",
            "journal d'audit");

    private static Etat etat;

    private Persistance() {
    }

    /** The mode of this run, decided on the first call from DB_URL / DB_USER / DB_PASSWORD. */
    public static synchronized Etat etat() {
        if (etat == null) {
            etat = choisir(ConnexionBase.depuisEnvironnement());
        }
        return etat;
    }

    static Etat choisir(Optional<ConnexionBase> configuration) {
        if (configuration.isEmpty()) {
            JOURNAL.log(System.Logger.Level.INFO, "DB_URL non défini : données en mémoire.");
            return new Etat(Mode.MEMOIRE, null, null);
        }
        ConnexionBase base = configuration.get();
        try {
            SchemaBase.appliquer(base);
            JOURNAL.log(System.Logger.Level.INFO, "Base MySQL prête : " + base);
            return new Etat(Mode.MYSQL, base, null);
        } catch (DonneesException e) {
            // The URL is masked by ConnexionBase.toString(); the full cause stays in the log only.
            JOURNAL.log(System.Logger.Level.WARNING, "MySQL injoignable (" + base + ") : mode mémoire de secours.", e);
            return new Etat(Mode.MEMOIRE_SECOURS, null, e.getMessage());
        }
    }
}
