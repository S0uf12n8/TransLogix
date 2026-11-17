package com.translogix.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class TransLogixApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dispatcher/DispatcherMain.fxml"));

        // L-mساحة l-7a9i9iya dyal l-écran (bla taskbar), b3d l-scaling
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        double width  = Math.min(1400, screen.getWidth()  * 0.95);
        double height = Math.min(850,  screen.getHeight() * 0.95);

        Scene scene = new Scene(loader.load(), width, height);

        stage.setTitle("TransLogix — Dispatcher Desk");
        stage.setScene(scene);

        stage.setMinWidth(1000);
        stage.setMinHeight(600);

        // Centrer f l-écran
        stage.setX(screen.getMinX() + (screen.getWidth()  - width)  / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - height) / 2);

        // Optionnel: ila bghiti tbda maximisé, 7ell had l-ster
        // stage.setMaximized(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}