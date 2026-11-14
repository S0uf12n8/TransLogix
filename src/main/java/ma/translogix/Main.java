package ma.translogix;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("fxml/auth/login.fxml"));
        stage.setScene(new Scene(loader.load(), 900, 600));
        stage.setTitle("TransLogix");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
