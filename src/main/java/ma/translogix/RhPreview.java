package ma.translogix;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Dev launcher: opens the "Espace RH" screen directly.
 * Run: ./mvnw javafx:run -Djavafx.mainClass=ma.translogix.RhPreview
 */
public class RhPreview extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent racine = FXMLLoader.load(RhPreview.class.getResource("fxml/rh/rh-layout.fxml"));
        stage.setScene(new Scene(racine, 1280, 780));
        stage.setTitle("TransLogix - Espace RH (aper\u00e7u)");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
