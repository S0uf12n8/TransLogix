package ma.translogix.controller.rh;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
import ma.translogix.service.EmployeService;
import ma.translogix.service.FiltreRapport;
import ma.translogix.service.Rapport;
import ma.translogix.service.TypeRapport;
import ma.translogix.util.Navigator;
import ma.translogix.util.Session;

/**
 * "Tableau de bord RH". KPI 1-3 and "Statut rapide" come from EmployeService / ConformiteService
 * (real, in memory); "Congés en attente" is a static DÉMO card (Iteration 3).
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
    @FXML private TableView<Employe> tableChauffeurs;
    @FXML private TableColumn<Employe, Employe> colEmploye;
    @FXML private TableColumn<Employe, String> colPoste;
    @FXML private TableColumn<Employe, Employe> colPermis;
    @FXML private TableColumn<Employe, String> colVisite;
    @FXML private TableColumn<Employe, String> colStatut;
    @FXML private HBox actionRenouveler;

    private final EmployeService employes = AppContext.employeService();
    private final ConformiteService conformite = AppContext.conformiteService();

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
        RhUi.pasEncoreLivre();
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

        tableChauffeurs.setItems(FXCollections.observableArrayList(employes.listerChauffeurs()));
        tableChauffeurs.refresh();
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
