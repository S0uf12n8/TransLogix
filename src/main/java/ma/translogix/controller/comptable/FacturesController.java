package ma.translogix.controller.comptable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import ma.translogix.AppContext;
import ma.translogix.dao.CritereFacture;
import ma.translogix.model.Client;
import ma.translogix.model.Facture;
import ma.translogix.model.StatutFacture;
import ma.translogix.service.ClientService;
import ma.translogix.service.FactureService;
import ma.translogix.util.Formats;
import ma.translogix.util.ValidationException;

/** "Facturation clients": searchable invoice register; new invoice and payment in dialogs. No rules here. */
public class FacturesController {

    /** First option of the status filter: no restriction. */
    private static final String TOUS_LES_STATUTS = "Tous les statuts";
    private static final String TOUS_LES_CLIENTS = "Tous les clients";

    @FXML private Button boutonNouvelle;
    @FXML private TextField recherche;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<Client> filtreClient;
    @FXML private DatePicker filtreDu;
    @FXML private DatePicker filtreAu;
    @FXML private TextField filtreMontantMin;
    @FXML private TextField filtreMontantMax;
    @FXML private Button boutonReset;
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
    private FilteredList<Facture> filtres;

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

        filtres = new FilteredList<>(donnees);
        SortedList<Facture> tries = new SortedList<>(filtres);
        tries.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(tries);

        preparerFiltres();
        if (ComptableUi.lectureSeule()) {
            ComptableUi.masquer(boutonNouvelle);
        }
        rafraichir();
    }

    /** Populates the filter controls; every change recomputes the displayed rows. */
    private void preparerFiltres() {
        filtreStatut.setItems(FXCollections.observableArrayList(TOUS_LES_STATUTS, "En attente", "En retard",
                StatutFacture.PAYEE.getLibelle(), StatutFacture.ANNULEE.getLibelle()));
        filtreStatut.setValue(TOUS_LES_STATUTS);

        filtreClient.setItems(FXCollections.observableArrayList(clients.lister()));
        filtreClient.setValue(null);
        filtreClient.setPromptText(TOUS_LES_CLIENTS); // Client.toString() is the company name

        for (DatePicker date : new DatePicker[] {filtreDu, filtreAu}) {
            date.setConverter(dateConverter());
        }

        recherche.textProperty().addListener((obs, ancien, requete) -> appliquerFiltres());
        filtreStatut.valueProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
        filtreClient.valueProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
        filtreDu.valueProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
        filtreAu.valueProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
        filtreMontantMin.textProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
        filtreMontantMax.textProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
    }

    /** Clears every criterion at once (the table comes back sorted, newest first). */
    @FXML
    private void reinitialiser() {
        recherche.clear();
        filtreStatut.setValue(TOUS_LES_STATUTS);
        filtreClient.setValue(null);
        filtreDu.setValue(null);
        filtreAu.setValue(null);
        filtreMontantMin.clear();
        filtreMontantMax.clear();
        appliquerFiltres();
    }

    /**
     * Asks the service/DAO for the matching ids, then keeps only those rows in the table. The table keeps its
     * live observable list (so a payment still refreshes it), while the real filtering logic stays in the service
     * and can move to a JDBC query without changing this screen.
     */
    private void appliquerFiltres() {
        Set<Long> correspondants = factures.rechercher(critere()).stream()
                .map(Facture::getId)
                .collect(Collectors.toSet());
        filtres.setPredicate(f -> correspondants.contains(f.getId()));
    }

    private CritereFacture critere() {
        return new CritereFacture(recherche.getText(), clientFiltre(), statutFiltre(), filtreDu.getValue(),
                filtreAu.getValue(), montant(filtreMontantMin.getText()), montant(filtreMontantMax.getText()));
    }

    private Long clientFiltre() {
        Client c = filtreClient.getValue();
        return c == null ? null : c.getId();
    }

    private String statutFiltre() {
        String valeur = filtreStatut.getValue();
        return valeur == null || valeur.equals(TOUS_LES_STATUTS) ? null : valeur;
    }

    /** Invalid amount text is ignored rather than hiding every row; the same parser as the invoice dialog. */
    private static BigDecimal montant(String texte) {
        BigDecimal valeur = FactureFormulaireController.lireMontant(texte);
        return valeur != null && valeur.signum() >= 0 ? valeur : null;
    }

    private static StringConverter<LocalDate> dateConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return Formats.date(date);
            }

            @Override
            public LocalDate fromString(String texte) {
                try {
                    return texte == null || texte.isBlank() ? null : Formats.lireDate(texte);
                } catch (ValidationException e) {
                    return null;
                }
            }
        };
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
        appliquerFiltres();
    }

    private String client(Facture f) {
        return nomsClients.getOrDefault(f.getClientId(), "—");
    }
}
