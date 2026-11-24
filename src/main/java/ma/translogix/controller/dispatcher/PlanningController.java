package ma.translogix.controller.dispatcher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;
import ma.translogix.model.TypeDocument;
import ma.translogix.model.Vehicule;
import ma.translogix.service.ConformiteService;
import ma.translogix.service.MissionService;

/** "Planning Chauffeurs": each active driver with his current or next mission. Read from the services only. */
public class PlanningController {

    /** Legal daily driving limit shown next to the (DÉMO) driving time. */
    private static final int LIMITE_HEURES = 9;

    /** One active driver with what the row shows (looked up once per refresh). */
    record Ligne(Employe chauffeur, Optional<Mission> mission, Optional<Vehicule> vehicule,
                 Optional<DocumentConformite> permis, Duration aujourdhui) {

        boolean enMission() {
            return mission.map(m -> m.getStatut() == StatutMission.EN_COURS).orElse(false);
        }
    }

    @FXML private Button boutonAssigner;
    @FXML private TableView<Ligne> table;
    @FXML private TableColumn<Ligne, Ligne> colChauffeur;
    @FXML private TableColumn<Ligne, String> colCamion;
    @FXML private TableColumn<Ligne, Ligne> colMission;
    @FXML private TableColumn<Ligne, Ligne> colPermis;
    @FXML private TableColumn<Ligne, String> colHeures;
    @FXML private TableColumn<Ligne, Ligne> colDisponibilite;
    @FXML private TableColumn<Ligne, Ligne> colAction;

    private final MissionService missions = AppContext.missionService();
    private final ConformiteService conformite = AppContext.conformiteService();

    @FXML
    private void initialize() {
        colChauffeur.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colChauffeur.setCellFactory(col -> DispatcherUi.doubleLigne(l -> l.chauffeur().getNomComplet(),
                l -> l.chauffeur().getMatricule()));
        colCamion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().vehicule()
                .map(v -> v.getMatricule() + (v.getMarque() == null ? "" : " (" + v.getMarque() + ")"))
                .orElse("Non assigné")));
        colCamion.setCellFactory(col -> DispatcherUi.texte("cell-normal"));
        colMission.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colMission.setCellFactory(col -> DispatcherUi.doubleLigne(
                l -> l.mission().map(m -> m.getReference() + " → " + m.getVilleArrivee()).orElse("Aucune mission"),
                PlanningController::horaire));
        colPermis.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colPermis.setCellFactory(col -> DispatcherUi.badge(this::permis, this::classePermis));
        colHeures.setCellValueFactory(c -> new SimpleStringProperty(heures(c.getValue().aujourdhui())));
        colHeures.setCellFactory(col -> DispatcherUi.texte("cell-normal"));
        colDisponibilite.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colDisponibilite.setCellFactory(col -> DispatcherUi.badge(l -> l.enMission() ? "En mission" : "Disponible",
                l -> l.enMission() ? "badge-amber" : "badge-green"));
        colAction.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colAction.setCellFactory(col -> DispatcherUi.liens(l -> List.of(
                new DispatcherUi.Lien("Assigner trajet", "link-orange", () -> assigner(l.chauffeur())))));

        if (DispatcherUi.lectureSeule()) {
            DispatcherUi.masquer(boutonAssigner);
        }
        rafraichir();
    }

    @FXML
    private void assigner() {
        assigner(null);
    }

    /** Assigns driver + truck to an existing planned mission, with the driver preselected from a row. */
    private void assigner(Employe chauffeur) {
        Mission mission = choisirMission(chauffeur);
        if (mission == null) {
            return;
        }
        if (DispatcherUi.<MissionFormulaireController>ouvrirFormulaire(table, MissionFormulaireController.FXML,
                f -> f.preparerAffectation(mission, chauffeur))) {
            rafraichir();
        }
    }

    /** Picks the mission to assign from the service (no hardcoded mission). */
    private Mission choisirMission(Employe chauffeur) {
        List<Mission> planifiees = missions.lister().stream()
                .filter(m -> m.getStatut() == StatutMission.PLANIFIEE)
                .sorted(Comparator.comparing(Mission::getDateDebut,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (planifiees.isEmpty()) {
            Dialogues.info("Assigner chauffeur / camion", "Aucune mission planifiée n'est à assigner.");
            return null;
        }
        // The choice box shows a readable label; the record keeps the mission itself (no hardcoded mission).
        record Option(Mission mission) {
            @Override
            public String toString() {
                return mission.getReference() + " · " + mission.getItineraire() + " · "
                        + (mission.getDateDebut() == null ? "" : DispatcherUi.momentRelatif(
                                mission.getDateDebut(), LocalDate.now()));
            }
        }
        List<Option> options = planifiees.stream().map(Option::new).toList();
        ChoiceDialog<Option> choix = new ChoiceDialog<>(options.get(0), options);
        choix.setTitle("Assigner chauffeur / camion");
        choix.setHeaderText(chauffeur == null ? "Choisissez la mission à assigner."
                : "Choisissez la mission à assigner à " + chauffeur.getNomComplet() + ".");
        choix.setContentText("Mission planifiée :");
        Option selection = choix.showAndWait().orElse(null);
        return selection == null ? null : selection.mission();
    }

    private void rafraichir() {
        LocalDateTime maintenant = LocalDateTime.now();
        table.setItems(FXCollections.observableArrayList(AppContext.employeService().listerChauffeurs().stream()
                .map(e -> {
                    Optional<Mission> m = missions.missionActuelleOuProchaine(e.getId());
                    Optional<Vehicule> v = m.map(x -> AppContext.vehiculeService().trouver(x.getVehiculeId()));
                    return new Ligne(e, m, v, conformite.document(e.getId(), TypeDocument.PERMIS),
                            tempsDeMissionAujourdhui(e.getId(), maintenant));
                })
                .toList()));
    }

    /** Second line of the mission cell: "Arrivée Aujourd'hui 18:30" / "Départ Demain 09:00". */
    private static String horaire(Ligne l) {
        return l.mission().map(m -> m.getStatut() == StatutMission.EN_COURS
                        ? "Arrivée " + DispatcherUi.momentRelatif(m.getDateFin(), LocalDate.now())
                        : "Départ " + DispatcherUi.momentRelatif(m.getDateDebut(), LocalDate.now()))
                .orElse("Libre");
    }

    /** "Cat. EC · Valide 2028", "Cat. C · Expiré". */
    private String permis(Ligne l) {
        return l.permis().map(d -> (d.getCategorie() == null ? "Permis" : "Cat. " + d.getCategorie()) + " · "
                        + switch (conformite.statut(d)) {
                            case VALIDE -> "Valide " + d.getDateExpiration().getYear();
                            case A_RENOUVELER -> "À renouveler";
                            case EXPIRE -> "Expiré";
                        })
                .orElse("Non renseigné");
    }

    private String classePermis(Ligne l) {
        return l.permis().map(d -> switch (conformite.statut(d)) {
            case VALIDE -> "badge-green";
            case A_RENOUVELER -> "badge-amber";
            case EXPIRE -> "badge-red";
        }).orElse("badge-gray");
    }

    /**
     * DÉMO driving time: the part of today's non-cancelled missions already elapsed.
     * Real driving time needs tachograph data (Iteration 3).
     */
    private Duration tempsDeMissionAujourdhui(Long chauffeurId, LocalDateTime maintenant) {
        LocalDateTime minuit = maintenant.toLocalDate().atStartOfDay();
        Duration total = Duration.ZERO;
        for (Mission m : missions.lister()) {
            if (!chauffeurId.equals(m.getChauffeurId()) || m.getStatut() == StatutMission.ANNULEE
                    || m.getStatut() == StatutMission.PLANIFIEE) {
                continue;
            }
            LocalDateTime debut = m.getDateDebut().isAfter(minuit) ? m.getDateDebut() : minuit;
            LocalDateTime fin = m.getDateFin().isBefore(maintenant) ? m.getDateFin() : maintenant;
            if (fin.isAfter(debut)) {
                total = total.plus(Duration.between(debut, fin));
            }
        }
        return total;
    }

    private static String heures(Duration d) {
        return String.format("%dh %02dm / %dh", d.toHours(), d.toMinutesPart(), LIMITE_HEURES);
    }
}
