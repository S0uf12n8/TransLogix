package ma.translogix.dao;

/**
 * A database problem, already turned into a French message the user can read (no SQL, no table names).
 * Thrown by the JDBC DAOs (see dao.jdbc.SqlErreurs); the original SQLException stays attached for the log.
 * Like ValidationException, it is unchecked: services let it through and the UI shows getMessage().
 */
public class DonneesException extends RuntimeException {

    /** What went wrong, for code that wants to react differently (e.g. offer "Réessayer" on CONNEXION). */
    public enum Type {
        /** Server unreachable, connection lost, wrong credentials or unknown database. */
        CONNEXION,
        /** A value that must be unique already exists. */
        DOUBLON,
        /** Delete refused: other data still points to this row. */
        REFERENCE_UTILISEE,
        /** Insert or update refused: the linked row does not exist (any more). */
        REFERENCE_INCONNUE,
        /** A required column was left empty. */
        CHAMP_OBLIGATOIRE,
        /** Value too long, out of range or of the wrong kind. */
        VALEUR_INVALIDE,
        /** Lock timeout or deadlock: someone else is changing the same data. */
        CONFLIT,
        /** Anything else. */
        AUTRE
    }

    private final Type type;

    public DonneesException(Type type, String message, Throwable origine) {
        super(message, origine);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
