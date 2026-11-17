
package ma.translogix.controller.comptabilite;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    private Label caLabel;

    @FXML
    private Label encaissementsLabel;

    @FXML
    private Label creancesLabel;

    @FXML
    private Label chargesLabel;

    @FXML
    private Label tresorerieLabel;

    @FXML
    private Label resultatLabel;

    @FXML
    public void initialize() {

        // Temporary demonstration values.
        // These will later come from the Spring Boot API.

        caLabel.setText("245 800 MAD");
        encaissementsLabel.setText("186 400 MAD");
        creancesLabel.setText("59 400 MAD");
        chargesLabel.setText("92 700 MAD");
        tresorerieLabel.setText("125 000 MAD");
        resultatLabel.setText("153 100 MAD");
    }
}
