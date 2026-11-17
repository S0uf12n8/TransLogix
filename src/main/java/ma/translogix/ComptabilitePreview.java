package ma.translogix;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

import ma.translogix.controller.comptabilite.ComptabiliteController;

public class ComptabilitePreview extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // Sidebar + top bar + page area; starts on the dashboard.
        Parent root = new ComptabiliteController().createMainLayout();

        URL css = ComptabilitePreview.class.getResource(
                "/ma/translogix/css/comptabilite.css"
        );

        if (css == null) {
            throw new IllegalStateException("comptabilite.css not found!");
        }

        Scene scene = new Scene(root, 1100, 700);
        // On the scene, so every page loaded into the center is styled too.
        scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("TransLogix - Comptabilite");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
