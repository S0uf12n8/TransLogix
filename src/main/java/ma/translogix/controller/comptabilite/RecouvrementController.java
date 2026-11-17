
package ma.translogix.controller.comptabilite;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class RecouvrementController {

    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        messageLabel.setText(
            "Suivi des factures clients non réglées ou partiellement réglées."
        );
    }

    @FXML
    private void actualiser() {
        messageLabel.setText(
            "Les données de recouvrement seront chargées depuis le backend."
        );
    }
}
