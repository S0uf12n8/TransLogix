package ma.translogix.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import ma.translogix.model.Utilisateur;
import ma.translogix.service.UtilisateurService;

/** Fields of the password reset dialog. The rules (length, confirmation) stay in UtilisateurService. */
public class MotDePasseFormulaireController {

    @FXML private Label titre;
    @FXML private Label description;
    @FXML private PasswordField champNouveau;
    @FXML private PasswordField champConfirmation;

    private Utilisateur cible;

    public void preparer(Utilisateur utilisateur) {
        cible = utilisateur;
        description.setText("Nouveau mot de passe pour " + utilisateur.getNomComplet()
                + " (" + utilisateur.getEmail() + ").");
    }

    /** Throws ValidationException when the password is too short or the two fields differ. */
    public void enregistrer(UtilisateurService service) {
        service.reinitialiserMotDePasse(cible.getId(), champNouveau.getText(), champConfirmation.getText());
    }

    public String titre() {
        return titre.getText();
    }
}
