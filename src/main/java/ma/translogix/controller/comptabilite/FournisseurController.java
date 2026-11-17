
package ma.translogix.controller.comptabilite;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class FournisseurController {

    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        messageLabel.setText(
            "Gestion des fournisseurs et suivi des dettes."
        );
    }

    @FXML
    private void actualiser() {
        messageLabel.setText(
            "Les dettes fournisseurs seront chargées depuis le backend."
        );
    }
}
