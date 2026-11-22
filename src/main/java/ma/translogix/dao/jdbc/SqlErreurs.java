package ma.translogix.dao.jdbc;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ma.translogix.dao.DonneesException;
import ma.translogix.dao.DonneesException.Type;

/**
 * Turns a MySQL SQLException into a DonneesException with a clear French message.
 * Every JDBC DAO wraps its JDBC code with executer(...) so no SQLException, SQL text or table name
 * ever reaches the screen. Classification uses the MySQL error code first, then the SQLState class.
 *
 * <pre>
 * public Optional&lt;Client&gt; findById(Long id) {
 *     return SqlErreurs.executer(() -&gt; {
 *         try (Connection c = source.getConnection();
 *              PreparedStatement ps = c.prepareStatement("SELECT ... WHERE id = ?")) {
 *             ...
 *         }
 *     });
 * }
 * </pre>
 */
public final class SqlErreurs {

    /** JDBC code that may throw SQLException. */
    @FunctionalInterface
    public interface ActionSql<T> {
        T executer() throws SQLException;
    }

    // MySQL server error codes (https://dev.mysql.com/doc/mysql-errors/8.0/en/server-error-reference.html).
    static final int ACCES_REFUSE = 1045;
    static final int BASE_INCONNUE = 1049;
    static final int DOUBLON = 1062;
    static final int PARENT_REFERENCE = 1451;
    static final int ENFANT_SANS_PARENT = 1452;
    static final int COLONNE_NULL = 1048;
    static final int SANS_VALEUR_PAR_DEFAUT = 1364;
    static final int TROP_LONG = 1406;
    static final int HORS_PLAGE = 1264;
    static final int VALEUR_INCORRECTE = 1366;
    static final int VERROU_EXPIRE = 1205;
    static final int INTERBLOCAGE = 1213;

    static final String MESSAGE_CONNEXION = "Connexion à la base de données impossible. "
            + "Vérifiez que le serveur MySQL est démarré, puis réessayez. Rien n'a été enregistré.";
    static final String MESSAGE_ACCES = "La base de données refuse la connexion (identifiant, mot de passe "
            + "ou nom de base incorrect). Contactez l'administrateur. Rien n'a été enregistré.";
    static final String MESSAGE_REFERENCE_UTILISEE = "Suppression impossible : cet élément est encore utilisé "
            + "ailleurs (missions, factures, documents…). Désactivez-le plutôt, ou retirez d'abord ces liens.";
    static final String MESSAGE_REFERENCE_INCONNUE = "L'élément choisi n'existe plus (il a peut-être été supprimé "
            + "entre-temps). Actualisez la page et choisissez-en un autre.";
    static final String MESSAGE_CHAMP_OBLIGATOIRE = "Un champ obligatoire n'est pas rempli. Rien n'a été enregistré.";
    static final String MESSAGE_VALEUR_INVALIDE = "Une valeur saisie est trop longue ou invalide. "
            + "Corrigez-la puis réessayez.";
    static final String MESSAGE_CONFLIT = "Ces données sont en cours de modification par quelqu'un d'autre. "
            + "Patientez quelques secondes puis réessayez.";
    static final String MESSAGE_INTEGRITE = "Opération refusée : elle casserait la cohérence des données "
            + "(valeur en double ou élément lié manquant). Rien n'a été enregistré.";
    static final String MESSAGE_AUTRE = "Erreur de la base de données : l'opération n'a pas été enregistrée. "
            + "Réessayez ; si le problème continue, contactez l'administrateur.";

    /** MySQL text "Duplicate entry 'X' for key '...'": X is the user's own value, safe to show. */
    private static final Pattern VALEUR_EN_DOUBLE = Pattern.compile("Duplicate entry '(.*)' for key");

    private SqlErreurs() {
    }

    /** Runs JDBC code; any SQLException comes out as a DonneesException. */
    public static <T> T executer(ActionSql<T> action) {
        try {
            return action.executer();
        } catch (SQLException e) {
            throw traduire(e);
        }
    }

    public static DonneesException traduire(SQLException e) {
        int code = e.getErrorCode();
        String etat = e.getSQLState() == null ? "" : e.getSQLState();
        return switch (code) {
            case ACCES_REFUSE, BASE_INCONNUE -> new DonneesException(Type.CONNEXION, MESSAGE_ACCES, e);
            case DOUBLON -> new DonneesException(Type.DOUBLON, messageDoublon(e.getMessage()), e);
            case PARENT_REFERENCE -> new DonneesException(Type.REFERENCE_UTILISEE, MESSAGE_REFERENCE_UTILISEE, e);
            case ENFANT_SANS_PARENT -> new DonneesException(Type.REFERENCE_INCONNUE, MESSAGE_REFERENCE_INCONNUE, e);
            case COLONNE_NULL, SANS_VALEUR_PAR_DEFAUT ->
                    new DonneesException(Type.CHAMP_OBLIGATOIRE, MESSAGE_CHAMP_OBLIGATOIRE, e);
            case TROP_LONG, HORS_PLAGE, VALEUR_INCORRECTE ->
                    new DonneesException(Type.VALEUR_INVALIDE, MESSAGE_VALEUR_INVALIDE, e);
            case VERROU_EXPIRE, INTERBLOCAGE -> new DonneesException(Type.CONFLIT, MESSAGE_CONFLIT, e);
            default -> parEtat(e, etat);
        };
    }

    /** No known MySQL code (other driver, connection never opened): fall back on the SQLState class. */
    private static DonneesException parEtat(SQLException e, String etat) {
        if (etat.startsWith("08") || e instanceof SQLNonTransientConnectionException
                || e instanceof SQLTransientConnectionException) {
            return new DonneesException(Type.CONNEXION, MESSAGE_CONNEXION, e);
        }
        if (etat.startsWith("28")) {
            return new DonneesException(Type.CONNEXION, MESSAGE_ACCES, e);
        }
        if (etat.startsWith("40")) {
            return new DonneesException(Type.CONFLIT, MESSAGE_CONFLIT, e);
        }
        if (etat.startsWith("23")) { // integrity rule without a known code: say so, without guessing which one
            return new DonneesException(Type.AUTRE, MESSAGE_INTEGRITE, e);
        }
        return new DonneesException(Type.AUTRE, MESSAGE_AUTRE, e);
    }

    static String messageDoublon(String messageMySql) {
        Matcher m = VALEUR_EN_DOUBLE.matcher(messageMySql == null ? "" : messageMySql);
        String debut = m.find() && !m.group(1).isBlank()
                ? "La valeur « " + m.group(1) + " » existe déjà"
                : "Cette valeur existe déjà";
        return debut + " alors qu'elle doit être unique (email, ICE, matricule, numéro…). "
                + "Choisissez-en une autre.";
    }
}
