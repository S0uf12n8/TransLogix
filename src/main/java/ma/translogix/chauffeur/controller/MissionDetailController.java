package ma.translogix.chauffeur.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import ma.translogix.chauffeur.AppContext;
import ma.translogix.chauffeur.model.MissionData;
import ma.translogix.chauffeur.util.Ui;

/** Section 7 : Détail &amp; Suivi de mission. */
public class MissionDetailController {
    @FXML private ComboBox<String> missionSelect;
    @FXML private Label mdStatusPill, mdFrom, mdTo, mdClient, mdTimes, mdDate, mdStepCount;
    @FXML private GridPane mdInfo;
    @FXML private VBox mdInstructions, mdSteps;
    @FXML private ProgressBar mdProgressBar;
    @FXML private Button mdConfirmBtn;

    private final List<String> refs = new ArrayList<>();
    private String currentMissionRef;
    private boolean updating;

    @FXML
    private void initialize() {
        AppContext ctx = AppContext.get();
        ctx.register(this);
        List<String> labels = new ArrayList<>();
        for (MissionData m : ctx.missionData.values()) {
            refs.add(m.ref);
            labels.add(label(m));
        }
        missionSelect.setItems(FXCollections.observableArrayList(labels));
        String selected = ctx.selectedMissionRef;
        if (selected == null || !ctx.missionData.containsKey(selected)) {
            selected = refs.isEmpty() ? null : refs.get(0);
        }
        if (selected != null) renderMissionDetail(selected);
    }

    private static String label(MissionData m) {
        return m.ref + " · " + m.from + " → " + m.to;
    }

    @FXML private void onMissionSelected() {
        if (updating) return;
        int i = missionSelect.getSelectionModel().getSelectedIndex();
        if (i >= 0 && i < refs.size()) renderMissionDetail(refs.get(i));
    }

    public void renderMissionDetail(String ref) {
        AppContext ctx = AppContext.get();
        MissionData m = ref == null ? null : ctx.missionData.get(ref);
        if (m == null) return;
        currentMissionRef = ref;
        updating = true;
        int index = refs.indexOf(ref);
        if (index < 0) {                 // mission apparue après une synchronisation
            refs.add(ref);
            missionSelect.getItems().add(label(m));
            index = refs.size() - 1;
        }
        missionSelect.getSelectionModel().select(index);
        updating = false;

        Ui.setPill(mdStatusPill, m.statusCls, ref + " · " + m.status);
        mdFrom.setText(m.from);
        mdTo.setText(m.to);
        mdClient.setText("Client : " + m.client);
        mdTimes.setText(m.start + " → " + m.end);
        mdDate.setText(m.date);

        mdInfo.getChildren().clear();
        String[][] blocks = {
            {"\uf1ad", "Client", m.client, m.contact},
            {"\ue09a", "Chargement", m.loadingPlace, m.loadingDetail + " · " + m.date + " à " + m.start},
            {"\uf019", "Déchargement", m.unloadingPlace, m.unloadingDetail + " · " + m.date + " à " + m.end},
            {"\uf017", "Horaires", m.start + " → " + m.end, "Date : " + m.date},
            {"\uf468", "Cargaison", m.cargo, m.cargoDetail},
            {"\ue2b7", "Véhicule", m.vehicle == null || m.vehicle.isBlank() ? "—" : m.vehicle,
                    "Véhicule affecté à la mission"}};
        for (int i = 0; i < blocks.length; i++) {
            try {
                FXMLLoader l = new FXMLLoader(getClass().getResource("/fxml/components/InfoBlock.fxml"));
                javafx.scene.Node node = l.load();
                InfoBlockController c = l.getController();
                c.setData(blocks[i][0], blocks[i][1], blocks[i][2], blocks[i][3]);
                mdInfo.add(node, i % 2, i / 2);
            } catch (IOException e) { throw new IllegalStateException(e); }
        }

        mdInstructions.getChildren().clear();
        for (String s : m.instructions) mdInstructions.getChildren().add(Ui.bullet(s, "fs-12", "c-s600"));

        int total = AppContext.STEP_LABELS.length;
        mdStepCount.setText(m.done + " / " + total + " étapes");
        mdProgressBar.setProgress(Math.round((double) m.done / total * 100) / 100.0);
        mdSteps.getChildren().clear();
        for (int i = 0; i < total; i++) {
            String state = i < m.done ? "done" : (i == m.done ? "current" : "pending");
            String sub = "done".equals(state) ? "Confirmée" + (m.times[i] != null ? " à " + m.times[i] : "")
                    : ("current".equals(state) ? "Étape en cours · à confirmer" : "À venir");
            try {
                FXMLLoader l = new FXMLLoader(getClass().getResource("/fxml/components/MissionStep.fxml"));
                javafx.scene.Node node = l.load();
                MissionStepController c = l.getController();
                c.setData(AppContext.STEP_LABELS[i], state, sub, i < total - 1, i == total - 1);
                mdSteps.getChildren().add(node);
            } catch (IOException e) { throw new IllegalStateException(e); }
        }

        boolean finished = m.done >= total;
        mdConfirmBtn.setDisable(finished);
        Label icon = new Label(finished || m.done != total - 1 ? "\uf058" : "\uf573");
        icon.getStyleClass().addAll("fa", "fs-12");
        mdConfirmBtn.setGraphic(icon);
        mdConfirmBtn.setText(finished ? "Mission terminée"
                : (m.done == total - 1 ? "Aller à la preuve de livraison" : "Confirmer : " + AppContext.STEP_LABELS[m.done]));
    }

    @FXML
    private void confirmMissionStep() {
        AppContext ctx = AppContext.get();
        MissionData m = ctx.missionData.get(currentMissionRef);
        int total = AppContext.STEP_LABELS.length;
        if (m == null || m.done >= total) return;
        if (m.done == total - 1) {
            ctx.navigate("driver-pod");
            ctx.toast("Dernière étape : transmettez la preuve de livraison.");
            return;
        }
        int etape = m.done;
        String heure = Ui.nowTime();
        m.times[etape] = heure;
        ctx.avancerEtape(currentMissionRef);          // le bridge met à jour le statut réel et recharge les fiches
        MissionData miseAJour = ctx.missionData.get(currentMissionRef);
        if (miseAJour != null) miseAJour.times[etape] = heure;
        renderMissionDetail(currentMissionRef);
        ctx.toast("Étape confirmée : " + AppContext.STEP_LABELS[etape]);
    }

    @FXML private void backToMissions() { AppContext.get().navigate("driver-missions"); }
    @FXML private void goPod() { AppContext.get().navigate("driver-pod"); }
    @FXML private void goExceptional() { AppContext.get().navigate("driver-exceptional"); }
}
