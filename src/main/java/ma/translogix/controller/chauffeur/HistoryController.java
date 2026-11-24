package ma.translogix.controller.chauffeur;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.translogix.controller.chauffeur.Rows.HistoryRow;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;

/** Section 12 : Historique des missions (recherche + filtre de statut). */
public class HistoryController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    @FXML private TableView<HistoryRow> table;
    @FXML private TableColumn<HistoryRow, ?> colRef, colRoute, colClient, colDate, colProof, colStatus, colAction;
    @FXML private ComboBox<String> histStatus;
    @FXML private TextField histSearch;
    @FXML private javafx.scene.control.Label historyDone, historyLate, historyCancelled;

    private final ObservableList<HistoryRow> all = FXCollections.observableArrayList();
    private final FilteredList<HistoryRow> filtered = new FilteredList<>(all, r -> true);

    @FXML
    private void initialize() {
        histStatus.setItems(FXCollections.observableArrayList("Tous les statuts", "Terminé", "Annulée"));
        histStatus.getSelectionModel().selectFirst();

        TableUtil.text(colRef, r -> r.ref, "cell-bold800");
        TableUtil.node(colRoute, r -> MissionsController.route(r.from, r.to));
        TableUtil.text(colClient, r -> r.client);
        TableUtil.text(colDate, r -> r.date);
        TableUtil.node(colProof, r -> Ui.pill(r.proofLabel, r.proofCls));
        TableUtil.node(colStatus, r -> Ui.pill(r.statusLabel, r.statusCls));
        TableUtil.node(colAction, r -> {
            Button b = MissionsController.link("Détails", "link-orange");
            b.setOnAction(e -> EspaceChauffeurUi.get().openMissionDetail(r.ref));
            return b;
        });
        table.setItems(filtered);
        TableUtil.fit(table, filtered, 0);
        histSearch.textProperty().addListener((o, a, b) -> applyHistoryFilters());
        refresh();
    }

    /** Missions livrées / facturées / annulées, du plus récent au plus ancien. */
    public void refresh() {
        EspaceChauffeurUi ctx = EspaceChauffeurUi.get();
        List<HistoryRow> lignes = new ArrayList<>();
        ctx.missions().stream()
                .filter(m -> estArchive(m.getStatut()))
                .sorted(Comparator.comparing(Mission::getDateDebut,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .forEach(m -> {
                    StatutMission statut = m.getStatut();
                    String ref = m.getReference();
                    MissionData data = ctx.missionData.get(ref);
                    String statusLabel = data == null ? statut.getLibelle() : data.status;
                    String statusCls = data == null ? statutClass(statut) : data.statusCls;
                    boolean livree = statut == StatutMission.LIVREE || statut == StatutMission.FACTUREE;
                    String date = m.getDateDebut() == null ? "—" : DATE.format(m.getDateDebut());
                    lignes.add(new HistoryRow(ref, m.getVilleDepart(), m.getVilleArrivee(), ctx.clientName(m),
                            date, livree ? "Signée" : "Non applicable", livree ? "green" : "gray",
                            statusLabel, statusCls, true));
                });
        all.setAll(lignes);
        long done = ctx.missions().stream()
                .filter(m -> m.getStatut() == StatutMission.LIVREE || m.getStatut() == StatutMission.FACTUREE).count();
        long cancelled = ctx.missions().stream()
                .filter(m -> m.getStatut() == StatutMission.ANNULEE).count();
        historyDone.setText(String.valueOf(done));
        historyLate.setText("—"); // retard non calculé par MissionService
        historyCancelled.setText(String.valueOf(cancelled));
    }

    private static boolean estArchive(StatutMission statut) {
        return statut == StatutMission.LIVREE || statut == StatutMission.FACTUREE || statut == StatutMission.ANNULEE;
    }

    private static String statutClass(StatutMission statut) {
        return statut == StatutMission.LIVREE || statut == StatutMission.FACTUREE ? "green" : "gray";
    }

    @FXML
    private void applyHistoryFilters() {
        String query = histSearch.getText().trim().toLowerCase(Locale.FRENCH);
        int idx = histStatus.getSelectionModel().getSelectedIndex();
        String status = idx <= 0 ? "" : histStatus.getValue().toLowerCase(Locale.FRENCH);
        filtered.setPredicate(r -> r.searchText().toLowerCase(Locale.FRENCH).contains(query)
                && (status.isEmpty() || r.statusLabel.toLowerCase(Locale.FRENCH).equals(status)));
    }
}
