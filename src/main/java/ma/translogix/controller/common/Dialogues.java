package ma.translogix.controller.common;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Standard dialogs shared by every controller (they replace the mockup's toasts). */
public final class Dialogues {

    private Dialogues() {
    }

    public static void info(String titre, String message) {
        afficher(Alert.AlertType.INFORMATION, titre, message);
    }

    public static void erreur(String message) {
        afficher(Alert.AlertType.ERROR, "Erreur", message);
    }

    /** @return true when the user clicked "Oui" */
    public static boolean confirmer(String titre, String message) {
        Alert alerte = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        return alerte.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private static void afficher(Alert.AlertType type, String titre, String message) {
        Alert alerte = new Alert(type, message, ButtonType.OK);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.showAndWait();
    }
}
