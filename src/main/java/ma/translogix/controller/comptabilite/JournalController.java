
package ma.translogix.controller.comptabilite;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class JournalController {

    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        messageLabel.setText(
            "Consultation des écritures comptables : débit et crédit."
        );
    }

    @FXML
    private void actualiser() {
        messageLabel.setText(
            "Les écritures comptables seront chargées depuis le backend."
        );
    }
}
