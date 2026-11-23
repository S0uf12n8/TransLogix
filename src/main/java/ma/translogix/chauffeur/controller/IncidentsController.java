package ma.translogix.chauffeur.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ma.translogix.chauffeur.AppContext;
import ma.translogix.chauffeur.model.Rows.IncidentRow;
import ma.translogix.chauffeur.util.TableUtil;
import ma.translogix.chauffeur.util.Ui;

/** Section 5 : Incidents &amp; Support. */
public class IncidentsController {
    @FXML private TableView<IncidentRow> table;
    @FXML private TableColumn<IncidentRow, ?> colRef, colMission, colType, colSeverity, colLocation, colAttachment, colStatus;

    private final AppContext ctx = AppContext.get();

    @FXML
    private void initialize() {
        ctx.register(this);
        TableUtil.text(colRef, r -> r.ref, "cell-bold800");
        TableUtil.text(colMission, r -> r.mission);
        TableUtil.text(colType, r -> r.type);
        TableUtil.node(colSeverity, r -> Ui.pill(r.severity, r.severityCls));
        TableUtil.text(colLocation, r -> r.location);
        TableUtil.text(colAttachment, r -> r.attachment);
        TableUtil.node(colStatus, r -> Ui.pill(r.status, "amber"));
        table.setItems(ctx.incidentRows);
        TableUtil.fit(table, ctx.incidentRows, 82);   // ligne vide « Aucun signalement… » : py-8 = 82 px
    }

    /** Délègue au pont partagé : la ligne est ajoutée à incidentRows (et l'alerte notifiée). */
    public void addIncident(String tripRef, String type, String severity, String location, String attachmentName) {
        ctx.addIncident(tripRef, type, severity, location, attachmentName);
        refresh();
    }

    public void refresh() { table.refresh(); }

    @FXML private void reportProblem() { ctx.openIncidentModal(); }
}
