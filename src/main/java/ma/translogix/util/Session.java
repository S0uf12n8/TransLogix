package ma.translogix.util;

import ma.translogix.model.Utilisateur;

/** The user currently logged in. Set by login, read by the shell and by any screen that needs it. */
public final class Session {
    private static Utilisateur courant;

    private Session() {
    }

    public static void ouvrir(Utilisateur utilisateur) {
        courant = utilisateur;
    }

    public static Utilisateur utilisateur() {
        if (courant == null) {
            throw new IllegalStateException("Aucun utilisateur connecté.");
        }
        return courant;
    }

    public static boolean estConnecte() {
        return courant != null;
    }

    public static void fermer() {
        courant = null;
    }
}
