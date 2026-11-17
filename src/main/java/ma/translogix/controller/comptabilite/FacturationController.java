
package ma.translogix.controller.comptabilite;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class FacturationController {

    @FXML private TextField numeroField;
    @FXML private TextField clientField;
    @FXML private TextField montantHTField;
    @FXML private TextField tauxTVAField;

    @FXML private Label montantTVAResult;
    @FXML private Label montantTTCResult;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        tauxTVAField.setText("20");
    }

    @FXML
    private void calculerFacture() {

        try {
            double ht = Double.parseDouble(montantHTField.getText());
            double taux = Double.parseDouble(tauxTVAField.getText());

            if (ht < 0 || taux < 0) {
                messageLabel.setText("Les montants doivent être positifs.");
                return;
            }

            double tva = ht * taux / 100;
            double ttc = ht + tva;

            montantTVAResult.setText(String.format("%.2f MAD", tva));
            montantTTCResult.setText(String.format("%.2f MAD", ttc));
            messageLabel.setText("Calcul effectué.");

        } catch (NumberFormatException e) {
            messageLabel.setText("Veuillez saisir des valeurs numériques valides.");
        }
    }
}
