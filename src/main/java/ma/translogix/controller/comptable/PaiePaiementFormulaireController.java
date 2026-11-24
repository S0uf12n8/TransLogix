package ma.translogix.controller.comptable;

import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ma.translogix.model.ModePaiementSalaire;
import ma.translogix.model.Paie;

/** Fields of the "Enregistrer le paiement du salaire" dialog. Rules stay in PaieService. */
public class PaiePaiementFormulaireController implements ComptableUi.Formulaire {
    static final String FXML = "comptable/paie-paiement-dialog.fxml";

    @FXML private Label titre;
    @FXML private Label resumeFiche;
    @FXML private ComboBox<ModePaiementSalaire> champMode;
    @FXML private TextField champReference;
    @FXML private DatePicker champDate;

    private Paie paie;

    @FXML
    private void initialize() {
        champMode.setItems(FXCollections.observableArrayList(ModePaiementSalaire.values()));
        champDate.setValue(LocalDate.now());
    }

    void preparer(Paie fiche) {
        this.paie = fiche;
        if (fiche == null) {
            resumeFiche.setText("Aucune fiche sélectionnée.");
            return;
        }
        resumeFiche.setText(fiche.getNumero() + " · mois " + fiche.getPeriode() + " · net "
                + ma.translogix.util.Formats.mad(fiche.getNet()));
    }

    @Override
    public void enregistrer() {
        if (paie == null) {
            throw new ma.translogix.util.ValidationException("Choisissez une fiche de paie.");
        }
        PaieController.service().enregistrerPaiement(paie.getId(), champMode.getValue(),
                champReference.getText(), champDate.getValue());
    }

    @Override
    public String titre() {
        return titre.getText();
    }
}
