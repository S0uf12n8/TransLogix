package ma.translogix.controller.comptable;

import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ma.translogix.AppContext;
import ma.translogix.model.Facture;
import ma.translogix.model.ModePaiement;
import ma.translogix.util.Formats;
import ma.translogix.util.ValidationException;

/** Fields of the "Enregistrer règlement" dialog. Rules stay in FactureService.enregistrerPaiement. */
public class PaiementFormulaireController implements ComptableUi.Formulaire {
    static final String FXML = "comptable/paiement-dialog.fxml";

    @FXML private Label titre;
    @FXML private Label description;
    @FXML private ComboBox<ModePaiement> champMode;
    @FXML private TextField champReference;
    @FXML private DatePicker champDate;

    private Facture facture;

    @FXML
    private void initialize() {
        champMode.setItems(FXCollections.observableArrayList(ModePaiement.values()));
        champMode.setValue(ModePaiement.VIREMENT);
        champDate.setValue(LocalDate.now());
        champDate.setConverter(new StringConverter<>() { // dd/MM/yyyy like every other date of the app
            @Override
            public String toString(LocalDate date) {
                return Formats.date(date);
            }

            @Override
            public LocalDate fromString(String texte) {
                try {
                    return texte == null || texte.isBlank() ? null : Formats.lireDate(texte);
                } catch (ValidationException e) {
                    return null;
                }
            }
        });
    }

    void preparer(Facture facture, String nomClient) {
        this.facture = facture;
        titre.setText("Enregistrer règlement " + facture.getNumero());
        description.setText("Saisissez l'encaissement reçu de " + nomClient + " : "
                + Formats.mad(facture.getMontantTtc()) + " TTC.");
    }

    @Override
    public void enregistrer() {
        // A typed date is only committed on focus loss: read the editor so "Confirmer" right after typing works.
        LocalDate date = champDate.getConverter().fromString(champDate.getEditor().getText());
        if (date == null && !champDate.getEditor().getText().isBlank()) {
            throw new ValidationException("Date d'encaissement invalide (format jj/mm/aaaa).");
        }
        AppContext.factureService().enregistrerPaiement(facture.getId(), champMode.getValue(),
                champReference.getText(), date);
    }

    @Override
    public String titre() {
        return titre.getText();
    }
}
