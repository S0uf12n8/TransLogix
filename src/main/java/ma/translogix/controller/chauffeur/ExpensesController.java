package ma.translogix.controller.chauffeur;

import java.util.Locale;

import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.translogix.controller.chauffeur.Rows.ExpenseRow;

/** Section 3 : Reçus &amp; Carburant. */
public class ExpensesController {
    @FXML private TableView<ExpenseRow> table;
    @FXML private TableColumn<ExpenseRow, ?> colRef, colMission, colType, colStation, colAmount, colStatus;
    @FXML private TextField searchField;
    @FXML private Label driverPendingAmount, validatedAmount, fuelConsumption;

    private final EspaceChauffeurUi ctx = EspaceChauffeurUi.get();
    private final FilteredList<ExpenseRow> filtered = new FilteredList<>(ctx.expenseRows, r -> true);

    @FXML
    private void initialize() {
        ctx.register(this);
        TableUtil.text(colRef, r -> r.ref, "cell-bold800");
        TableUtil.text(colMission, r -> r.mission);
        TableUtil.text(colType, r -> r.type);
        TableUtil.text(colStation, r -> r.station);
        TableUtil.text(colAmount, r -> r.amount, "cell-bold900");
        TableUtil.node(colStatus, r -> Ui.pill(r.statusLabel, r.statusCls));
        table.setItems(filtered);
        TableUtil.fit(table, filtered, 0);
        searchField.textProperty().addListener((o, a, q) -> {
            String query = q.trim().toLowerCase(Locale.FRENCH);
            filtered.setPredicate(r -> r.searchText().toLowerCase(Locale.FRENCH).contains(query));
        });
        // Le montant en attente suit les ajouts faits depuis la modale.
        ctx.expenseRows.addListener((ListChangeListener<ExpenseRow>) c -> refresh());
        refresh();
    }

    /** Recalcule le cumul des reçus « En attente » et rafraîchit le tableau. */
    public void refresh() {
        long pending = ctx.expenseRows.stream()
                .filter(r -> "En attente".equals(r.statusLabel))
                .mapToLong(r -> parseAmount(r.amount))
                .sum();
        long validated = ctx.expenseRows.stream()
                .filter(r -> r.statusLabel != null
                        && (r.statusLabel.contains("Validé") || r.statusLabel.contains("Remboursé")))
                .mapToLong(r -> parseAmount(r.amount))
                .sum();
        driverPendingAmount.setText(formatAmount(pending) + " en attente");
        validatedAmount.setText(formatAmount(validated));
        fuelConsumption.setText("—"); // aucune mesure de consommation dans le service
        table.refresh();
    }

    /** « 1 500 MAD » -> 1500 (les espaces de regroupement sont ignorés). */
    private static long parseAmount(String amount) {
        if (amount == null) return 0;
        String digits = amount.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try { return Long.parseLong(digits); }
        catch (NumberFormatException e) { return 0; }
    }

    private static String formatAmount(long amount) {
        return String.valueOf(amount).replaceAll("(\\d)(?=(\\d{3})+$)", "$1 ") + " MAD";
    }

    /** Ajout délégué au pont partagé (conservé pour compatibilité avec l'ancien shell). */
    public void addReceipt(String tripRef, String type, String station, String amount) {
        ctx.addReceipt(tripRef, type, station, amount);
        refresh();
    }

    @FXML private void newReceipt() { ctx.openExpenseModal(); }
}
