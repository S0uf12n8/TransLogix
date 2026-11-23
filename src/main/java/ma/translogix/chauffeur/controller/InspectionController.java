package ma.translogix.chauffeur.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import ma.translogix.chauffeur.AppContext;
import ma.translogix.chauffeur.model.MissionData;
import ma.translogix.chauffeur.util.Ui;

/** Section 9 : Inspection du camion (check-list avant départ). */
public class InspectionController {
    @FXML private VBox inspList;
    @FXML private Label inspStatus, inspCount, inspSubLabel;
    @FXML private ProgressBar inspProgress;

    private static final String[][] ITEMS = {
        {"Pneumatiques", "Pression, usure, état des flancs", "\uf192"},
        {"Freins", "Course de la pédale, frein de parc, bruits anormaux", "\uf28d"},
        {"Feux et signalisation", "Croisement, stop, clignotants, feux de détresse", "\uf0eb"},
        {"Niveau d’huile moteur", "Niveau sur la jauge, absence de fuite", "\uf613"},
        {"Niveau d’eau", "Lave-glace et niveau d’eau du circuit", "\uf043"},
        {"Système de refroidissement", "Liquide, durites, ventilateur", "\uf2c9"}};

    private final List<InspectionItemController> items = new ArrayList<>();

    @FXML
    private void initialize() {
        MissionData mission = activeMission();
        inspSubLabel.setText((mission == null ? "Aucune mission active" : mission.vehicle)
                + " — Contrôle visuel avant départ.");
        renderInspection();
    }

    private void renderInspection() {
        inspList.getChildren().clear();
        items.clear();
        for (String[] it : ITEMS) {
            try {
                FXMLLoader l = new FXMLLoader(getClass().getResource("/fxml/components/InspectionItem.fxml"));
                javafx.scene.Node node = l.load();
                InspectionItemController c = l.getController();
                c.setData(it[0], it[1], it[2], this::onInspChange);
                items.add(c);
                inspList.getChildren().add(node);
            } catch (IOException e) { throw new IllegalStateException(e); }
        }
        onInspChange();
    }

    private void onInspChange() {
        long answered = items.stream().filter(i -> i.getChoice() != null).count();
        inspCount.setText(answered + " / " + items.size());
        inspProgress.setProgress(Math.round((double) answered / items.size() * 100) / 100.0);
    }

    private void setInspStatus(String cls, String text) { Ui.setPill(inspStatus, cls, text); }

    private MissionData activeMission() {
        AppContext ctx = AppContext.get();
        return ctx.missionData.get(ctx.selectedMissionRef);
    }

    @FXML
    private void submitInspection() {
        AppContext ctx = AppContext.get();
        MissionData mission = activeMission();
        String prefix = mission == null ? "" : "Mission " + mission.ref + " · ";
        String suffix = mission == null ? "" : " pour la mission " + mission.ref;
        List<InspectionItemController> missing = items.stream().filter(i -> i.getChoice() == null).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            ctx.toast(prefix + "Points à contrôler : " + missing.stream().map(InspectionItemController::getLabel).collect(Collectors.joining(", ")) + ".");
            return;
        }
        List<InspectionItemController> anomalies = items.stream().filter(i -> "ko".equals(i.getChoice())).collect(Collectors.toList());
        for (InspectionItemController a : anomalies) {
            if (a.getNoteField().getText().trim().isEmpty()) {
                ctx.toast(prefix + "Décrivez l’anomalie : " + a.getLabel() + ".");
                a.getNoteField().requestFocus();
                return;
            }
        }
        if (!anomalies.isEmpty()) {
            setInspStatus("red", anomalies.size() + " anomalie(s) · " + Ui.nowTime());
            ctx.addNotification("alert", "Inspection : anomalies signalées",
                    prefix + anomalies.stream().map(InspectionItemController::getLabel).collect(Collectors.joining(", "))
                            + ". Le Dispatching est informé (simulation).", null, null, null);
            ctx.toast("Inspection enregistrée" + suffix + " avec anomalies : Dispatching informé (démo).");
        } else {
            setInspStatus("green", "Conforme · " + Ui.nowTime());
            ctx.toast("Inspection validée" + suffix + " : camion conforme.");
        }
    }

    @FXML
    private void resetInspection() {
        renderInspection();
        setInspStatus("gray", "Non effectuée");
    }
}
