package ma.translogix;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Dev launcher: opens the admin screen directly, until login routes users by role.
 * Run: ./mvnw javafx:run -Djavafx.mainClass=ma.translogix.AdminPreview
 */
public class AdminPreview extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent racine = FXMLLoader.load(AdminPreview.class.getResource("fxml/admin/admin-layout.fxml"));
        stage.setScene(new Scene(racine, 1100, 700));
        stage.setTitle("TransLogix - Gérant (aperçu)");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
