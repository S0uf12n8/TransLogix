package ma.translogix.controller.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.translogix.AppContext;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.service.UtilisateurService;
import ma.translogix.util.ValidationException;

/** User management screen: table on the left, form on the right. No business rules here. */
public class UtilisateursController {

    @FXML private TableView<Utilisateur> table;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colPrenom;
    @FXML private TableColumn<Utilisateur, String> colEmail;
    @FXML private TableColumn<Utilisateur, String> colRole;
    @FXML private TableColumn<Utilisateur, String> colActif;

    @FXML private TextField champNom;
    @FXML private TextField champPrenom;
    @FXML private TextField champEmail;
    @FXML private PasswordField champMotDePasse;
    @FXML private ComboBox<Role> champRole;
    @FXML private CheckBox champActif;

    private final UtilisateurService service = AppContext.utilisateurService();
    private Utilisateur selection;

    @FXML
    private void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrenom()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole().getLibelle()));
        colActif.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActif() ? "Oui" : "Non"));

        champRole.setItems(FXCollections.observableArrayList(Role.values()));
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, ancien, choisi) -> remplirFormulaire(choisi));
        rafraichir();
    }

    @FXML
    private void nouveau() {
        table.getSelectionModel().clearSelection();
        viderFormulaire();
    }

    @FXML
    private void enregistrer() {
        try {
            if (selection == null) {
                service.creer(champNom.getText(), champPrenom.getText(), champEmail.getText(),
                        champMotDePasse.getText(), champRole.getValue());
            } else {
                service.modifier(selection.getId(), champNom.getText(), champPrenom.getText(),
                        champEmail.getText(), champRole.getValue(), champActif.isSelected());
            }
            rafraichir();
            viderFormulaire();
        } catch (ValidationException e) {
            erreur(e.getMessage());
        }
    }

    @FXML
    private void supprimer() {
        if (selection == null) {
            erreur("Sélectionnez un utilisateur dans le tableau.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer " + selection.getNomComplet() + " ?", ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText(null);
        if (confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                service.supprimer(selection.getId());
                rafraichir();
                viderFormulaire();
            } catch (ValidationException e) {
                erreur(e.getMessage());
            }
        }
    }

    private void rafraichir() {
        table.setItems(FXCollections.observableArrayList(service.lister()));
    }

    private void remplirFormulaire(Utilisateur utilisateur) {
        if (utilisateur == null) {
            viderFormulaire();
            return;
        }
        selection = utilisateur;
        champNom.setText(utilisateur.getNom());
        champPrenom.setText(utilisateur.getPrenom());
        champEmail.setText(utilisateur.getEmail());
        champRole.setValue(utilisateur.getRole());
        champActif.setSelected(utilisateur.isActif());
        champMotDePasse.clear();
        champMotDePasse.setDisable(true);
    }

    private void viderFormulaire() {
        selection = null;
        champNom.clear();
        champPrenom.clear();
        champEmail.clear();
        champMotDePasse.clear();
        champMotDePasse.setDisable(false);
        champRole.setValue(null);
        champActif.setSelected(true);
    }

    private void erreur(String message) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alerte.setTitle("Erreur");
        alerte.setHeaderText(null);
        alerte.showAndWait();
    }
}
