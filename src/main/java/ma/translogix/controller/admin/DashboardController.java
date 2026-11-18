package ma.translogix.controller.admin;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectProperty;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import ma.translogix.AppContext;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.ResultatAudit;
import ma.translogix.service.AuditService;
import ma.translogix.service.DashboardService;
import ma.translogix.util.Navigator;
import ma.translogix.util.Session;

/**
 * Gérant (CEO) dashboard: money, fleet, risk. Layout from admine.html.
 * KPI cards and "Trajets récents" are static DEMO values from the mockup. Charts, alerts and
 * "Dernières actions" come from DashboardService (demo figures labelled as such; audit events are real).
 */
public class DashboardController {

    /** One row of the "Trajets recents" table. */
    public record LigneTrajet(String reference, String chauffeur, String camion,
                              String depart, String arrivee, String statut, String marge) {
    }

    private static final List<LigneTrajet> DEMO = List.of(
            new LigneTrajet("TRJ-9081", "Mohammed Bennani", "12345-A-6 · Volvo",
                    "Casablanca", "Tanger", "En cours", "8 400 MAD"),
            new LigneTrajet("TRJ-9082", "Abderrahim El Amrani", "67890-B-2 · Scania",
                    "Agadir", "Marrakech", "Terminé", "12 100 MAD"),
            new LigneTrajet("TRJ-9083", "Youssef Amrani", "45678-C-1 · MAN",
                    "Rabat", "Fès", "Planifié", "6 700 MAD"));

    private static final Map<String, String> BADGES = Map.of(
            "En cours", "badge-blue",
            "Terminé", "badge-green",
            "Planifié", "badge-amber");

    private static final int NB_MOIS = 6;
    private static final int NB_EVENEMENTS = 5;
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMM yy", Locale.FRENCH);
    private static final DateTimeFormatter MOIS_LONG = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    /** Bar colours of "État de la flotte", in the order of DashboardService.etatFlotte(). */
    private static final List<String> CLASSES_FLOTTE =
            List.of("flotte-mission", "flotte-disponible", "flotte-maintenance");

    /** Per alert level: row style, icon style, icon drawing. */
    private static final Map<DashboardService.Niveau, String[]> STYLES_ALERTE = Map.of(
            DashboardService.Niveau.CRITIQUE, new String[] {"alert-red", "svg-red",
                "M2 5h20v14H2z M5 8h6v8H5z M13 9h6v1.5h-6z M13 12h6v1.5h-6z"},
            DashboardService.Niveau.ATTENTION, new String[] {"alert-amber", "svg-amber",
                "M5 2h10l5 5v15H5z M8 10h9v2H8z M8 14h9v2H8z M8 18h6v1.5H8z"},
            DashboardService.Niveau.INFO, new String[] {"alert-sky", "svg-sky",
                "M14.5 2.5a5 5 0 0 0-4.8 6.4L2.5 16.1l3.4 3.4l7.2-7.2a5 5 0 0 0 6.4-4.8l-2.9 2.9l-2.8-0.7l-0.7-2.8z"});

    private final DashboardService service = AppContext.dashboardService();

    @FXML private Label salutation;
    @FXML private StackPane kpiChiffre;
    @FXML private StackPane kpiFlotte;
    @FXML private StackPane kpiCreances;
    @FXML private StackPane kpiConformite;
    @FXML private Label chipCa;
    @FXML private Label chipMissions;
    @FXML private Label chipFlotte;
    @FXML private Label chipAlertes;
    @FXML private Label sousTitreCa;
    @FXML private Label sousTitreMissions;
    @FXML private Label sousTitreFlotte;
    @FXML private Label totalCa;
    @FXML private Label nombreAlertes;
    @FXML private BarChart<String, Number> graphiqueCa;
    @FXML private NumberAxis axeCa;
    @FXML private PieChart graphiqueMissions;
    @FXML private BarChart<Number, String> graphiqueFlotte;
    @FXML private NumberAxis axeFlotte;
    @FXML private VBox listeAlertes;
    @FXML private VBox listeAudit;
    @FXML private TableView<LigneTrajet> tableTrajets;
    @FXML private TableColumn<LigneTrajet, String> colReference;
    @FXML private TableColumn<LigneTrajet, String> colChauffeur;
    @FXML private TableColumn<LigneTrajet, String> colTrajet;
    @FXML private TableColumn<LigneTrajet, String> colStatut;
    @FXML private TableColumn<LigneTrajet, String> colMarge;

    @FXML
    private void initialize() {
        salutation.setText("Bonjour, " + nomAffiche());
        for (Region carte : List.of(kpiChiffre, kpiFlotte, kpiCreances, kpiConformite)) {
            arrondir(carte);
        }

        colReference.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reference()));
        colReference.setCellFactory(col -> celluleTexte("cell-strong"));

        colChauffeur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().chauffeur()));
        colChauffeur.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                LigneTrajet ligne = empty || getTableRow() == null ? null : getTableRow().getItem();
                setText(null);
                if (ligne == null) {
                    setGraphic(null);
                    return;
                }
                Label nom = new Label(ligne.chauffeur());
                nom.getStyleClass().add("cell-strong");
                Label camion = new Label(ligne.camion());
                camion.getStyleClass().add("cell-sub");
                VBox bloc = new VBox(2, nom, camion);
                bloc.setAlignment(Pos.CENTER_LEFT);
                setGraphic(bloc);
            }
        });

        colTrajet.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().depart() + "  →  " + c.getValue().arrivee()));
        colTrajet.setCellFactory(col -> celluleTexte("cell-normal"));

        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().statut()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                setText(null);
                if (empty || statut == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label("● " + statut);
                badge.getStyleClass().add(BADGES.getOrDefault(statut, "badge-gray"));
                setGraphic(badge);
            }
        });

        colMarge.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().marge()));
        colMarge.setCellFactory(col -> celluleTexte("cell-strong"));

        tableTrajets.setItems(FXCollections.observableArrayList(DEMO));

        marquerDemo();
        remplirChiffreAffaires();
        remplirMissions();
        remplirFlotte();
        remplirAlertes();
        remplirAudit();
    }

    // Targets owned by other members. Until their FXML exists, Navigator shows "Module pas encore livré".

    /** "+ Nouvel ordre de transport". */
    @FXML
    private void ouvrirDispatching() {
        Navigator.ouvrir("dispatcher/missions.fxml");
    }

    /** "Voir la flotte" (the alert "Maintenance flotte" opens the same page through DashboardService). */
    @FXML
    private void ouvrirFlotte() {
        Navigator.ouvrir("dispatcher/flotte.fxml");
    }

    /** "Voir le journal" in "Dernières actions". */
    @FXML
    private void ouvrirJournal() {
        Navigator.ouvrir("admin/audit.fxml");
    }

    // ------------------------------------------------------------------ MONEY

    private void remplirChiffreAffaires() {
        List<DashboardService.ChiffreMensuel> mois = service.chiffreAffairesParMois(NB_MOIS);
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        graphiqueCa.getData().add(serie);
        long total = 0;
        for (int i = 0; i < mois.size(); i++) {
            DashboardService.ChiffreMensuel m = mois.get(i);
            total += m.montantMad();
            XYChart.Data<String, Number> point = new XYChart.Data<>(m.mois().format(MOIS), m.montantMad());
            serie.getData().add(point);
            boolean courant = i == mois.size() - 1;
            String detail = m.mois().format(MOIS_LONG) + " : " + mad(m.montantMad());
            quandAffiche(point.nodeProperty(), noeud -> {
                Tooltip.install(noeud, new Tooltip(detail));
                if (courant) {
                    noeud.getStyleClass().add("barre-courante"); // current month in navy
                }
            });
        }
        axeCa.setTickLabelFormatter(new NumberAxis.DefaultFormatter(axeCa) {
            @Override
            public String toString(Number valeur) {
                return valeur.longValue() / 1000 + " k";
            }
        });
        totalCa.setText("Total : " + mad(total));
    }

    private void remplirMissions() {
        List<DashboardService.Part> parts = service.missionsParStatut();
        int total = parts.stream().mapToInt(DashboardService.Part::nombre).sum();
        for (DashboardService.Part part : parts) {
            PieChart.Data tranche = new PieChart.Data(part.libelle() + " (" + part.nombre() + ")", part.nombre());
            graphiqueMissions.getData().add(tranche);
            long pourcent = total == 0 ? 0 : Math.round(part.nombre() * 100.0 / total);
            String detail = part.libelle() + " : " + part.nombre() + " mission(s), " + pourcent + " %";
            quandAffiche(tranche.nodeProperty(), noeud -> Tooltip.install(noeud, new Tooltip(detail)));
        }
        sousTitreMissions.setText(total + " missions · cycle de vie" + suffixeDemo());
    }

    // ------------------------------------------------------------------ FLEET

    private void remplirFlotte() {
        List<DashboardService.Part> etats = service.etatFlotte();
        XYChart.Series<Number, String> serie = new XYChart.Series<>();
        graphiqueFlotte.getData().add(serie);
        int total = etats.stream().mapToInt(DashboardService.Part::nombre).sum();
        int max = etats.stream().mapToInt(DashboardService.Part::nombre).max().orElse(0);
        // Whole vehicles only: integer ticks, a little headroom after the longest bar.
        double unite = max <= 12 ? 2 : Math.ceil(max / 6.0);
        axeFlotte.setTickUnit(unite);
        axeFlotte.setUpperBound(Math.max(unite, Math.ceil((max + 1) / unite) * unite));
        axeFlotte.setTickLabelFormatter(new NumberAxis.DefaultFormatter(axeFlotte) {
            @Override
            public String toString(Number valeur) {
                return String.valueOf(valeur.intValue());
            }
        });
        // A vertical category axis lists from the bottom up: add in reverse so the first state is on top.
        for (int i = etats.size() - 1; i >= 0; i--) {
            DashboardService.Part etat = etats.get(i);
            XYChart.Data<Number, String> barre = new XYChart.Data<>(etat.nombre(), etat.libelle());
            serie.getData().add(barre);
            String classe = i < CLASSES_FLOTTE.size() ? CLASSES_FLOTTE.get(i) : "flotte-autre";
            String detail = etat.libelle() + " : " + etat.nombre() + " véhicule(s)";
            quandAffiche(barre.nodeProperty(), noeud -> {
                noeud.getStyleClass().add(classe);
                Tooltip.install(noeud, new Tooltip(detail));
            });
        }
        sousTitreFlotte.setText(total + " véhicules" + suffixeDemo());
    }

    // ------------------------------------------------------------------ RISK

    private void remplirAlertes() {
        List<DashboardService.Alerte> alertes = service.alertesConformite();
        nombreAlertes.setText(alertes.size() + (alertes.size() > 1 ? " alertes" : " alerte"));
        for (DashboardService.Alerte alerte : alertes) {
            String[] style = STYLES_ALERTE.get(alerte.niveau());
            SVGPath icone = new SVGPath();
            icone.setContent(style[2]);
            icone.setFillRule(FillRule.EVEN_ODD);
            icone.getStyleClass().add(style[1]);
            StackPane cadre = new StackPane(icone);
            cadre.getStyleClass().add("alert-icon");

            Label titre = new Label(alerte.titre());
            titre.getStyleClass().add("alert-title");
            Label detail = new Label(alerte.detail());
            detail.setWrapText(true);
            detail.getStyleClass().add("alert-text");
            VBox textes = new VBox(3, titre, detail);
            HBox.setHgrow(textes, Priority.ALWAYS);
            Label chevron = new Label("›");
            chevron.getStyleClass().add("chevron");

            HBox ligne = new HBox(12, cadre, textes, chevron);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getStyleClass().add(style[0]);
            ligne.setOnMouseClicked(e -> Navigator.ouvrir(alerte.cibleFxml()));
            listeAlertes.getChildren().add(ligne);
        }
    }

    private void remplirAudit() {
        List<EvenementAudit> evenements = service.derniersEvenementsAudit(NB_EVENEMENTS);
        if (evenements.isEmpty()) {
            Label vide = new Label("Aucune action enregistrée pour cette session.");
            vide.getStyleClass().add("empty-placeholder");
            listeAudit.getChildren().add(vide);
            return;
        }
        for (EvenementAudit e : evenements) {
            Label action = new Label(e.getAction());
            action.setWrapText(true);
            action.getStyleClass().add("audit-action");
            Label qui = new Label(e.getHorodatage().format(AuditService.FORMAT_DATE) + " · " + e.getUtilisateur());
            qui.getStyleClass().add("audit-meta");
            VBox textes = new VBox(3, action, qui);
            HBox.setHgrow(textes, Priority.ALWAYS);
            Label badge = new Label("● " + e.getResultat().getLibelle());
            badge.getStyleClass().add(e.getResultat() == ResultatAudit.REUSSI ? "badge-green" : "badge-red");
            badge.setMinWidth(Region.USE_PREF_SIZE);
            HBox ligne = new HBox(10, textes, badge);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getStyleClass().add("audit-row");
            listeAudit.getChildren().add(ligne);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Demo chips and subtitles follow DashboardService.estDemo(), so a real service removes them. */
    private void marquerDemo() {
        boolean demo = service.estDemo();
        for (Label chip : List.of(chipCa, chipMissions, chipFlotte, chipAlertes)) {
            chip.setVisible(demo);
            chip.setManaged(demo);
        }
        sousTitreCa.setText(NB_MOIS + " derniers mois, en MAD" + suffixeDemo());
    }

    private String suffixeDemo() {
        return service.estDemo() ? " · Données de démonstration" : "";
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

    private static String mad(long montant) {
        return String.format(Locale.ROOT, "%,d", montant).replace(',', ' ') + " MAD";
    }

    /** Rounded clip so the tinted circle stays inside the card, like overflow:hidden in the mockup. */
    private static void arrondir(Region carte) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(carte.widthProperty());
        clip.heightProperty().bind(carte.heightProperty());
        carte.setClip(clip);
    }

    private static TableCell<LigneTrajet, String> celluleTexte(String styleClass) {
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

    private static String nomAffiche() {
        return Session.estConnecte() ? Session.utilisateur().getNomComplet() : "M. Bennani";
    }
}
