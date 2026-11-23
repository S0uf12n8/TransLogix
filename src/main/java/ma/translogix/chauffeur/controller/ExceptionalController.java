package ma.translogix.chauffeur.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import ma.translogix.chauffeur.AppContext;
import ma.translogix.chauffeur.model.Rows.ExceptionalRow;
import ma.translogix.chauffeur.util.FileInput;
import ma.translogix.chauffeur.util.TableUtil;
import ma.translogix.chauffeur.util.Ui;

/** Section 11 : Demande d'intervention exceptionnelle. */
public class ExceptionalController {
    @FXML private ComboBox<String> excMission, excReason;
    @FXML private TextArea excDetails;
    @FXML private HBox excFileBox;
    @FXML private Button excFileBtn;
    @FXML private Label excFileName;
    @FXML private CheckBox excCertify;
    @FXML private TableView<ExceptionalRow> table;
    @FXML private TableColumn<ExceptionalRow, ?> colRef, colMission, colReason, colDate, colDecision, colDemo;

    private final ObservableList<ExceptionalRow> rows = FXCollections.observableArrayList();
    private FileInput file;
    private int excSeq = 100;
    private int excDecisionIdx = 0;
    private static final String[][] OUTCOMES = {
        {"green", "Demande acceptée · mission réaffectée"},
        {"amber", "Mission reportée par le Dispatching"},
        {"red", "Demande refusée · mission maintenue"}};

    @FXML
    private void initialize() {
        excMission.setItems(FXCollections.observableArrayList(AppContext.get().missionData.values().stream()
                .map(d -> d.ref + " · " + d.from + " → " + d.to)
                .toList()));
        excReason.setItems(FXCollections.observableArrayList("Panne grave du véhicule", "Problème de santé",
                "Document obligatoire expiré (permis, FIMC, carte tachygraphe, visite médicale)",
                "Accident ou incident majeur", "Autre impossibilité justifiée"));
        excMission.getSelectionModel().selectFirst();
        excReason.getSelectionModel().selectFirst();
        file = new FileInput(excFileBtn, excFileName);

        TableUtil.text(colRef, r -> r.ref, "cell-bold800");
        TableUtil.text(colMission, r -> r.mission);
        TableUtil.text(colReason, r -> r.reason);
        TableUtil.text(colDate, r -> r.date);
        TableUtil.node(colDecision, r -> Ui.pill(r.decisionLabel, r.decisionCls));
        TableUtil.node(colDemo, r -> {
            if (r.decided) return new Label("—");
            Button b = MissionsController.link("Simuler la décision", "link-orange");
            b.setOnAction(e -> simulateDecision(r));
            return b;
        });
        table.setItems(rows);
        TableUtil.fit(table, rows, 82);
    }

    @FXML private void toggleCertify() { excCertify.setSelected(!excCertify.isSelected()); }

    @FXML
    private void submitExceptional() {
        if (excDetails.getText().isEmpty()) { Ui.invalid(excDetails, "Veuillez renseigner ce champ."); return; }
        if (!excCertify.isSelected()) { Ui.invalid(excCertify, "Cochez cette case si vous voulez continuer."); return; }
        if (excMission.getValue() == null) { Ui.invalid(excMission, "Sélectionnez une mission."); return; }
        String mission = Ui.firstToken(excMission.getValue());
        String reason = excReason.getValue();
        rows.add(0, new ExceptionalRow("INT-2026-" + (++excSeq), mission, reason, Ui.todayShort()));
        AppContext ctx = AppContext.get();
        ctx.addNotification("instruction", "Demande transmise au Dispatching",
                mission + " · " + reason + ". La mission reste à exécuter jusqu’à la décision du Dispatching.", null, null, null);
        ctx.toast("Demande transmise. La décision appartient au Dispatching : la mission reste à exécuter.");
        excMission.getSelectionModel().selectFirst();
        excReason.getSelectionModel().selectFirst();
        excDetails.clear();
        file.reset();
        excCertify.setSelected(false);
    }

    private void simulateDecision(ExceptionalRow r) {
        String[] outcome = OUTCOMES[excDecisionIdx++ % OUTCOMES.length];
        r.decisionCls = outcome[0];
        r.decisionLabel = outcome[1];
        r.decided = true;
        table.refresh();
        AppContext.get().addNotification("instruction", "Décision du Dispatching",
                r.mission + " : " + outcome[1] + " (simulation).", null, null, null);
    }
}
