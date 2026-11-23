package ma.translogix.controller.comptable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Facture;
import ma.translogix.model.StatutFacture;
import ma.translogix.service.ClientService;
import ma.translogix.service.FactureService;
import ma.translogix.util.Formats;
import ma.translogix.util.Navigator;
import ma.translogix.util.Session;

/**
 * Comptable dashboard. KPI 1-3 and "Dernières factures émises" come from FactureService (real, in memory);
 * "Reçus gazole" is a static DÉMO card (expenses arrive in Iteration 3).
 */
public class DashboardController {

    private static final int NB_DERNIERES = 5;

    private static final String PERIODE_6 = "6 derniers mois";
    private static final String PERIODE_12 = "12 derniers mois";
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMM yy", Locale.FRENCH);
    private static final DateTimeFormatter MOIS_LONG = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    @FXML private Label salutation;
    @FXML private Button boutonCreer;
    @FXML private StackPane kpiChiffre;
    @FXML private StackPane kpiEncaissements;
    @FXML private StackPane kpiCreances;
    @FXML private StackPane kpiRecus;
    @FXML private Label valeurChiffre;
    @FXML private Label piedChiffre;
    @FXML private Label valeurEncaissements;
    @FXML private Label piedEncaissements;
    @FXML private Label valeurCreances;
    @FXML private Label piedCreances;
    @FXML private TableView<Facture> tableFactures;
    @FXML private TableColumn<Facture, String> colNumero;
    @FXML private TableColumn<Facture, String> colClient;
    @FXML private TableColumn<Facture, String> colEmission;
    @FXML private TableColumn<Facture, String> colTtc;
    @FXML private TableColumn<Facture, String> colStatut;
    @FXML private TableColumn<Facture, Facture> colAction;
    @FXML private VBox listeActions;
    @FXML private ComboBox<String> periodeCa;
    @FXML private Label totalCa;
    @FXML private Label sousTitreCa;
    @FXML private Label sousTitreStatuts;
    @FXML private BarChart<String, Number> graphiqueCa;
    @FXML private NumberAxis axeCa;
    @FXML private PieChart graphiqueStatuts;

    private final FactureService factures = AppContext.factureService();
    private final ClientService clients = AppContext.clientService();
    private final Map<Long, String> nomsClients = new HashMap<>();

    @FXML
    private void initialize() {
        salutation.setText(Session.estConnecte() ? "Bonjour, " + Session.utilisateur().getNomComplet() : "Bonjour");
        for (Region carte : List.of(kpiChiffre, kpiEncaissements, kpiCreances, kpiRecus)) {
            ComptableUi.arrondir(carte);
        }

        colNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumero()));
        colNumero.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colClient.setCellValueFactory(c -> new SimpleStringProperty(
                nomsClients.getOrDefault(c.getValue().getClientId(), "—")));
        colClient.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colEmission.setCellValueFactory(c -> new SimpleStringProperty(Formats.date(c.getValue().getDateEmission())));
        colEmission.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colTtc.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getMontantTtc())));
        colTtc.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(factures.libelleStatut(c.getValue())));
        colStatut.setCellFactory(col -> ComptableUi.badge(ComptableUi::classeStatut));
        colAction.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colAction.setCellFactory(col -> new TableCell<>() {
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
                boolean reglable = !empty && f != null && f.getStatut() == StatutFacture.EN_ATTENTE
                        && !ComptableUi.lectureSeule();
                setGraphic(reglable ? regler : null);
            }
        });

        if (ComptableUi.lectureSeule()) {
            ComptableUi.masquer(boutonCreer);
        }
        preparerCharts();
        rafraichir();
    }

    private void preparerCharts() {
        periodeCa.setItems(FXCollections.observableArrayList(PERIODE_6, PERIODE_12));
        periodeCa.setValue(PERIODE_6);
        periodeCa.valueProperty().addListener((obs, ancien, periode) -> remplirCharts());
    }

    /** "Créer une facture" (header) and the quick action "Émettre une facture transport". */
    @FXML
    private void creerFacture() {
        if (ComptableUi.<FactureFormulaireController>ouvrirFormulaire(tableFactures, FactureFormulaireController.FXML,
                FactureFormulaireController::preparer)) {
            rafraichir();
        }
    }

    @FXML
    private void voirFactures() {
        Navigator.ouvrir("comptable/factures.fxml");
    }

    private void regler(Facture facture) {
        if (facture == null || ComptableUi.lectureSeule()) {
            return;
        }
        if (!Dialogues.confirmer("Enregistrer un règlement",
                "Vous allez enregistrer le règlement de la facture " + facture.getNumero() + " ("
                        + Formats.mad(facture.getMontantTtc()) + ").\nCette opération est définitive. Continuer ?")) {
            return;
        }
        if (ComptableUi.<PaiementFormulaireController>ouvrirFormulaire(tableFactures,
                PaiementFormulaireController.FXML,
                f -> f.preparer(facture, nomsClients.getOrDefault(facture.getClientId(), "—")))) {
            rafraichir();
        }
    }

    private void rafraichir() {
        nomsClients.clear();
        clients.lister().forEach(c -> nomsClients.put(c.getId(), c.getNom()));

        FactureService.Synthese s = factures.synthese();
        valeurChiffre.setText(Formats.montant(s.chiffreAffairesMoisHT()));
        piedChiffre.setText(s.facturesDuMois() + (s.facturesDuMois() > 1 ? " factures émises" : " facture émise")
                + " ce mois");
        valeurEncaissements.setText(Formats.montant(s.encaissementsMois()));
        piedEncaissements.setText(pourcentage(s.encaissementsMois(), s.factureMoisTtc()) + " du facturé TTC du mois");
        valeurCreances.setText(Formats.montant(s.creancesEnRetard()));
        piedCreances.setText("● " + s.facturesEnRetard()
                + (s.facturesEnRetard() > 1 ? " factures à relancer" : " facture à relancer"));

        tableFactures.setItems(FXCollections.observableArrayList(factures.dernieres(NB_DERNIERES)));
        remplirActions(s);
        remplirCharts();
    }

    // ------------------------------------------------------------------ CHARTS

    /** Rebuilds both charts from the current stored invoices (also used after a payment or a creation). */
    private void remplirCharts() {
        remplirStatuts();
        remplirChiffreAffaires();
    }

    private void remplirStatuts() {
        List<FactureService.Part> parts = factures.facturesParStatut();
        long total = parts.stream().mapToLong(FactureService.Part::nombre).sum();
        graphiqueStatuts.getData().clear();
        if (total == 0) {
            sousTitreStatuts.setText("Aucune facture enregistrée");
            return; // an all-zero pie would only draw an empty circle
        }
        for (FactureService.Part part : parts) {
            PieChart.Data tranche = new PieChart.Data(part.libelle() + " (" + part.nombre() + ")",
                    part.nombre());
            graphiqueStatuts.getData().add(tranche);
            long pourcent = Math.round(part.nombre() * 100.0 / total);
            quandAffiche(tranche.nodeProperty(), noeud -> Tooltip.install(noeud,
                    new Tooltip(part.libelle() + " : " + part.nombre() + " facture(s), " + pourcent + " %")));
        }
        sousTitreStatuts.setText(total + (total > 1 ? " factures" : " facture") + " · répartition");
    }

    private void remplirChiffreAffaires() {
        int nbMois = PERIODE_12.equals(periodeCa.getValue()) ? 12 : 6;
        List<FactureService.ChiffreMensuel> mois = factures.chiffreAffairesParMois(nbMois);
        graphiqueCa.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        graphiqueCa.getData().add(serie);

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < mois.size(); i++) {
            FactureService.ChiffreMensuel m = mois.get(i);
            total = total.add(m.montantHt());
            XYChart.Data<String, Number> point =
                    new XYChart.Data<>(m.mois().format(MOIS), m.montantHt());
            serie.getData().add(point);
            boolean courant = i == mois.size() - 1;
            quandAffiche(point.nodeProperty(), noeud -> {
                Tooltip.install(noeud, new Tooltip(m.mois().format(MOIS_LONG) + " : "
                        + Formats.mad(m.montantHt())));
                if (courant) {
                    noeud.getStyleClass().add("barre-courante"); // same navy as the Gérant's dashboard
                }
            });
        }
        axeCa.setTickLabelFormatter(new NumberAxis.DefaultFormatter(axeCa) {
            @Override
            public String toString(Number valeur) {
                return valeur.longValue() / 1000 + " k";
            }
        });
        totalCa.setText("Total : " + Formats.mad(total));
        sousTitreCa.setText(nbMois + " derniers mois, en MAD HT");
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

    private void remplirActions(FactureService.Synthese s) {
        listeActions.getChildren().clear();
        listeActions.getChildren().add(action("alert-amber", "svg-amber",
                "M4 2h16v20l-2.7-1.8L14.7 22L12 20.2L9.3 22l-2.6-1.8L4 22z M7 7h10v2H7z M7 11h10v2H7z",
                "Validation des frais de route", "3 reçus gazole en attente d'approbation (démo).",
                () -> Navigator.ouvrir("comptable/depenses.fxml")));
        listeActions.getChildren().add(action("alert-red", "svg-red",
                "M5 2h10l5 5v15H5z M8 10h9v2H8z M8 14h9v2H8z M8 18h6v1.5H8z",
                "Plan de recouvrement clients", Formats.mad(s.creancesEnRetard()) + " de créances échues.",
                () -> Navigator.ouvrir("comptable/recouvrement.fxml")));
        if (!ComptableUi.lectureSeule()) {
            listeActions.getChildren().add(action("alert-sky", "svg-sky",
                    "M11 4h2v7h7v2h-7v7h-2v-7H4v-2h7z",
                    "Émettre une facture transport", "Saisie rapide avec calcul automatique TVA 14 %.",
                    this::creerFacture));
        }
    }

    /** One clickable row of "Actions financières rapides" (same look as the Gérant's alerts). */
    private static HBox action(String styleLigne, String styleIcone, String dessin, String titre, String detail,
                               Runnable auClic) {
        SVGPath icone = new SVGPath();
        icone.setContent(dessin);
        icone.setFillRule(FillRule.EVEN_ODD);
        icone.getStyleClass().add(styleIcone);
        StackPane cadre = new StackPane(icone);
        cadre.getStyleClass().add("alert-icon");

        Label libelle = new Label(titre);
        libelle.getStyleClass().add("alert-title");
        Label texte = new Label(detail);
        texte.setWrapText(true);
        texte.getStyleClass().add("alert-text");
        VBox textes = new VBox(3, libelle, texte);
        HBox.setHgrow(textes, Priority.ALWAYS);
        Label chevron = new Label("›");
        chevron.getStyleClass().add("chevron");

        HBox ligne = new HBox(12, cadre, textes, chevron);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.getStyleClass().add(styleLigne);
        ligne.setOnMouseClicked(e -> auClic.run());
        return ligne;
    }

    private static String pourcentage(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return "0 %";
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP) + " %";
    }
}
