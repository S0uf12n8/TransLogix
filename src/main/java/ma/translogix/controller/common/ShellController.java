package ma.translogix.controller.common;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.Session;

/**
 * The shared banner: sidebar + top bar. Every role uses it.
 * Each page is loaded into the centre area; pages never contain the sidebar themselves.
 */
public class ShellController {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    @FXML private Label sousTitre;
    @FXML private Label initiales;
    @FXML private Label nomUtilisateur;
    @FXML private Label roleUtilisateur;
    @FXML private Label kicker;
    @FXML private Label titrePage;
    @FXML private Label dateDuJour;
    @FXML private VBox menu;
    @FXML private StackPane contenu;

    private Button boutonActif;

    @FXML
    private void initialize() {
        Utilisateur utilisateur = Session.utilisateur();
        Role role = utilisateur.getRole();

        sousTitre.setText(NavigationMenu.sousTitre(role));
        initiales.setText(initialesDe(utilisateur));
        nomUtilisateur.setText(utilisateur.getNomComplet());
        roleUtilisateur.setText(role.getLibelle());
        kicker.setText(role.getLibelle().toUpperCase(Locale.FRENCH));
        dateDuJour.setText(LocalDate.now().format(DATE));

        Button premier = null;
        for (NavigationMenu.Entree entree : NavigationMenu.pour(role)) {
            Button bouton = new Button(entree.libelle());
            bouton.getStyleClass().add("nav-item");
            bouton.setMaxWidth(Double.MAX_VALUE);
            URL vue = getClass().getResource(NavigationMenu.RACINE + entree.fxml());
            if (vue == null) {
                bouton.setDisable(true);
                bouton.setTooltip(new Tooltip("Bientôt disponible"));
            } else {
                bouton.setOnAction(e -> ouvrir(bouton, entree.libelle(), vue));
                if (premier == null) {
                    premier = bouton;
                }
            }
            menu.getChildren().add(bouton);
        }
        if (premier != null) {
            premier.fire();
        }
    }

    private void ouvrir(Button bouton, String titre, URL vue) {
        try {
            Parent page = FXMLLoader.load(vue);
            contenu.getChildren().setAll(page);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            Alert alerte = new Alert(Alert.AlertType.ERROR,
                    "Impossible d'ouvrir " + vue.getPath() + "\n" + e.getMessage());
            alerte.setHeaderText(null);
            alerte.showAndWait();
            return;
        }
        if (boutonActif != null) {
            boutonActif.getStyleClass().remove("active");
        }
        bouton.getStyleClass().add("active");
        boutonActif = bouton;
        titrePage.setText(titre);
    }

    @FXML
    private void deconnexion() {
        Session.fermer();
        try {
            Parent login = FXMLLoader.load(getClass().getResource(NavigationMenu.RACINE + "auth/login.fxml"));
            contenu.getScene().setRoot(login);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger l'écran de connexion", e);
        }
    }

    private static String initialesDe(Utilisateur utilisateur) {
        String p = utilisateur.getPrenom();
        String n = utilisateur.getNom();
        String i = (p == null || p.isBlank() ? "" : p.substring(0, 1))
                + (n == null || n.isBlank() ? "" : n.substring(0, 1));
        return i.toUpperCase(Locale.FRENCH);
    }
}
