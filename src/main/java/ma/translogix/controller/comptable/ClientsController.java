package ma.translogix.controller.comptable;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Client;
import ma.translogix.service.ClientService;
import ma.translogix.util.Recherche;
import ma.translogix.util.ValidationException;

/** "Clients": searchable directory, create / edit in a dialog, delete with confirmation. No rules here. */
public class ClientsController {

    @FXML private Button boutonNouveau;
    @FXML private TextField recherche;
    @FXML private TableView<Client> table;
    @FXML private TableColumn<Client, String> colNom;
    @FXML private TableColumn<Client, String> colIce;
    @FXML private TableColumn<Client, String> colTelephone;
    @FXML private TableColumn<Client, String> colEmail;
    @FXML private TableColumn<Client, String> colAdresse;
    @FXML private TableColumn<Client, String> colDelai;
    @FXML private TableColumn<Client, Client> colActions;

    private final ClientService service = AppContext.clientService();
    private final ObservableList<Client> donnees = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colNom.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colIce.setCellValueFactory(c -> new SimpleStringProperty(tiret(c.getValue().getIce())));
        colIce.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colTelephone.setCellValueFactory(c -> new SimpleStringProperty(tiret(c.getValue().getTelephone())));
        colTelephone.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(tiret(c.getValue().getEmail())));
        colEmail.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colAdresse.setCellValueFactory(c -> new SimpleStringProperty(tiret(c.getValue().getAdresse())));
        colAdresse.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colDelai.setCellValueFactory(c -> new SimpleStringProperty(delai(c.getValue())));
        colDelai.setCellFactory(col -> ComptableUi.badge(texte -> "Comptant".equals(texte) ? "badge-amber" : "badge-blue"));
        colActions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button modifier = new Button("Modifier");
            private final Button supprimer = new Button("Supprimer");
            private final HBox boutons = new HBox(6, modifier, supprimer);

            {
                boutons.setAlignment(Pos.CENTER_LEFT);
                modifier.getStyleClass().add("link-orange");
                supprimer.getStyleClass().add("link-red");
                modifier.setMinWidth(Region.USE_PREF_SIZE);
                supprimer.setMinWidth(Region.USE_PREF_SIZE);
                modifier.setOnAction(e -> ouvrirFormulaire(getItem()));
                supprimer.setOnAction(e -> supprimer(getItem()));
            }

            @Override
            protected void updateItem(Client c, boolean empty) {
                super.updateItem(c, empty);
                setText(null);
                setGraphic(empty || c == null || ComptableUi.lectureSeule() ? null : boutons);
            }
        });

        FilteredList<Client> filtres = new FilteredList<>(donnees);
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(c -> Recherche.correspond(requete, c.getNom(), c.getIce(),
                        c.getTelephone(), c.getEmail(), c.getAdresse())));
        SortedList<Client> tries = new SortedList<>(filtres);
        tries.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(tries);

        if (ComptableUi.lectureSeule()) {
            ComptableUi.masquer(boutonNouveau);
        }
        rafraichir();
    }

    @FXML
    private void nouveau() {
        ouvrirFormulaire(null);
    }

    private void ouvrirFormulaire(Client client) {
        if (ComptableUi.<ClientFormulaireController>ouvrirFormulaire(table, ClientFormulaireController.FXML,
                f -> f.preparer(client))) {
            rafraichir();
        }
    }

    private void supprimer(Client client) {
        if (client == null || !Dialogues.confirmer("Supprimer", "Supprimer le client « " + client.getNom() + " » ?")) {
            return;
        }
        try {
            service.supprimer(client.getId());
            rafraichir();
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
    }

    private void rafraichir() {
        donnees.setAll(service.lister());
    }

    private static String delai(Client c) {
        return c.getDelaiPaiementJours() == 0 ? "Comptant" : c.getDelaiPaiementJours() + " jours";
    }

    private static String tiret(String valeur) {
        return valeur == null ? "—" : valeur;
    }
}
