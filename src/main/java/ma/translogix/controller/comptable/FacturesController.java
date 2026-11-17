package ma.translogix.controller.comptable;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import ma.translogix.AppContext;
import ma.translogix.model.Facture;
import ma.translogix.model.StatutFacture;
import ma.translogix.service.ClientService;
import ma.translogix.service.FactureService;
import ma.translogix.util.Formats;
import ma.translogix.util.Recherche;

/** "Facturation clients": searchable invoice register; new invoice and payment in dialogs. No rules here. */
public class FacturesController {

    @FXML private Button boutonNouvelle;
    @FXML private TextField recherche;
    @FXML private TableView<Facture> table;
    @FXML private TableColumn<Facture, String> colNumero;
    @FXML private TableColumn<Facture, String> colClient;
    @FXML private TableColumn<Facture, String> colEmission;
    @FXML private TableColumn<Facture, String> colEcheance;
    @FXML private TableColumn<Facture, String> colHt;
    @FXML private TableColumn<Facture, String> colTva;
    @FXML private TableColumn<Facture, String> colTtc;
    @FXML private TableColumn<Facture, String> colStatut;
    @FXML private TableColumn<Facture, Facture> colActions;

    private final FactureService factures = AppContext.factureService();
    private final ClientService clients = AppContext.clientService();
    private final ObservableList<Facture> donnees = FXCollections.observableArrayList();
    private final Map<Long, String> nomsClients = new HashMap<>();

    @FXML
    private void initialize() {
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumero()));
        colNumero.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colClient.setCellValueFactory(c -> new SimpleStringProperty(client(c.getValue())));
        colClient.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colEmission.setCellValueFactory(c -> new SimpleStringProperty(Formats.date(c.getValue().getDateEmission())));
        colEmission.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colEcheance.setCellValueFactory(c -> new SimpleStringProperty(Formats.date(c.getValue().getDateEcheance())));
        colEcheance.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colHt.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getMontantHT())));
        colHt.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colTva.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getMontantTva())));
        colTva.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colTtc.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getMontantTtc())));
        colTtc.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(factures.libelleStatut(c.getValue())));
        colStatut.setCellFactory(col -> ComptableUi.badge(ComptableUi::classeStatut));
        colActions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button regler = new Button("Régler");

            {
                regler.getStyleClass().add("link-orange");
                regler.setMinWidth(Region.USE_PREF_SIZE); // never "Ré..."
                regler.setOnAction(e -> regler(getItem()));
            }

            @Override
            protected void updateItem(Facture f, boolean empty) {
                super.updateItem(f, empty);
                setText(null);
                // Only unpaid invoices can be paid; the Gérant's view is read-only.
                boolean reglable = !empty && f != null && f.getStatut() == StatutFacture.EN_ATTENTE
                        && !ComptableUi.lectureSeule();
                setGraphic(reglable ? regler : null);
            }
        });

        FilteredList<Facture> filtres = new FilteredList<>(donnees);
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(f -> Recherche.correspond(requete, f.getNumero(), client(f),
                        f.getReferenceTrajet(), Formats.montant(f.getMontantHT()),
                        Formats.montant(f.getMontantTtc()), factures.libelleStatut(f))));
        SortedList<Facture> tries = new SortedList<>(filtres);
        tries.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(tries);

        if (ComptableUi.lectureSeule()) {
            ComptableUi.masquer(boutonNouvelle);
        }
        rafraichir();
    }

    @FXML
    private void nouvelle() {
        if (ComptableUi.<FactureFormulaireController>ouvrirFormulaire(table, FactureFormulaireController.FXML,
                FactureFormulaireController::preparer)) {
            rafraichir();
        }
    }

    private void regler(Facture facture) {
        if (facture != null && ComptableUi.<PaiementFormulaireController>ouvrirFormulaire(table,
                PaiementFormulaireController.FXML, f -> f.preparer(facture, client(facture)))) {
            rafraichir();
        }
    }

    private void rafraichir() {
        nomsClients.clear();
        clients.lister().forEach(c -> nomsClients.put(c.getId(), c.getNom()));
        donnees.setAll(factures.lister());
    }

    private String client(Facture f) {
        return nomsClients.getOrDefault(f.getClientId(), "—");
    }
}
