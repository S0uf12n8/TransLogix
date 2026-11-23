package ma.translogix.chauffeur;

import java.io.IOException;
import java.io.InputStream;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/** Point d'entrée : Espace Chauffeur – TransLogix. */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        loadFont("/fonts/fa-solid-900.ttf");
        loadFont("/fonts/fa-regular-400.ttf");

        Parent root = FXMLLoader.load(Main.class.getResource("/fxml/Main.fxml"));
        Scene scene = new Scene(root, 1440, 900);
        stage.setTitle("Espace Chauffeur | Translogix");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    private static void loadFont(String path) {
        try (InputStream in = Main.class.getResourceAsStream(path)) {
            if (in != null) Font.loadFont(in, 16);
        } catch (IOException ignored) {
            // les icônes n'apparaîtront pas, le reste de l'interface fonctionne
        }
    }

    public static void main(String[] args) { launch(args); }
}
