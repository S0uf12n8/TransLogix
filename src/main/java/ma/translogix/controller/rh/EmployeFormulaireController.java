package ma.translogix.controller.rh;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ma.translogix.AppContext;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;
import ma.translogix.model.TypeContrat;
import ma.translogix.service.EmployeService;

/** Fields of the employee dialog. Rules stay in EmployeService. */
public class EmployeFormulaireController implements RhUi.Formulaire {
    static final String FXML = "rh/employe-dialog.fxml";

    @FXML private Label titre;
    @FXML private Label description;
    @FXML private Label note;
    @FXML private TextField champPrenom;
    @FXML private TextField champNom;
    @FXML private TextField champCin;
    @FXML private TextField champCnss;
    @FXML private ComboBox<Poste> champPoste;
    @FXML private ComboBox<TypeContrat> champContrat;
    @FXML private DatePicker champEmbauche;

    private final EmployeService service = AppContext.employeService();
    /** null when creating a new employee. */
    private Employe edite;

    @FXML
    private void initialize() {
        champPoste.getItems().setAll(Poste.values());
        champContrat.getItems().setAll(TypeContrat.values());
        RhUi.formatDate(champEmbauche);
    }

    /** Fills the form for an edit, or prepares a creation (employe == null). */
    void preparer(Employe employe) {
        edite = employe;
        if (employe == null) {
            return;
        }
        titre.setText("Modifier " + employe.getNomComplet());
        description.setText("Matricule " + employe.getMatricule() + " : informations administratives du salarié.");
        note.setText("Le matricule ne change pas.");
        champPrenom.setText(employe.getPrenom());
        champNom.setText(employe.getNom());
        champCin.setText(employe.getCin());
        champCnss.setText(employe.getCnss());
        champPoste.setValue(employe.getPoste());
        champContrat.setValue(employe.getTypeContrat());
        champEmbauche.setValue(employe.getDateEmbauche());
    }

    @Override
    public void enregistrer() {
        var embauche = RhUi.lireDate(champEmbauche);
        if (edite == null) {
            service.creer(champNom.getText(), champPrenom.getText(), champCin.getText(), champCnss.getText(),
                    champPoste.getValue(), champContrat.getValue(), embauche);
        } else {
            service.modifier(edite.getId(), champNom.getText(), champPrenom.getText(), champCin.getText(),
                    champCnss.getText(), champPoste.getValue(), champContrat.getValue(), embauche);
        }
    }

    @Override
    public String titre() {
        return titre.getText();
    }
}
