
package ma.translogix;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ma.translogix.view.rh.PortailRhView;

public class RHMain extends Application {

    @Override
    public void start(Stage stage) {

        PortailRhView rhView = new PortailRhView();

        Scene scene = new Scene(rhView, 1400, 900);

        stage.setTitle("TransLogix - Espace RH");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

