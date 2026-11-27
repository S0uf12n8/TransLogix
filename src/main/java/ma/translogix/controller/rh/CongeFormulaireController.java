package ma.translogix.controller.rh;

import java.util.Comparator;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ma.translogix.AppContext;
import ma.translogix.model.Employe;
import ma.translogix.model.TypeConge;
import ma.translogix.util.ValidationException;

/** Fields of the leave-request dialog. Rules stay in CongeService. */
public class CongeFormulaireController implements RhUi.Formulaire {
    static final String FXML = "rh/conge-dialog.fxml";

    @FXML private Label titre;
    @FXML private ComboBox<Employe> champEmploye;
    @FXML private ComboBox<TypeConge> champType;
    @FXML private DatePicker champDebut;
    @FXML private DatePicker champFin;
    @FXML private TextField champMotif;

    @FXML
    private void initialize() {
        champEmploye.getItems().setAll(AppContext.employeService().lister().stream()
                .filter(Employe::isActif)
                .sorted(Comparator.comparing(Employe::getNom).thenComparing(Employe::getPrenom))
                .toList());
        champEmploye.setConverter(new StringConverter<>() {
            @Override
            public String toString(Employe employe) {
                return employe == null ? "" : employe.getNomComplet() + " (" + employe.getMatricule() + ")";
            }

            @Override
            public Employe fromString(String texte) {
                return null;
            }
        });
        champType.getItems().setAll(TypeConge.values());
        RhUi.formatDate(champDebut);
        RhUi.formatDate(champFin);
    }

    @Override
    public void enregistrer() {
        Employe employe = champEmploye.getValue();
        if (employe == null) {
            throw new ValidationException("Choisissez un salarié.");
        }
        AppContext.congeService().creer(employe.getId(), champType.getValue(),
                RhUi.lireDate(champDebut), RhUi.lireDate(champFin), champMotif.getText());
    }

    @Override
    public String titre() {
        return titre.getText();
    }
}
