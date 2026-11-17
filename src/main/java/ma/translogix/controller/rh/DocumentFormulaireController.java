package ma.translogix.controller.rh;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ma.translogix.AppContext;
import ma.translogix.model.Employe;
import ma.translogix.model.TypeDocument;
import ma.translogix.service.ConformiteService;
import ma.translogix.service.EmployeService;

/** Fields of the renewal dialog. Rules stay in ConformiteService. */
public class DocumentFormulaireController implements RhUi.Formulaire {
    static final String FXML = "rh/document-dialog.fxml";

    @FXML private Label titre;
    @FXML private ComboBox<Employe> champEmploye;
    @FXML private ComboBox<TypeDocument> champType;
    @FXML private TextField champNumero;
    @FXML private TextField champCategorie;
    @FXML private DatePicker champExpiration;

    private final EmployeService employes = AppContext.employeService();
    private final ConformiteService service = AppContext.conformiteService();

    @FXML
    private void initialize() {
        champEmploye.getItems().setAll(employes.listerChauffeurs());
        champEmploye.setConverter(new StringConverter<>() {
            @Override
            public String toString(Employe e) {
                return e == null ? "" : e.getNomComplet() + " (" + e.getMatricule() + ")";
            }

            @Override
            public Employe fromString(String texte) {
                return null; // not editable
            }
        });
        champType.getItems().setAll(TypeDocument.values());
        RhUi.formatDate(champExpiration);
        champEmploye.valueProperty().addListener((obs, ancien, nouveau) -> remplirDepuisExistant());
        champType.valueProperty().addListener((obs, ancien, nouveau) -> remplirDepuisExistant());
    }

    /** Pre-selects the driver and the document (both may be null: "Mettre à jour document"). */
    void preparer(Employe employe, TypeDocument type) {
        if (employe != null) {
            champEmploye.getItems().stream()
                    .filter(e -> e.getId().equals(employe.getId()))
                    .findFirst()
                    .ifPresent(champEmploye::setValue);
        }
        champType.setValue(type);
    }

    /** Shows the current number / category / expiry of the chosen document, so only the date needs changing. */
    private void remplirDepuisExistant() {
        TypeDocument type = champType.getValue();
        boolean permis = type == TypeDocument.PERMIS;
        champCategorie.setDisable(!permis);
        if (!permis) {
            champCategorie.clear();
        }
        Employe employe = champEmploye.getValue();
        if (employe == null || type == null) {
            return;
        }
        service.document(employe.getId(), type).ifPresentOrElse(d -> {
            champNumero.setText(d.getNumero());
            champCategorie.setText(permis ? d.getCategorie() : null);
            champExpiration.setValue(d.getDateExpiration());
        }, () -> {
            champNumero.clear();
            champCategorie.clear();
            champExpiration.setValue(null);
        });
    }

    @Override
    public void enregistrer() {
        Employe employe = champEmploye.getValue();
        service.renouveler(employe == null ? null : employe.getId(), champType.getValue(), champNumero.getText(),
                champCategorie.getText(), RhUi.lireDate(champExpiration));
    }

    @Override
    public String titre() {
        return titre.getText();
    }
}
