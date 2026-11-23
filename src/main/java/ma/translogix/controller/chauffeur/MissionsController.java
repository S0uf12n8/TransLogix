package ma.translogix.controller.chauffeur;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ma.translogix.controller.chauffeur.Rows.MissionRow;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;

/** Section 2 : Mes Ordres de Transport. */
public class MissionsController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM", Locale.FRENCH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

    @FXML private TableView<MissionRow> table;
    @FXML private TableColumn<MissionRow, ?> colRef, colRoute, colClient, colCargo, colDate, colCmr, colStatus, colAction;
    @FXML private TextField searchField;

    private final ObservableList<MissionRow> all = FXCollections.observableArrayList();
    private final FilteredList<MissionRow> filtered = new FilteredList<>(all, r -> true);

    @FXML
    private void initialize() {
        TableUtil.text(colRef, r -> r.ref, "cell-bold800");
        TableUtil.node(colRoute, r -> route(r.from, r.to));
        TableUtil.text(colClient, r -> r.client);
        TableUtil.text(colCargo, r -> r.cargo);
        TableUtil.text(colDate, r -> r.dates);
        TableUtil.node(colCmr, r -> Ui.pill(r.cmrLabel, r.cmrCls));
        TableUtil.node(colStatus, r -> Ui.pill(r.statusLabel, r.statusCls));
        TableUtil.node(colAction, this::actionButtons);
        table.setItems(filtered);
        TableUtil.fit(table, filtered, 0);
        searchField.textProperty().addListener((o, a, q) -> {
            String query = q.trim().toLowerCase(Locale.FRENCH);
            filtered.setPredicate(r -> r.searchText().toLowerCase(Locale.FRENCH).contains(query));
        });
        refresh();
    }

    /** Reconstruit la liste depuis le service TransLogix ; le filtre de recherche est conservé. */
    public void refresh() {
        EspaceChauffeurUi ctx = EspaceChauffeurUi.get();
        List<MissionRow> lignes = new ArrayList<>();
        for (Mission m : ctx.missions()) {
            StatutMission statut = m.getStatut() == null ? StatutMission.PLANIFIEE : m.getStatut();
            String ref = m.getReference();
            String[] cmr = cmr(statut);
            MissionData data = ctx.missionData.get(ref);
            String statusLabel = data == null ? statut.getLibelle() : data.status;
            String statusCls = data == null ? statutClass(statut) : data.statusCls;

            String action1 = "—", action1Toast = "";
            boolean action1Orange = false;
            if (statut == StatutMission.PLANIFIEE) {
                action1 = "Confirmer";
                action1Orange = true;
                action1Toast = "Ordre confirmé : " + ref;
            } else if (statut == StatutMission.EN_COURS) {
                action1 = "Livrée";
                action1Toast = "Mission déclarée livrée : " + ref;
            }
            lignes.add(new MissionRow(ref, m.getVilleDepart(), m.getVilleArrivee(), ctx.clientName(m),
                    cargo(m), dates(m), cmr[0], cmr[1], statusLabel, statusCls,
                    action1, action1Orange, action1Toast));
        }
        all.setAll(lignes);
    }

    private HBox actionButtons(MissionRow r) {
        EspaceChauffeurUi ctx = EspaceChauffeurUi.get();
        String cls = "Livrée".equals(r.action1Label) ? "link-green"
                : (r.action1Orange ? "link-orange" : "link-slate");
        Button a = link(r.action1Label, cls);
        a.setDisable("—".equals(r.action1Label));
        a.setOnAction(e -> {
            if ("Confirmer".equals(r.action1Label)) {
                ctx.demarrer(r.ref);
                ctx.toast(r.action1Toast);
                refresh();
            } else if ("Livrée".equals(r.action1Label)) {
                if (!Dialogues.confirmer("Clôturer la mission",
                        "Confirmer la livraison de la mission " + r.ref + " ?")) return;
                ctx.livrer(r.ref);
                ctx.toast(r.action1Toast);
                refresh();
            }
        });
        Button d = link("Détails", "link-slate");
        d.setOnAction(e -> ctx.openMissionDetail(r.ref));
        HBox box = new HBox(12, a, d);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static String[] cmr(StatutMission statut) {
        return switch (statut) {
            case PLANIFIEE -> new String[]{"En attente", "gray"};
            case EN_COURS -> new String[]{"Signée départ", "green"};
            case LIVREE, FACTUREE -> new String[]{"Livré & signé", "green"};
            case ANNULEE -> new String[]{"—", "gray"};
        };
    }

    private static String statutClass(StatutMission statut) {
        return switch (statut) {
            case PLANIFIEE -> "amber";
            case EN_COURS -> "blue";
            case LIVREE, FACTUREE -> "green";
            case ANNULEE -> "gray";
        };
    }

    private static String cargo(Mission m) {
        String poids = m.getPoidsTonnes() == null ? ""
                : m.getPoidsTonnes().stripTrailingZeros().toPlainString() + " T";
        String cargaison = m.getCargaison() == null ? "" : m.getCargaison().trim();
        if (poids.isEmpty()) return cargaison;
        if (cargaison.isEmpty()) return poids;
        return poids + " · " + cargaison;
    }

    /** Une mission sur deux jours affiche la date courte aux deux extrémités de l'intervalle. */
    private static String dates(Mission m) {
        LocalDateTime debut = m.getDateDebut();
        if (debut == null) return "—";
        LocalDateTime fin = m.getDateFin();
        String debutTxt = DATE.format(debut) + " · " + TIME.format(debut);
        if (fin == null) return debutTxt + " → —";
        if (fin.toLocalDate().equals(debut.toLocalDate())) return debutTxt + " → " + TIME.format(fin);
        return SHORT_DATE.format(debut) + " " + TIME.format(debut) + " → "
                + SHORT_DATE.format(fin) + " " + TIME.format(fin);
    }

    static HBox route(String from, String to) {
        Label arrow = new Label("\uf061");
        arrow.getStyleClass().addAll("fa", "cell-arrow");
        Label f = new Label(from), t = new Label(to);
        f.setMinWidth(Label.USE_PREF_SIZE);
        t.setMinWidth(Label.USE_PREF_SIZE);
        HBox b = new HBox(4, f, arrow, t);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    static Button link(String text, String cls) {
        Button b = new Button(text);
        b.getStyleClass().addAll("link-btn", cls);
        b.setFocusTraversable(false);
        return b;
    }

    @FXML
    private void syncOrders() {
        EspaceChauffeurUi ctx = EspaceChauffeurUi.get();
        ctx.reload();
        refresh();
        ctx.toast("Ordres synchronisés depuis le service TransLogix.");
    }
}
