package ma.translogix.controller.comptable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Client;
import ma.translogix.model.ModePaiement;
import ma.translogix.service.TresorerieService;
import ma.translogix.service.TresorerieService.CritereTresorerie;
import ma.translogix.service.TresorerieService.Encaissement;
import ma.translogix.service.TresorerieService.LigneStatut;
import ma.translogix.service.TresorerieService.Tresorerie;
import ma.translogix.util.Formats;
import ma.translogix.util.Navigator;
import ma.translogix.util.Recherche;
import ma.translogix.util.ValidationException;

/**
 * "Trésorerie &amp; bilan": invoiced revenue, cash received and receivables come from TresorerieService.
 * The two chart series are deliberately separate: an invoice and its payment must never be added together.
 */
public class TresorerieController {

    private static final String TOUS_LES_CLIENTS = "Tous les clients";
    private static final String TOUS_LES_MODES = "Tous les modes";
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMM yy", Locale.FRENCH);

    @FXML private Label valeurFacture;
    @FXML private Label valeurEncaisse;
    @FXML private Label valeurCreances;
    @FXML private Label valeurTaux;
    @FXML private Label piedFacture;
    @FXML private Label piedEncaisse;
    @FXML private Label piedCreances;
    @FXML private Label piedTaux;
    @FXML private Label resumePeriode;
    @FXML private Label resumeFacturation;
    @FXML private Label statutPayee;
    @FXML private Label statutAttente;
    @FXML private Label statutRetard;
    @FXML private Label statutAnnulee;
    @FXML private BarChart<String, Number> graphiqueTresorerie;
    @FXML private CategoryAxis axeMois;
    @FXML private NumberAxis axeMontants;
    @FXML private DatePicker dateDu;
    @FXML private DatePicker dateAu;
    @FXML private ComboBox<Client> filtreClient;
    @FXML private ComboBox<ModePaiement> filtreMode;
    @FXML private TextField recherche;
    @FXML private TableView<Encaissement> table;
    @FXML private TableColumn<Encaissement, String> colDate;
    @FXML private TableColumn<Encaissement, String> colFacture;
    @FXML private TableColumn<Encaissement, String> colClient;
    @FXML private TableColumn<Encaissement, String> colMode;
    @FXML private TableColumn<Encaissement, String> colReference;
    @FXML private TableColumn<Encaissement, String> colMontant;

    private final TresorerieService service = AppContext.tresorerieService();
    private final ObservableList<Encaissement> donnees = FXCollections.observableArrayList();
    private FilteredList<Encaissement> filtres;

    @FXML
    private void initialize() {
        configurerColonnes();
        configurerFiltres();
        filtres = new FilteredList<>(donnees);
        SortedList<Encaissement> tries = new SortedList<>(filtres);
        tries.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(tries);
        table.setRowFactory(tv -> ligneAvecMenu());
        rafraichir();
    }

    private void configurerColonnes() {
        colDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                Formats.date(c.getValue().date())));
        colDate.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colFacture.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().numeroFacture() == null ? "—" : c.getValue().numeroFacture()));
        colFacture.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colClient.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().client()));
        colClient.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colMode.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().mode() == null ? "—" : c.getValue().mode().getLibelle()));
        colMode.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colReference.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().reference() == null ? "—" : c.getValue().reference()));
        colReference.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colMontant.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                Formats.mad(c.getValue().montantTtc())));
        colMontant.setCellFactory(col -> ComptableUi.texte("cell-strong"));
    }

    private void configurerFiltres() {
        LocalDate aujourdHui = LocalDate.now();
        dateDu.setValue(YearMonth.from(aujourdHui).atDay(1));
        dateAu.setValue(aujourdHui);

        ObservableList<Client> clients = FXCollections.observableArrayList(service.clients());
        filtreClient.setItems(clients);
        filtreClient.setPromptText(TOUS_LES_CLIENTS);
        ObservableList<ModePaiement> modes = FXCollections.observableArrayList();
        modes.addAll(ModePaiement.values());
        filtreMode.setItems(modes);
        filtreMode.setPromptText(TOUS_LES_MODES);

        recherche.textProperty().addListener((obs, ancien, valeur) -> appliquerRecherche());
        dateDu.valueProperty().addListener((obs, ancien, valeur) -> rafraichir());
        dateAu.valueProperty().addListener((obs, ancien, valeur) -> rafraichir());
        filtreClient.valueProperty().addListener((obs, ancien, valeur) -> rafraichir());
        filtreMode.valueProperty().addListener((obs, ancien, valeur) -> rafraichir());
    }

    /** Free-text search only narrows the history table; the KPIs keep the selected period/client/mode. */
    private void appliquerRecherche() {
        filtres.setPredicate(e -> Recherche.correspond(recherche.getText(), e.numeroFacture(), e.client(),
                e.reference(), e.mode() == null ? null : e.mode().getLibelle()));
    }

    @FXML
    private void reinitialiser() {
        LocalDate aujourdHui = LocalDate.now();
        dateDu.setValue(YearMonth.from(aujourdHui).atDay(1));
        dateAu.setValue(aujourdHui);
        filtreClient.setValue(null);
        filtreMode.setValue(null);
        recherche.clear();
        rafraichir();
    }

    @FXML
    private void voirFactures() {
        Navigator.ouvrir("comptable/factures.fxml");
    }

    @FXML
    private void rafraichir() {
        CritereTresorerie critere = new CritereTresorerie(dateDu.getValue(), dateAu.getValue(),
                filtreClient.getValue() == null ? null : filtreClient.getValue().getId(), filtreMode.getValue());
        try {
            Tresorerie resultat = service.generer(critere);
            afficherSynthese(resultat);
            remplirGraphique(resultat);
            donnees.setAll(resultat.encaissements());
            appliquerRecherche();
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
    }

    private void afficherSynthese(Tresorerie resultat) {
        var s = resultat.synthese();
        valeurFacture.setText(Formats.montant(s.factureTtc()));
        valeurEncaisse.setText(Formats.montant(s.encaisse()));
        valeurCreances.setText(Formats.montant(s.creancesTtc()));
        valeurTaux.setText(s.tauxEncaissement().setScale(0, java.math.RoundingMode.HALF_UP) + " %");

        piedFacture.setText(s.nombreFactures() + (s.nombreFactures() > 1 ? " factures émises" : " facture émise"));
        piedEncaisse.setText(s.nombreEncaissements()
                + (s.nombreEncaissements() > 1 ? " encaissements enregistrés" : " encaissement enregistré"));
        piedCreances.setText("factures en attente de règlement");
        piedTaux.setText("part du facturé TTC encaissée");

        resumePeriode.setText(periodeLibelle(dateDu.getValue(), dateAu.getValue()));
        resumeFacturation.setText("HT " + Formats.mad(s.factureHt()) + " · TVA " + Formats.mad(s.tva())
                + " · TTC " + Formats.mad(s.factureTtc()));

        for (LigneStatut ligne : resultat.statuts()) {
            Label cible = switch (ligne.statut()) {
                case "Payée" -> statutPayee;
                case "En attente" -> statutAttente;
                case "En retard" -> statutRetard;
                default -> statutAnnulee;
            };
            cible.setText(ligne.nombre() + " · " + Formats.mad(ligne.montantTtc()));
        }
    }

    private static String periodeLibelle(LocalDate du, LocalDate au) {
        if (du == null && au == null) {
            return "Toutes les périodes";
        }
        if (du == null) {
            return "Jusqu'au " + Formats.date(au);
        }
        if (au == null) {
            return "Depuis le " + Formats.date(du);
        }
        return Formats.date(du) + " → " + Formats.date(au);
    }

    private void remplirGraphique(Tresorerie resultat) {
        graphiqueTresorerie.getData().clear();
        XYChart.Series<String, Number> facturees = new XYChart.Series<>();
        facturees.setName("Facturé TTC");
        XYChart.Series<String, Number> encaissees = new XYChart.Series<>();
        encaissees.setName("Encaissé");

        for (TresorerieService.MoisTresorerie mois : resultat.mois()) {
            String libelle = mois.mois().format(MOIS);
            XYChart.Data<String, Number> pointFacture = new XYChart.Data<>(libelle, mois.factureTtc());
            XYChart.Data<String, Number> pointCaisse = new XYChart.Data<>(libelle, mois.encaisse());
            facturees.getData().add(pointFacture);
            encaissees.getData().add(pointCaisse);
            quandAffiche(pointFacture.nodeProperty(), noeud -> {
                noeud.getStyleClass().add("comptable-tresorerie-facture");
                Tooltip.install(noeud, new Tooltip("Facturé TTC " + mois.mois() + " : "
                        + Formats.mad(mois.factureTtc())));
            });
            quandAffiche(pointCaisse.nodeProperty(), noeud -> {
                noeud.getStyleClass().add("comptable-tresorerie-encaisse");
                Tooltip.install(noeud, new Tooltip("Encaissé " + mois.mois() + " : "
                        + Formats.mad(mois.encaisse())));
            });
        }
        graphiqueTresorerie.getData().addAll(facturees, encaissees);
        axeMontants.setTickLabelFormatter(new NumberAxis.DefaultFormatter(axeMontants) {
            @Override
            public String toString(Number valeur) {
                return valeur.longValue() / 1000 + " k";
            }
        });
    }

    /** Chart nodes are created lazily: apply the styling as soon as JavaFX creates them. */
    private static void quandAffiche(ReadOnlyObjectProperty<Node> propriete, Consumer<Node> action) {
        if (propriete.get() != null) {
            action.accept(propriete.get());
            return;
        }
        propriete.addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Node> obs, Node ancien, Node noeud) {
                if (noeud != null) {
                    propriete.removeListener(this);
                    action.accept(noeud);
                }
            }
        });
    }

    private TableRow<Encaissement> ligneAvecMenu() {
        TableRow<Encaissement> ligne = new TableRow<>();
        MenuItem voir = new MenuItem("Voir les factures du client");
        voir.setOnAction(e -> voirFactures(ligne.getItem()));
        ContextMenu menu = new ContextMenu(voir);
        ligne.emptyProperty().addListener((obs, avant, vide) -> ligne.setContextMenu(vide ? null : menu));
        return ligne;
    }

    private void voirFactures(Encaissement encaissement) {
        if (encaissement == null || encaissement.clientId() == null) {
            return;
        }
        FacturesController.preparerFiltreClient(encaissement.clientId());
        Navigator.ouvrir("comptable/factures.fxml");
    }

    /** Kept for the FXML header: the page never invents a "solde" without expense/payroll data. */
    @FXML
    private void afficherBilan() {
        Dialogues.info("Bilan complet",
                "Le bilan complet nécessite les dépenses et la paie. Ces flux ne sont pas encore consolidés : "
                        + "aucun solde n'est affiché pour éviter un chiffre inventé.");
    }
}
