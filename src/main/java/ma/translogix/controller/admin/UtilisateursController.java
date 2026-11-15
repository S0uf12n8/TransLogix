package ma.translogix.controller.admin;

import java.io.IOException;
import java.util.function.Function;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.service.UtilisateurService;
import ma.translogix.util.Navigator;
import ma.translogix.util.Recherche;
import ma.translogix.util.ValidationException;

/** User management screen: searchable table, create/edit in a dialog. No business rules here. */
public class UtilisateursController {

    private static final String FORMULAIRE = Navigator.RACINE + "admin/utilisateur-dialog.fxml";

    @FXML private TextField recherche;
    @FXML private TableView<Utilisateur> table;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colPrenom;
    @FXML private TableColumn<Utilisateur, String> colEmail;
    @FXML private TableColumn<Utilisateur, String> colRole;
    @FXML private TableColumn<Utilisateur, String> colActif;
    @FXML private TableColumn<Utilisateur, Utilisateur> colActions;

    private final UtilisateurService service = AppContext.utilisateurService();
    private final ObservableList<Utilisateur> donnees = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colNom.setCellFactory(col -> texte("cell-strong"));
        colPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrenom()));
        colPrenom.setCellFactory(col -> texte("cell-normal"));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colEmail.setCellFactory(col -> texte("cell-normal"));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole().getLibelle()));
        colRole.setCellFactory(col -> badge(libelle ->
                Role.GERANT.getLibelle().equals(libelle) ? "badge-orange" : "badge-blue"));
        colActif.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActif() ? "Actif" : "Inactif"));
        colActif.setCellFactory(col -> badge(statut -> "Actif".equals(statut) ? "badge-green" : "badge-gray"));
        colActions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button modifier = new Button("Modifier");
            private final Button supprimer = new Button("Supprimer");
            private final HBox boutons = new HBox(6, modifier, supprimer);

            {
                boutons.setAlignment(Pos.CENTER_LEFT);
                modifier.getStyleClass().add("link-orange");
                supprimer.getStyleClass().add("link-red");
                modifier.setOnAction(e -> ouvrirFormulaire(getItem()));
                supprimer.setOnAction(e -> supprimer(getItem()));
            }

            @Override
            protected void updateItem(Utilisateur u, boolean empty) {
                super.updateItem(u, empty);
                setText(null);
                setGraphic(empty || u == null ? null : boutons);
            }
        });

        FilteredList<Utilisateur> filtres = new FilteredList<>(donnees);
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(u -> Recherche.correspond(requete, u.getNom(), u.getPrenom(),
                        u.getEmail(), u.getRole().getLibelle(), u.isActif() ? "Actif" : "Inactif")));
        SortedList<Utilisateur> tries = new SortedList<>(filtres);
        tries.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(tries);
        rafraichir();
    }

    @FXML
    private void nouveau() {
        ouvrirFormulaire(null);
    }

    /** Opens the dialog; on a ValidationException the error is shown and the dialog stays open. */
    private void ouvrirFormulaire(Utilisateur utilisateur) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(FORMULAIRE));
        DialogPane panneau;
        try {
            panneau = loader.load();
        } catch (IOException e) {
            Dialogues.erreur("Impossible d'ouvrir le formulaire : " + e.getMessage());
            return;
        }
        UtilisateurFormulaireController formulaire = loader.getController();
        formulaire.preparer(utilisateur);

        ButtonType annuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmer = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        panneau.getButtonTypes().setAll(annuler, confirmer);
        panneau.lookupButton(annuler).getStyleClass().add("btn-secondary");
        Button boutonConfirmer = (Button) panneau.lookupButton(confirmer);
        boutonConfirmer.getStyleClass().add("btn-primary");
        boutonConfirmer.addEventFilter(ActionEvent.ACTION, e -> {
            try {
                formulaire.enregistrer(service);
            } catch (ValidationException ex) {
                Dialogues.erreur(ex.getMessage());
                e.consume(); // keep the dialog open so the user can fix the field
            }
        });

        Dialog<ButtonType> dialogue = new Dialog<>();
        dialogue.setDialogPane(panneau);
        dialogue.setTitle(formulaire.titre());
        dialogue.initOwner(table.getScene().getWindow());
        if (dialogue.showAndWait().orElse(annuler) == confirmer) {
            rafraichir();
        }
    }

    private void supprimer(Utilisateur utilisateur) {
        if (utilisateur == null
                || !Dialogues.confirmer("Supprimer", "Supprimer " + utilisateur.getNomComplet() + " ?")) {
            return;
        }
        try {
            service.supprimer(utilisateur.getId());
            rafraichir();
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
    }

    private void rafraichir() {
        donnees.setAll(service.lister());
    }

    private static TableCell<Utilisateur, String> texte(String styleClass) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setText(empty ? null : item);
                getStyleClass().removeAll("cell-strong", "cell-normal");
                if (!empty) {
                    getStyleClass().add(styleClass);
                }
            }
        };
    }

    private static TableCell<Utilisateur, String> badge(Function<String, String> classePour) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label("● " + item);
                badge.getStyleClass().add(classePour.apply(item));
                setGraphic(badge);
            }
        };
    }
}
