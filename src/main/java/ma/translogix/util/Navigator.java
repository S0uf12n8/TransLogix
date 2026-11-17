package ma.translogix.util;

/**
 * Single entry point for moving between pages. The shell registers itself as the host at startup;
 * pages call Navigator.ouvrir("rh/conformite.fxml") and never use FXMLLoader to navigate.
 * Pure Java on purpose (no JavaFX import) so the routing logic is unit-tested without a display.
 */
public final class Navigator {

    /** Classpath folder holding every FXML page. */
    public static final String RACINE = "/ma/translogix/fxml/";

    /** What the shell must provide. */
    public interface Hote {
        /** Load the page in the centre area and highlight its sidebar entry. */
        void afficher(String fxml);

        /** The page file does not exist yet: tell the user. */
        void signalerNonLivre(String fxml);
    }

    private static Hote hote;

    private Navigator() {
    }

    public static synchronized void enregistrer(Hote nouveau) {
        hote = nouveau;
    }

    /** Unregisters the host, only if it is still the current one (e.g. on logout). */
    public static synchronized void liberer(Hote ancien) {
        if (hote == ancien) {
            hote = null;
        }
    }

    /**
     * Opens a page given relative to RACINE, e.g. "admin/audit.fxml".
     * @return true if the page was shown, false if it is not delivered yet
     */
    public static boolean ouvrir(String fxml) {
        if (fxml == null || fxml.isBlank() || fxml.contains("..") || fxml.startsWith("/")) {
            throw new IllegalArgumentException("Chemin de page invalide : " + fxml);
        }
        Hote courant;
        synchronized (Navigator.class) {
            courant = hote;
        }
        if (courant == null) {
            throw new IllegalStateException("Aucun écran principal n'est enregistré dans le Navigator.");
        }
        String chemin = fxml.trim();
        if (existe(chemin)) {
            courant.afficher(chemin);
            return true;
        }
        courant.signalerNonLivre(chemin);
        return false;
    }

    public static boolean existe(String fxml) {
        return Navigator.class.getResource(RACINE + fxml) != null;
    }
}
