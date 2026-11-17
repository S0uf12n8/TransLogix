
package ma.translogix.controller.comptabilite;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PaieController {

    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        messageLabel.setText(
            "Gestion des salaires, primes, retenues et paiements des employés."
        );
    }

    @FXML
    private void actualiser() {
        messageLabel.setText(
            "Les informations de paie seront chargées depuis le backend."
        );
    }
}
