package ma.translogix;

import java.util.List;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.Session;

/**
 * Dev launcher, not part of the real app.
 *   Full banner for a role : ./mvnw javafx:run -Djavafx.mainClass=ma.translogix.Preview -Djavafx.args=shell=DISPATCHER
 *   One page alone         : ./mvnw javafx:run -Djavafx.mainClass=ma.translogix.Preview -Djavafx.args=dispatcher/flotte.fxml
 * Roles: GERANT, DISPATCHER, RH, COMPTABLE, CHAUFFEUR
 */
public class Preview extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        List<String> args = getParameters().getRaw();
        String cible = args.isEmpty() ? "shell=GERANT" : args.get(0);

        String vue = cible;
        if (cible.startsWith("shell=")) {
            Role role = Role.valueOf(cible.substring("shell=".length()));
            Session.ouvrir(new Utilisateur(0L, "Aperçu", "Mode", "apercu@translogix.ma", null, role, true));
            vue = "common/shell.fxml";
        }

        var url = Preview.class.getResource("fxml/" + vue);
        if (url == null) {
            throw new IllegalArgumentException("FXML introuvable : fxml/" + vue);
        }
        Parent racine = FXMLLoader.load(url);
        Scene scene = new Scene(racine, 1280, 760);
        scene.getStylesheets().add(Preview.class.getResource("css/app.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("TransLogix - aperçu : " + cible);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
