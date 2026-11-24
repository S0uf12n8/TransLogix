package ma.translogix.controller.rh;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import ma.translogix.AppContext;
import ma.translogix.controller.common.ExportRapport;
import ma.translogix.controller.common.ImpressionRapport;
import ma.translogix.controller.common.Imprimable;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.Employe;
import ma.translogix.model.StatutConformite;
import ma.translogix.model.TypeDocument;
import ma.translogix.service.ConformiteService;
import ma.translogix.service.CongeService;
import ma.translogix.service.EmployeService;
import ma.translogix.service.FiltreRapport;
import ma.translogix.service.Rapport;
import ma.translogix.service.TypeRapport;
import ma.translogix.util.Navigator;
import ma.translogix.util.Session;

/**
 * "Tableau de bord RH". Every KPI and chart comes from the RH services (real, in memory).
 */
public class DashboardController implements Imprimable {

    @FXML private Label salutation;
    @FXML private StackPane kpiEffectif;
    @FXML private StackPane kpiChauffeurs;
    @FXML private StackPane kpiAlertes;
    @FXML private StackPane kpiConges;
    @FXML private Label valeurEffectif;
    @FXML private Label piedEffectif;
    @FXML private Label valeurChauffeurs;
    @FXML private Label totalChauffeurs;
    @FXML private ProgressBar progressionChauffeurs;
    @FXML private Label piedChauffeurs;
    @FXML private Label valeurAlertes;
    @FXML private Label piedAlertes;
    @FXML private Label valeurConges;
    @FXML private Label piedConges;
    @FXML private TableView<Employe> tableChauffeurs;
    @FXML private TableColumn<Employe, Employe> colEmploye;
    @FXML private TableColumn<Employe, String> colPoste;
    @FXML private TableColumn<Employe, Employe> colPermis;
    @FXML private TableColumn<Employe, String> colVisite;
    @FXML private TableColumn<Employe, String> colStatut;
    @FXML private HBox actionRenouveler;
    @FXML private PieChart graphiquePostes;
    @FXML private Label totalPostes;
    @FXML private BarChart<String, Number> graphiqueConformite;
    @FXML private NumberAxis axeConformite;
    @FXML private Label totalConformite;

    private final EmployeService employes = AppContext.employeService();
    private final ConformiteService conformite = AppContext.conformiteService();
    private final CongeService conges = AppContext.congeService();

    @FXML
    private void initialize() {
        salutation.setText(Session.estConnecte() ? "Bienvenue, " + Session.utilisateur().getNomComplet() : "Bienvenue");
        for (Region carte : List.of(kpiEffectif, kpiChauffeurs, kpiAlertes, kpiConges)) {
            RhUi.arrondir(carte);
        }

        colEmploye.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colEmploye.setCellFactory(col -> RhUi.doubleLigne(Employe::getNomComplet, Employe::getMatricule));
        colPoste.setCellValueFactory(c -> new SimpleStringProperty(poste(c.getValue())));
        colPoste.setCellFactory(col -> RhUi.texte("cell-normal"));
        colPermis.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colPermis.setCellFactory(col -> RhUi.doubleLigne(this::numeroPermis, this::echeancesPermisFimc));
        colVisite.setCellValueFactory(c -> new SimpleStringProperty(visite(c.getValue())));
        colVisite.setCellFactory(col -> RhUi.texteColore(e -> document(e, TypeDocument.VISITE_MEDICALE)
                .map(d -> RhUi.classeTexte(conformite.statut(d))).orElse("cell-normal")));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(conformite.statutGlobal(c.getValue().getId())
                .map(StatutConformite::getLibelle).orElse("Non renseigné")));
        colStatut.setCellFactory(col -> RhUi.badge(RhUi::classeBadge));

        if (RhUi.lectureSeule()) {
            RhUi.masquer(actionRenouveler);
        }
        rafraichir();
    }

    /** "Exporter Rapport RH": print / PDF or CSV of the compliance documents to handle. */
    @FXML
    private void exporter() {
        ExportRapport.proposer(documentAImprimer(), tableChauffeurs.getScene().getWindow());
    }

    /**
     * RH report: expired documents and documents expiring within 30 days, most urgent first, with the headcount
     * (real data, through RapportService and EmployeService). Also used by the top-bar "Imprimer".
     */
    @Override
    public ImpressionRapport.Document documentAImprimer() {
        Rapport documents = AppContext.rapportService().generer(TypeRapport.CONFORMITE, FiltreRapport.TOUT);
        EmployeService.Effectifs effectifs = employes.effectifs();
        Rapport rapport = new Rapport("Rapport RH — conformité des documents",
                "rapport-rh-conformite-" + LocalDate.now() + ".csv", documents.entetes(), documents.lignes(),
                documents.resume());
        return new ImpressionRapport.Document(rapport, List.of(
                "Effectif actif : " + effectifs.actifs() + " dont " + effectifs.chauffeursActifs() + " chauffeur(s).",
                "Documents expirés ou expirant dans les "
                        + ConformiteService.SEUIL_RENOUVELLEMENT_JOURS + " prochains jours, du plus urgent au moins urgent."),
                false);
    }

    @FXML
    private void voirAnnuaire() {
        Navigator.ouvrir("rh/employes.fxml");
    }

    @FXML
    private void saisirConge() {
        Navigator.ouvrir("rh/conges.fxml");
    }

    @FXML
    private void renouvelerDocument() {
        if (RhUi.<DocumentFormulaireController>ouvrirFormulaire(tableChauffeurs, DocumentFormulaireController.FXML,
                f -> f.preparer(null, null))) {
            rafraichir();
        }
    }

    /** As in rh.html: the quick action opens the "Paie & Prime Trajet" tab (a DÉMO page until Iteration 3). */
    @FXML
    private void ouvrirPrimes() {
        Navigator.ouvrir("rh/primes.fxml");
    }

    private void rafraichir() {
        EmployeService.Effectifs e = employes.effectifs();
        valeurEffectif.setText(String.valueOf(e.actifs()));
        int recrues = e.recrutementsDuMois();
        piedEffectif.setText(recrues == 0 ? "Aucun recrutement ce mois"
                : "+" + recrues + (recrues > 1 ? " recrutements" : " recrutement") + " ce mois");
        piedEffectif.getStyleClass().setAll("label", recrues == 0 ? "foot-muted" : "foot-green");

        valeurChauffeurs.setText(String.valueOf(e.chauffeursActifs()));
        totalChauffeurs.setText("/ " + e.chauffeurs());
        progressionChauffeurs.setProgress(e.chauffeurs() == 0 ? 0 : (double) e.chauffeursActifs() / e.chauffeurs());
        int inactifs = e.chauffeurs() - e.chauffeursActifs();
        piedChauffeurs.setText(inactifs == 0 ? "Tous les chauffeurs sont actifs"
                : inactifs + (inactifs > 1 ? " chauffeurs désactivés" : " chauffeur désactivé"));

        List<DocumentConformite> alertes = conformite.alertes();
        valeurAlertes.setText(String.valueOf(alertes.size()));
        piedAlertes.setText(alertes.isEmpty() ? "Aucune échéance sous 30 jours" : detailAlertes(alertes));
        piedAlertes.getStyleClass().setAll("label", alertes.isEmpty() ? "foot-green" : "foot-red");

        int demandesAttente = conges.aValider();
        valeurConges.setText(String.valueOf(demandesAttente));
        piedConges.setText(demandesAttente == 0 ? "Aucune demande à traiter"
                : demandesAttente + (demandesAttente > 1 ? " demandes à traiter" : " demande à traiter"));
        piedConges.getStyleClass().setAll("label", demandesAttente == 0 ? "foot-green" : "foot-amber");

        tableChauffeurs.setItems(FXCollections.observableArrayList(employes.listerChauffeurs()));
        tableChauffeurs.refresh();
        remplirGraphiques();
    }

    /** Pie and bar charts fed only by the two services; no figure is written in the FXML or here. */
    private void remplirGraphiques() {
        List<EmployeService.Part> postes = employes.employesParPoste();
        long totalSalaries = postes.stream().mapToLong(EmployeService.Part::nombre).sum();
        graphiquePostes.getData().clear();
        for (EmployeService.Part part : postes) {
            PieChart.Data tranche = new PieChart.Data(
                    part.libelle() + " (" + part.nombre() + ")", part.nombre());
            graphiquePostes.getData().add(tranche);
            long pourcent = totalSalaries == 0 ? 0 : Math.round(part.nombre() * 100.0 / totalSalaries);
            String detail = part.libelle() + " : " + part.nombre()
                    + (part.nombre() > 1 ? " salariés" : " salarié") + ", " + pourcent + " %";
            quandAffiche(tranche.nodeProperty(), noeud -> Tooltip.install(noeud, new Tooltip(detail)));
        }
        totalPostes.setText("Total : " + totalSalaries);

        List<ConformiteService.Part> statuts = conformite.documentsParStatut();
        long totalDocuments = statuts.stream().mapToLong(ConformiteService.Part::nombre).sum();
        graphiqueConformite.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        graphiqueConformite.getData().add(serie);
        long max = statuts.stream().mapToLong(ConformiteService.Part::nombre).max().orElse(0);
        double unite = max <= 12 ? 1 : Math.ceil(max / 6.0);
        axeConformite.setTickUnit(unite);
        axeConformite.setUpperBound(Math.max(unite, Math.ceil((max + 1) / unite) * unite));
        axeConformite.setTickLabelFormatter(new NumberAxis.DefaultFormatter(axeConformite) {
            @Override
            public String toString(Number valeur) {
                return String.valueOf(valeur.intValue());
            }
        });
        for (ConformiteService.Part part : statuts) {
            XYChart.Data<String, Number> barre = new XYChart.Data<>(part.libelle(), part.nombre());
            serie.getData().add(barre);
            String detail = part.libelle() + " : " + part.nombre()
                    + (part.nombre() > 1 ? " documents" : " document");
            quandAffiche(barre.nodeProperty(), noeud -> Tooltip.install(noeud, new Tooltip(detail)));
        }
        totalConformite.setText("Total : " + totalDocuments);
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

    /** "2 Permis + 1 Visite médicale", in the TypeDocument order. */
    private static String detailAlertes(List<DocumentConformite> alertes) {
        Map<TypeDocument, Long> parType = alertes.stream()
                .collect(Collectors.groupingBy(DocumentConformite::getType, Collectors.counting()));
        return java.util.Arrays.stream(TypeDocument.values())
                .filter(parType::containsKey)
                .map(t -> parType.get(t) + " " + court(t))
                .collect(Collectors.joining(" + "));
    }

    private static String court(TypeDocument type) {
        return switch (type) {
            case PERMIS -> "Permis";
            case FIMC -> "FIMC";
            case VISITE_MEDICALE -> "Visite médicale";
        };
    }

    private Optional<DocumentConformite> document(Employe e, TypeDocument type) {
        return conformite.document(e.getId(), type);
    }

    /** "Chauffeur (EC)": the poste and the licence category. */
    private String poste(Employe e) {
        return e.getPoste().getLibelle() + document(e, TypeDocument.PERMIS)
                .map(DocumentConformite::getCategorie).map(c -> " (" + c + ")").orElse("");
    }

    /** First line of "Permis / FIMC": the licence number. */
    private String numeroPermis(Employe e) {
        return document(e, TypeDocument.PERMIS).map(d -> RhUi.tiret(d.getNumero())).orElse("Non renseigné");
    }

    /** Second line of "Permis / FIMC": "Exp. 10/2028", plus "· FIMC 12/2026" when the FIMC card needs attention. */
    private String echeancesPermisFimc(Employe e) {
        Function<DocumentConformite, String> moisAnnee = d -> String.format("%02d/%d",
                d.getDateExpiration().getMonthValue(), d.getDateExpiration().getYear());
        String permis = document(e, TypeDocument.PERMIS).map(d -> "Exp. " + moisAnnee.apply(d)).orElse("");
        String fimc = document(e, TypeDocument.FIMC)
                .filter(d -> conformite.statut(d) != StatutConformite.VALIDE)
                .map(d -> "FIMC " + moisAnnee.apply(d)).orElse("");
        return permis.isEmpty() || fimc.isEmpty() ? permis + fimc : permis + " · " + fimc;
    }

    /** "Conforme (11/2027)", "Échéance 15 jours", "Expirée". */
    private String visite(Employe e) {
        return document(e, TypeDocument.VISITE_MEDICALE).map(d -> switch (conformite.statut(d)) {
            case EXPIRE -> "Expirée";
            case A_RENOUVELER -> {
                long jours = conformite.joursRestants(d);
                yield jours == 0 ? "Échéance aujourd'hui" : "Échéance " + jours + (jours > 1 ? " jours" : " jour");
            }
            case VALIDE -> String.format("Conforme (%02d/%d)", d.getDateExpiration().getMonthValue(),
                    d.getDateExpiration().getYear());
        }).orElse("Non renseignée");
    }
}
