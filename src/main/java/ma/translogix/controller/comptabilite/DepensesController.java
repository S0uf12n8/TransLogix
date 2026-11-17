
package ma.translogix.controller.comptabilite;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DepensesController {

    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        messageLabel.setText(
            "Gestion des dépenses : carburant, péages, frais de route et maintenance."
        );
    }

    @FXML
    private void actualiser() {
        messageLabel.setText(
            "Les dépenses seront chargées depuis le backend."
        );
    }
}
