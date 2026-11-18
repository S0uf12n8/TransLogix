package ma.translogix.controller.auth;

import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ma.translogix.AppContext;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.Navigator;
import ma.translogix.util.Session;
import ma.translogix.util.ValidationException;

/** Login screen ([O2], first version by Hiba). The check is in AuthService; this class only shows the result. */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        Platform.runLater(emailField::requestFocus);
    }

    @FXML
    private void connecter() {
        Utilisateur utilisateur;
        try {
            utilisateur = AppContext.authService().connecter(emailField.getText(), passwordField.getText());
        } catch (ValidationException e) {
            afficherErreur(e.getMessage());
            passwordField.clear();
            passwordField.requestFocus();
            return;
        }
        Session.ouvrir(utilisateur);
        // Switching the whole window (login -> shell) is the only FXMLLoader use outside the shell,
        // as ShellController.deconnexion does in the other direction.
        try {
            Parent shell = FXMLLoader.load(getClass().getResource(Navigator.RACINE + "common/shell.fxml"));
            Stage fenetre = (Stage) emailField.getScene().getWindow();
            fenetre.getScene().setRoot(shell);
            fenetre.setMaximized(true);
        } catch (IOException e) {
            Session.fermer();
            afficherErreur("Impossible d'ouvrir l'application : " + e.getMessage());
        }
    }

    private void afficherErreur(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
