
package ma.translogix;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.Session;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // Temporary accountant session for development
        Utilisateur comptable = AppContext.utilisateurService()
                .lister()
                .stream()
                .filter(u -> u.getRole() == Role.COMPTABLE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun utilisateur COMPTABLE trouvé."
                ));

        Session.ouvrir(comptable);

        FXMLLoader loader = new FXMLLoader(
                Main.class.getResource("fxml/common/shell.fxml")
        );

        Parent root = loader.load();

        Scene scene = new Scene(root, 1400, 850);

        stage.setTitle("TransLogix - Comptabilité");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
