package ma.translogix.controller.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.service.UtilisateurService;
import ma.translogix.service.UtilisateurService.CritereUtilisateur;
import ma.translogix.util.Navigator;
import ma.translogix.util.ValidationException;

/** User management screen: multi-criteria search (text, role, status), right-click menu and form dialogs, create/edit in a dialog. No business rules here. */
public class UtilisateursController {

    private static final String FORMULAIRE = Navigator.RACINE + "admin/utilisateur-dialog.fxml";
    private static final String FORMULAIRE_MOT_DE_PASSE = Navigator.RACINE + "admin/mot-de-passe-dialog.fxml";

    private static final String TOUS_LES_ROLES = "Tous les rôles";
    private static final String TOUS_LES_STATUTS = "Tous les statuts";
    private static final String ACTIFS = "Actifs";
    private static final String INACTIFS = "Inactifs";

    @FXML private TextField recherche;
    @FXML private ComboBox<String> filtreRole;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private Label compteur;
    @FXML private TableView<Utilisateur> table;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colPrenom;
    @FXML private TableColumn<Utilisateur, String> colEmail;
    @FXML private TableColumn<Utilisateur, String> colRole;
    @FXML private TableColumn<Utilisateur, String> colActif;
    @FXML private TableColumn<Utilisateur, Utilisateur> colActions;

    private final UtilisateurService service = AppContext.utilisateurService();
    private final ObservableList<Utilisateur> donnees = FXCollections.observableArrayList();
    private FilteredList<Utilisateur> filtres;

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
                if (empty || u == null) {
                    setGraphic(null);
                    return;
                }
                // The Gérant account cannot be deleted: no dead "Supprimer" link on its row.
                supprimer.setVisible(u.getRole() != Role.GERANT);
                supprimer.setManaged(u.getRole() != Role.GERANT);
                setGraphic(boutons);
            }
        });

        List<String> roles = new ArrayList<>();
        roles.add(TOUS_LES_ROLES);
        Arrays.stream(Role.values()).map(Role::getLibelle).forEach(roles::add);
        filtreRole.setItems(FXCollections.observableArrayList(roles));
        filtreStatut.setItems(FXCollections.observableArrayList(TOUS_LES_STATUTS, ACTIFS, INACTIFS));
        filtreRole.getSelectionModel().select(TOUS_LES_ROLES);
        filtreStatut.getSelectionModel().select(TOUS_LES_STATUTS);

        filtres = new FilteredList<>(donnees);
        recherche.textProperty().addListener((obs, avant, apres) -> appliquerFiltres());
        filtreRole.valueProperty().addListener((obs, avant, apres) -> appliquerFiltres());
        filtreStatut.valueProperty().addListener((obs, avant, apres) -> appliquerFiltres());
        SortedList<Utilisateur> tries = new SortedList<>(filtres);
        tries.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(tries);
        table.setRowFactory(t -> ligneAvecMenu());
        rafraichir();
    }

    /** Text, role and status combined (AND); the rule itself lives in UtilisateurService.correspond. */
    private void appliquerFiltres() {
        Role role = Arrays.stream(Role.values())
                .filter(r -> r.getLibelle().equals(filtreRole.getValue())).findFirst().orElse(null);
        String statut = filtreStatut.getValue();
        Boolean actif = ACTIFS.equals(statut) ? Boolean.TRUE : INACTIFS.equals(statut) ? Boolean.FALSE : null;
        CritereUtilisateur critere = new CritereUtilisateur(recherche.getText(), role, actif);
        filtres.setPredicate(u -> UtilisateurService.correspond(u, critere));
        int n = filtres.size();
        compteur.setText(n + (n > 1 ? " comptes" : " compte") + " sur " + donnees.size());
    }

    @FXML
    private void reinitialiser() {
        recherche.clear();
        filtreRole.getSelectionModel().select(TOUS_LES_ROLES);
        filtreStatut.getSelectionModel().select(TOUS_LES_STATUTS);
    }

    @FXML
    private void nouveau() {
        ouvrirFormulaire(null);
    }

    /** Create (utilisateur == null) or edit dialog. */
    private void ouvrirFormulaire(Utilisateur utilisateur) {
        this.<UtilisateurFormulaireController>ouvrirDialogue(FORMULAIRE, f -> f.preparer(utilisateur),
                f -> f.enregistrer(service), UtilisateurFormulaireController::titre);
    }

    private void reinitialiserMotDePasse(Utilisateur utilisateur) {
        this.<MotDePasseFormulaireController>ouvrirDialogue(FORMULAIRE_MOT_DE_PASSE, f -> f.preparer(utilisateur),
                f -> f.enregistrer(service), MotDePasseFormulaireController::titre);
    }

    /**
     * Loads a form dialog, adds Annuler / Confirmer, and refreshes the table once confirmed.
     * On a ValidationException the error is shown and the dialog stays open so the user can fix the field.
     */
    private <C> void ouvrirDialogue(String fxml, Consumer<C> preparer, Consumer<C> enregistrer,
                                    Function<C, String> titre) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        DialogPane panneau;
        try {
            panneau = loader.load();
        } catch (IOException e) {
            Dialogues.erreur("Impossible d'ouvrir le formulaire : " + e.getMessage());
            return;
        }
        C formulaire = loader.getController();
        preparer.accept(formulaire);

        ButtonType annuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmer = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        panneau.getButtonTypes().setAll(annuler, confirmer);
        panneau.lookupButton(annuler).getStyleClass().add("btn-secondary");
        Button boutonConfirmer = (Button) panneau.lookupButton(confirmer);
        boutonConfirmer.getStyleClass().add("btn-primary");
        boutonConfirmer.addEventFilter(ActionEvent.ACTION, e -> {
            try {
                enregistrer.accept(formulaire);
            } catch (ValidationException ex) {
                Dialogues.erreur(ex.getMessage());
                e.consume(); // keep the dialog open so the user can fix the field
            }
        });

        Dialog<ButtonType> dialogue = new Dialog<>();
        dialogue.setDialogPane(panneau);
        dialogue.setTitle(titre.apply(formulaire));
        dialogue.initOwner(table.getScene().getWindow());
        if (dialogue.showAndWait().orElse(annuler) == confirmer) {
            rafraichir();
        }
    }

    /** Right-click: activate or deactivate, after confirmation. The Gérant rule is checked by the service. */
    private void basculerStatut(Utilisateur utilisateur) {
        boolean activer = !utilisateur.isActif();
        String verbe = activer ? "Activer" : "Désactiver";
        String consequence = activer
                ? "La personne pourra de nouveau se connecter."
                : "La personne ne pourra plus se connecter tant que le compte reste inactif.";
        if (!Dialogues.confirmer(verbe + " le compte",
                verbe + " le compte de " + utilisateur.getNomComplet() + " ?\n" + consequence)) {
            return;
        }
        try {
            service.changerStatut(utilisateur.getId(), activer);
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
        rafraichir(); // also after a refusal: the audit log changed and the row must show the real state
    }

    /**
     * One context menu per row, built for that row's account: the Gérant gets no "Désactiver" or "Supprimer"
     * entry (the service refuses them anyway). Double-click on a row opens the edit dialog.
     */
    private TableRow<Utilisateur> ligneAvecMenu() {
        TableRow<Utilisateur> ligne = new TableRow<>();
        MenuItem modifier = new MenuItem("Modifier…");
        MenuItem statut = new MenuItem();
        MenuItem motDePasse = new MenuItem("Réinitialiser le mot de passe…");
        MenuItem email = new MenuItem("Copier l'email");
        MenuItem supprimer = new MenuItem("Supprimer…");
        supprimer.getStyleClass().add("menu-danger");
        modifier.setOnAction(e -> ouvrirFormulaire(ligne.getItem()));
        statut.setOnAction(e -> basculerStatut(ligne.getItem()));
        motDePasse.setOnAction(e -> reinitialiserMotDePasse(ligne.getItem()));
        email.setOnAction(e -> PressePapiers.copier(ligne.getItem().getEmail()));
        supprimer.setOnAction(e -> supprimer(ligne.getItem()));
        ContextMenu menu = new ContextMenu(modifier, statut, motDePasse, email, new SeparatorMenuItem(), supprimer);

        // Entries are set when the menu opens, so they follow the account's current state (it may have changed).
        menu.setOnShowing(e -> {
            Utilisateur u = ligne.getItem();
            boolean gerant = u.getRole() == Role.GERANT;
            statut.setText(u.isActif() ? "Désactiver le compte…" : "Activer le compte…");
            statut.setVisible(!gerant || !u.isActif());
            supprimer.setVisible(!gerant);
            menu.getItems().get(4).setVisible(!gerant); // separator before "Supprimer"
        });
        ligne.emptyProperty().addListener((obs, avant, vide) -> ligne.setContextMenu(vide ? null : menu));
        ligne.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !ligne.isEmpty()) {
                ouvrirFormulaire(ligne.getItem());
            }
        });
        return ligne;
    }

    private void supprimer(Utilisateur utilisateur) {
        if (utilisateur == null
                || !Dialogues.confirmer("Supprimer", "Supprimer définitivement le compte de "
                        + utilisateur.getNomComplet() + " (" + utilisateur.getEmail() + ") ?\n"
                        + "Pour seulement bloquer l'accès, utilisez plutôt « Désactiver le compte ».")) {
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
        appliquerFiltres();
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
