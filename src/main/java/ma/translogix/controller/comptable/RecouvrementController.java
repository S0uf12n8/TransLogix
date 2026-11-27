package ma.translogix.controller.comptable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.dao.CritereFacture;
import ma.translogix.model.Facture;
import ma.translogix.service.FactureService;
import ma.translogix.service.FactureService.ClientARelancer;
import ma.translogix.service.FactureService.Recouvrement;
import ma.translogix.service.FactureService.TrancheAge;
import ma.translogix.service.FactureService.TrancheAgee;
import ma.translogix.util.Formats;
import ma.translogix.util.Navigator;
import ma.translogix.util.Recherche;

/** "Créances & recouvrement": aging chart and clients to chase. Every figure comes from FactureService. */
public class RecouvrementController {

    private static final String TOUTES_LES_TRANCHES = "Toutes les tranches";

    @FXML private StackPane kpiTotal;
    @FXML private StackPane kpiClients;
    @FXML private StackPane kpiAncien;
    @FXML private Label valeurTotal;
    @FXML private Label valeurClients;
    @FXML private Label valeurAncien;
    @FXML private Label piedTotal;
    @FXML private Label piedClients;
    @FXML private Label sousTitreBalance;
    @FXML private BarChart<String, Number> graphiqueTranches;
    @FXML private NumberAxis axeTranches;
    @FXML private TextField recherche;
    @FXML private ComboBox<String> filtreTranche;
    @FXML private TableView<ClientARelancer> table;
    @FXML private TableColumn<ClientARelancer, String> colClient;
    @FXML private TableColumn<ClientARelancer, String> colIce;
    @FXML private TableColumn<ClientARelancer, String> colFactures;
    @FXML private TableColumn<ClientARelancer, String> colMontant;
    @FXML private TableColumn<ClientARelancer, String> colEcheance;
    @FXML private TableColumn<ClientARelancer, String> colRetard;

    private final FactureService factures = AppContext.factureService();
    private final ObservableList<ClientARelancer> donnees = FXCollections.observableArrayList();
    private FilteredList<ClientARelancer> filtres;

    @FXML
    private void initialize() {
        colClient.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nomClient()));
        colClient.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colIce.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().ice() == null ? "—" : c.getValue().ice()));
        colIce.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colFactures.setCellValueFactory(c -> new SimpleStringProperty(
                Integer.toString(c.getValue().nombreFacturesEnRetard())));
        colFactures.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colMontant.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().totalDuTtc())));
        colMontant.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colEcheance.setCellValueFactory(c -> new SimpleStringProperty(
                Formats.date(c.getValue().echeanceLaPlusAncienne())));
        colEcheance.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colRetard.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().joursDeRetard() + " j"));
        colRetard.setCellFactory(col -> ComptableUi.badge(libelle -> "badge-red"));

        table.setRowFactory(tv -> ligneAvecMenu());
        filtres = new FilteredList<>(donnees);
        SortedList<ClientARelancer> tries = new SortedList<>(filtres);
        tries.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(tries);

        for (Region carte : List.of(kpiTotal, kpiClients, kpiAncien)) {
            ComptableUi.arrondir(carte);
        }
        preparerFiltres();
        rafraichir();
    }

    private void preparerFiltres() {
        filtreTranche.setItems(FXCollections.observableArrayList(TOUTES_LES_TRANCHES,
                TrancheAge.J1_A_30.getLibelle(), TrancheAge.J31_A_60.getLibelle(),
                TrancheAge.J61_A_90.getLibelle(), TrancheAge.PLUS_DE_90.getLibelle()));
        filtreTranche.setValue(TOUTES_LES_TRANCHES);
        recherche.textProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
        filtreTranche.valueProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
    }

    /** Search and bracket are display filters only: the figures and the order come from the service. */
    private void appliquerFiltres() {
        String tranche = filtreTranche.getValue();
        filtres.setPredicate(c -> {
            if (tranche != null && !TOUTES_LES_TRANCHES.equals(tranche)
                    && !tranche.equals(TrancheAge.pourJours(c.joursDeRetard()).getLibelle())) {
                return false;
            }
            return Recherche.correspond(recherche.getText(), c.nomClient(), c.ice());
        });
    }

    @FXML
    private void reinitialiser() {
        recherche.clear();
        filtreTranche.setValue(TOUTES_LES_TRANCHES);
    }

    private void rafraichir() {
        Recouvrement r = factures.recouvrement();
        valeurTotal.setText(Formats.montant(r.totalEnRetard()));
        valeurClients.setText(Integer.toString(r.nombreClientsARelancer()));
        valeurAncien.setText(r.plusAncienDelaiJours() == null ? "—" : r.plusAncienDelaiJours().toString());
        piedTotal.setText("factures échues non réglées");
        piedClients.setText("à contacter en priorité");

        long nombreEchues = r.tranches().stream()
                .filter(t -> t.tranche() != TrancheAge.NON_ECHUE)
                .mapToLong(TrancheAgee::nombreFactures)
                .sum();
        sousTitreBalance.setText(nombreEchues + (nombreEchues > 1 ? " factures échues · " : " facture échue · ")
                + Formats.mad(r.totalEnRetard()) + " TTC");

        remplirGraphique(r.tranches());
        donnees.setAll(r.clientsARelancer());
        appliquerFiltres();
    }

    private void remplirGraphique(List<TrancheAgee> tranches) {
        graphiqueTranches.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Montant TTC");
        graphiqueTranches.getData().add(serie);
        for (TrancheAgee tranche : tranches) {
            XYChart.Data<String, Number> point =
                    new XYChart.Data<>(tranche.tranche().getLibelle(), tranche.montantTtc());
            serie.getData().add(point);
            quandAffiche(point.nodeProperty(), noeud -> {
                noeud.getStyleClass().add("comptable-recouvrement-" + styleTranche(tranche.tranche()));
                Tooltip.install(noeud, new Tooltip(tranche.tranche().getLibelle() + " : "
                        + tranche.nombreFactures() + (tranche.nombreFactures() > 1 ? " factures" : " facture")
                        + " · " + Formats.mad(tranche.montantTtc())));
            });
        }
        axeTranches.setTickLabelFormatter(new NumberAxis.DefaultFormatter(axeTranches) {
            @Override
            public String toString(Number valeur) {
                return valeur.longValue() / 1000 + " k";
            }
        });
    }

    private static String styleTranche(TrancheAge tranche) {
        return switch (tranche) {
            case NON_ECHUE -> "non-echue";
            case J1_A_30 -> "j1-a-30";
            case J31_A_60 -> "j31-a-60";
            case J61_A_90 -> "j61-a-90";
            case PLUS_DE_90 -> "plus-de-90";
        };
    }

    /** Chart nodes are created lazily by JavaFX: run the action as soon as the node exists. */
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

    // ------------------------------------------------------------------ CONTEXT MENU & DIALOGS

    private TableRow<ClientARelancer> ligneAvecMenu() {
        TableRow<ClientARelancer> ligne = new TableRow<>();
        MenuItem voir = new MenuItem("Voir les factures du client");
        MenuItem payer = new MenuItem("Enregistrer un paiement…");
        voir.setOnAction(e -> voirFactures(ligne.getItem()));
        payer.setOnAction(e -> enregistrerPaiement(ligne.getItem()));
        ContextMenu menu = new ContextMenu(voir, payer);
        menu.setOnShowing(e -> payer.setVisible(!ComptableUi.lectureSeule()));
        ligne.emptyProperty().addListener((obs, avant, vide) -> ligne.setContextMenu(vide ? null : menu));
        return ligne;
    }

    /** Opens the invoice register with this client preselected (one-shot filter in the comptable controller). */
    private void voirFactures(ClientARelancer client) {
        if (client == null || client.clientId() == null) {
            return;
        }
        FacturesController.preparerFiltreClient(client.clientId());
        Navigator.ouvrir("comptable/factures.fxml");
    }

    /** Records a payment on one of the client's real overdue invoices, using the existing dialog. */
    private void enregistrerPaiement(ClientARelancer client) {
        if (client == null || client.clientId() == null || ComptableUi.lectureSeule()) {
            return;
        }
        List<Facture> enRetard = factures.rechercher(new CritereFacture(null, client.clientId(), "En retard",
                null, null, null, null));
        if (enRetard.isEmpty()) {
            Dialogues.info("Aucune facture en retard",
                    "Ce client n'a plus de facture échue à régler.");
            rafraichir();
            return;
        }
        Facture facture = enRetard.size() == 1 ? enRetard.get(0) : choisirFacture(client, enRetard);
        if (facture == null) {
            return;
        }
        if (!Dialogues.confirmer("Enregistrer un règlement",
                "Vous allez enregistrer le règlement de la facture " + facture.getNumero() + " ("
                        + Formats.mad(facture.getMontantTtc()) + ").\nCette opération est définitive. Continuer ?")) {
            return;
        }
        if (ComptableUi.<PaiementFormulaireController>ouvrirFormulaire(table, PaiementFormulaireController.FXML,
                f -> f.preparer(facture, client.nomClient()))) {
            rafraichir();
        }
    }

    private static Facture choisirFacture(ClientARelancer client, List<Facture> enRetard) {
        Map<String, Facture> facturesParLibelle = new LinkedHashMap<>();
        for (Facture facture : enRetard) {
            facturesParLibelle.put(facture.getNumero() + " · échéance "
                    + Formats.date(facture.getDateEcheance()) + " · " + Formats.mad(facture.getMontantTtc()), facture);
        }
        ChoiceDialog<String> dialogue = new ChoiceDialog<>(facturesParLibelle.keySet().iterator().next(),
                facturesParLibelle.keySet());
        dialogue.setTitle("Choisir une facture");
        dialogue.setHeaderText("Client : " + client.nomClient());
        dialogue.setContentText("Facture à régler :");
        return dialogue.showAndWait().map(facturesParLibelle::get).orElse(null);
    }
}
