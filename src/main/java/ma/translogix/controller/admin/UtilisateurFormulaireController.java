package ma.translogix.controller.admin;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.service.UtilisateurService;

/** Fields of the user dialog. Sends the values to the service; the rules stay in UtilisateurService. */
public class UtilisateurFormulaireController {

    @FXML private Label titre;
    @FXML private Label description;
    @FXML private TextField champNom;
    @FXML private TextField champPrenom;
    @FXML private TextField champEmail;
    @FXML private VBox blocMotDePasse;
    @FXML private PasswordField champMotDePasse;
    @FXML private ComboBox<Role> champRole;
    @FXML private CheckBox champActif;

    /** null when creating a new account. */
    private Utilisateur edite;

    @FXML
    private void initialize() {
        champRole.setItems(FXCollections.observableArrayList(Role.values()));
    }

    /** Fills the form for an edit, or empties it for a creation (utilisateur == null). */
    public void preparer(Utilisateur utilisateur) {
        edite = utilisateur;
        boolean creation = utilisateur == null;
        titre.setText(creation ? "Nouvel utilisateur" : "Modifier " + utilisateur.getNomComplet());
        description.setText(creation
                ? "Complétez les informations du nouveau compte."
                : "Le mot de passe ne se change pas ici.");
        blocMotDePasse.setVisible(creation);
        blocMotDePasse.setManaged(creation);
        champActif.setVisible(!creation);
        champActif.setManaged(!creation);
        if (!creation) {
            champNom.setText(utilisateur.getNom());
            champPrenom.setText(utilisateur.getPrenom());
            champEmail.setText(utilisateur.getEmail());
            champRole.setValue(utilisateur.getRole());
            champActif.setSelected(utilisateur.isActif());
        }
    }

    /** Creates or updates the account. Throws ValidationException when a rule is broken. */
    public void enregistrer(UtilisateurService service) {
        if (edite == null) {
            service.creer(champNom.getText(), champPrenom.getText(), champEmail.getText(),
                    champMotDePasse.getText(), champRole.getValue());
        } else {
            service.modifier(edite.getId(), champNom.getText(), champPrenom.getText(),
                    champEmail.getText(), champRole.getValue(), champActif.isSelected());
        }
    }

    public String titre() {
        return titre.getText();
    }
}
