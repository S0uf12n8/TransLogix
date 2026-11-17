
package ma.translogix.controller.comptabilite;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ReglementController {

    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        messageLabel.setText(
            "Enregistrement et suivi des règlements fournisseurs."
        );
    }

    @FXML
    private void actualiser() {
        messageLabel.setText(
            "Les règlements seront chargés depuis le backend."
        );
    }
}
