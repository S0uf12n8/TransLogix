package ma.translogix.chauffeur.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import ma.translogix.chauffeur.AppContext;
import ma.translogix.chauffeur.model.MissionData;
import ma.translogix.chauffeur.util.Ui;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;

/** Section 1 : Tableau de bord. */
public class DashboardController {
    @FXML private StackPane bannerPane;
    @FXML private ProgressBar tripProgressBar, kpiDoneBar;
    @FXML private Label greetingLabel, bannerSubLabel, activeOrderLabel, bannerFrom, bannerTo,
            bannerClient, bannerEta, etaDeltaLabel, bannerCargo, tripProgressLabel,
            tachyLabel, remainingDistanceLabel, cmrLabel;
    @FXML private Label kpiMissions, kpiMissionsTitle, kpiMissionsUnit, kpiMissionsHint;
    @FXML private Label kpiDone, kpiDoneTitle, kpiDoneHint;
    @FXML private Label kpiActive, kpiActiveTitle, kpiActiveUnit, kpiActiveHint;
    @FXML private Label kpiPlanned, kpiPlannedTitle, kpiPlannedUnit, kpiPlannedHint;

    @FXML
    private void initialize() {
        Ui.clipRounded(bannerPane, 16);                 // overflow:hidden + rounded-2xl
        for (Node n : bannerPane.getParent().lookupAll(".kpi")) {   // cartes KPI : overflow:hidden
            if (n instanceof Region) Ui.clipRounded((Region) n, 14);
        }

        AppContext ctx = AppContext.get();
        Employe chauffeur = ctx.chauffeur();
        String prenom = chauffeur == null || chauffeur.getPrenom() == null || chauffeur.getPrenom().isBlank()
                ? "—" : chauffeur.getPrenom();
        greetingLabel.setText("Salam, " + prenom);

        // Mission affichée par le détail / la preuve de livraison : l'ordre actif du tableau de bord.
        MissionData active = ctx.missionData.get(ctx.selectedMissionRef);
        bannerSubLabel.setText(active == null ? "—"
                : "Camion " + nvl(active.vehicle) + " · " + nvl(active.from) + " → " + nvl(active.to));
        activeOrderLabel.setText("Ordre Actif · " + (active == null ? "—" : nvl(active.ref)));
        bannerFrom.setText(active == null ? "—" : nvl(active.from));
        bannerTo.setText(active == null ? "—" : nvl(active.to));
        bannerClient.setText(active == null ? "—"
                : "Client : " + nvl(active.client) + " · Produit : " + nvl(active.cargoDetail));
        bannerEta.setText(active == null ? "—" : nvl(active.end));
        etaDeltaLabel.setText("");                      // aucun écart ETA calculé par le service
        bannerCargo.setText(active == null ? "—" : nvl(active.cargo));   // poids + " T" (voir AppContext.reload)
        tachyLabel.setText("Non renseigné par le service");
        remainingDistanceLabel.setText("—");                            // distance non portée par Mission
        cmrLabel.setText(active == null ? "—" : cmr(active));

        int totalSteps = AppContext.STEP_LABELS.length;
        double progression = active == null ? 0
                : Math.min(1.0, Math.max(0.0, (double) active.done / totalSteps));
        tripProgressLabel.setText(Math.round(progression * 100) + "% effectué");
        tripProgressBar.setProgress(progression);

        // KPI : comptage réel des missions du chauffeur, par statut métier.
        int missions = 0, livres = 0, enCours = 0, planifiees = 0;
        for (Mission mission : ctx.missions()) {
            missions++;
            StatutMission statut = mission.getStatut();
            if (statut == StatutMission.LIVREE || statut == StatutMission.FACTUREE) {
                livres++;
            } else if (statut == StatutMission.EN_COURS) {
                enCours++;
            } else if (statut == StatutMission.PLANIFIEE) {
                planifiees++;
            }
        }
        kpiMissions.setText(String.valueOf(missions));
        kpiDone.setText(String.valueOf(livres));
        kpiActive.setText(String.valueOf(enCours));
        kpiPlanned.setText(String.valueOf(planifiees));

        // Libellés et unités des cartes alignés sur les nouveaux indicateurs.
        kpiMissionsTitle.setText("Missions");
        kpiDoneTitle.setText("Livrées");
        kpiActiveTitle.setText("En cours");
        kpiPlannedTitle.setText("Planifiées");
        kpiMissionsUnit.setText("");
        kpiActiveUnit.setText("");
        kpiPlannedUnit.setText("");
        kpiMissionsHint.setText("Missions assignées");
        kpiDoneHint.setText("Livrées ou facturées");
        kpiActiveHint.setText("En cours de transport");
        kpiPlannedHint.setText("Planifiées");
        Ui.setVisibleManaged(kpiDoneBar, false);
    }

    @FXML private void updateMissionProgress() {
        AppContext ctx = AppContext.get();
        MissionData active = ctx.missionData.get(ctx.selectedMissionRef);
        // Le point GPS ne modifie pas le statut métier : on réaffiche la progression réelle.
        int totalSteps = AppContext.STEP_LABELS.length;
        double progression = active == null ? 0
                : Math.min(1.0, Math.max(0.0, (double) active.done / totalSteps));
        tripProgressBar.setProgress(progression);
        tripProgressLabel.setText(Math.round(progression * 100) + "% effectué");
        ctx.toast("Point GPS mis à jour : position enregistrée sur la mission "
                + (active == null ? "—" : nvl(active.ref)) + ".");
    }
    @FXML private void openIncident() { AppContext.get().openIncidentModal(); }
    @FXML private void openExpense() { AppContext.get().openExpenseModal(); }
    @FXML private void onPause() { AppContext.get().toast("Début de pause obligatoire enregistré sur le tachygraphe."); }
    @FXML private void goInspection() { AppContext.get().navigate("driver-inspection"); }
    @FXML private void goPod() { AppContext.get().navigate("driver-pod"); }
    @FXML private void goChat() { AppContext.get().navigate("driver-chat"); }

    private static String cmr(MissionData m) {
        return switch (nvl(m.status)) {
            case "Planifié" -> "En attente de signature";
            case "En cours" -> "Signée au départ";
            case "Terminé" -> "Livrée & signée";
            default -> "—";
        };
    }

    private static String nvl(String s) { return s == null || s.isBlank() ? "—" : s; }
}
