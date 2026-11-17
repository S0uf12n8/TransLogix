
package ma.translogix.controller.comptabilite;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TVAController {

    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        messageLabel.setText(
            "Suivi de la TVA collectée, déductible et du solde fiscal."
        );
    }

    @FXML
    private void actualiser() {
        messageLabel.setText(
            "Les données fiscales seront chargées depuis le backend."
        );
    }
}
