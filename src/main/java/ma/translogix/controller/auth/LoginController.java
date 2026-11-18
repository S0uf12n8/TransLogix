
package ma.translogix.controller.auth;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ma.translogix.AppContext;
import ma.translogix.model.Utilisateur;
import ma.translogix.service.UtilisateurService;
import ma.translogix.util.PasswordHasher;
import ma.translogix.util.Session;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void connecter() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        try {
            UtilisateurService service = AppContext.utilisateurService();

            Utilisateur utilisateur = service.lister().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email))
                    .findFirst()
                    .orElse(null);

            if (utilisateur == null
                    || !utilisateur.isActif()
                    || !PasswordHasher.verifier(
                            password, utilisateur.getMotDePasseHash())) {
                errorLabel.setText("Email ou mot de passe incorrect.");
                return;
            }

            Session.ouvrir(utilisateur);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/translogix/fxml/common/shell.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setMaximized(true);

        } catch (Exception e) {
            errorLabel.setText("Erreur de connexion : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
