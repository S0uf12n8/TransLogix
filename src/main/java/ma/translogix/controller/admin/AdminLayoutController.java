package ma.translogix.controller.admin;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

/** Banner + side menu. Swaps the centre area when a menu button is clicked. */
public class AdminLayoutController {

    @FXML
    private StackPane contenu;

    @FXML
    private void initialize() {
        afficherTableauDeBord();
    }

    @FXML
    private void afficherTableauDeBord() {
        charger("dashboard.fxml");
    }

    @FXML
    private void afficherUtilisateurs() {
        charger("utilisateurs.fxml");
    }

    @FXML
    private void afficherRh() {
        charger("rh/rh-layout.fxml");
    }

    private void charger(String fichier) {
        try {
            Parent vue = FXMLLoader.load(getClass().getResource("/ma/translogix/fxml/admin/" + fichier));
            contenu.getChildren().setAll(vue);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger " + fichier, e);
        }
    }
}
