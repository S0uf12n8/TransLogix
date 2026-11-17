package ma.translogix;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class ComptabilitePreview extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        URL fxml = ComptabilitePreview.class.getResource(
                "/ma/translogix/fxml/comptabilite/Dashboard.fxml"
        );

        if (fxml == null) {
            throw new IllegalStateException("Dashboard.fxml not found!");
        }

        Parent root = FXMLLoader.load(fxml);

        URL css = ComptabilitePreview.class.getResource(
                "/ma/translogix/css/comptabilite.css"
        );

        if (css != null) {
            root.getStylesheets().add(css.toExternalForm());
        }

        Scene scene = new Scene(root, 1100, 700);

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
