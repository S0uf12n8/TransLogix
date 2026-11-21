package ma.translogix.util;

import ma.translogix.dao.DonneesException;

/**
 * Chooses what the user reads when an error was not handled by the screen itself.
 * Our own exceptions already carry a French message meant for the user; anything else
 * (a bug, a JavaFX failure) gets a generic sentence, never a class name or a stack trace.
 */
public final class MessagesErreur {

    public static final String MESSAGE_INATTENDU = "Une erreur inattendue est survenue : l'opération n'a "
            + "peut-être pas abouti. Réessayez ; si le problème continue, signalez-le à l'administrateur "
            + "(le détail technique est enregistré dans le journal de l'application).";

    /** Guard against a broken cause chain that loops on itself. */
    private static final int PROFONDEUR_MAX = 20;

    private MessagesErreur() {
    }

    /**
     * The first DonneesException or ValidationException in the cause chain gives its message
     * (JavaFX often wraps them, e.g. in InvocationTargetException); otherwise MESSAGE_INATTENDU.
     */
    public static String pourUtilisateur(Throwable erreur) {
        Throwable t = erreur;
        for (int i = 0; t != null && i < PROFONDEUR_MAX; i++, t = t.getCause()) {
            if ((t instanceof DonneesException || t instanceof ValidationException)
                    && t.getMessage() != null && !t.getMessage().isBlank()) {
                return t.getMessage();
            }
        }
        return MESSAGE_INATTENDU;
    }

    /** True when the error is a database connection problem (the dialog title says so). */
    public static boolean estConnexion(Throwable erreur) {
        Throwable t = erreur;
        for (int i = 0; t != null && i < PROFONDEUR_MAX; i++, t = t.getCause()) {
            if (t instanceof DonneesException d) {
                return d.getType() == DonneesException.Type.CONNEXION;
            }
        }
        return false;
    }
}
