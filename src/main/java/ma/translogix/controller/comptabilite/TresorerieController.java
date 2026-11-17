
package ma.translogix.controller.comptabilite;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TresorerieController {

    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        messageLabel.setText(
            "Suivi des entrées, sorties et soldes de trésorerie."
        );
    }

    @FXML
    private void actualiser() {
        messageLabel.setText(
            "Les mouvements de trésorerie seront chargés depuis le backend."
        );
    }
}
