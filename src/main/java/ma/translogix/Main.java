package ma.translogix;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import ma.translogix.util.MessagesErreur;

public class Main extends Application {

    private static final System.Logger JOURNAL = System.getLogger(Main.class.getName());

    /** True while an error dialog is open: a second error during it is only logged (no dialog cascade). */
    private static boolean dialogueOuvert;

    @Override
    public void start(Stage stage) throws Exception {
        installerFiletDeSecurite();
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("fxml/auth/login.fxml"));
        stage.setScene(new Scene(loader.load(), 900, 600));
        stage.setTitle("TransLogix");
        stage.show();
    }

    /**
     * Safety net for errors no screen caught (Iteration 3, DB error handling): the full stack trace goes to
     * the application log, the user only sees a short French message (MessagesErreur). A database error
     * (DonneesException) or a broken rule (ValidationException) shows its own message.
     */
    private static void installerFiletDeSecurite() {
        Thread.UncaughtExceptionHandler filet = (thread, erreur) -> {
            JOURNAL.log(System.Logger.Level.ERROR, "Erreur non gérée dans " + thread.getName(), erreur);
            // Always later: showAndWait is forbidden while JavaFX is laying out or animating.
            Platform.runLater(() -> afficher(erreur));
        };
        Thread.setDefaultUncaughtExceptionHandler(filet);
        Thread.currentThread().setUncaughtExceptionHandler(filet); // the JavaFX thread
    }

    private static void afficher(Throwable erreur) {
        if (dialogueOuvert) {
            return;
        }
        dialogueOuvert = true;
        try {
            Alert alerte = new Alert(Alert.AlertType.ERROR, MessagesErreur.pourUtilisateur(erreur), ButtonType.OK);
            alerte.setTitle(MessagesErreur.estConnexion(erreur) ? "Base de données indisponible" : "Erreur");
            alerte.setHeaderText(null);
            alerte.getDialogPane().setMinHeight(Region.USE_PREF_SIZE); // show the whole message, never "…"
            alerte.showAndWait();
        } finally {
            dialogueOuvert = false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
