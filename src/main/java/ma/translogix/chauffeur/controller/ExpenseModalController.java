package ma.translogix.chauffeur.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import ma.translogix.chauffeur.AppContext;
import ma.translogix.chauffeur.util.Ui;

/** Fenêtre « Saisir un Bon / Reçu » (mode « expense » de la modale d'origine). */
public class ExpenseModalController {
    @FXML private ComboBox<String> tripRef, type;
    @FXML private TextField station, amount, liters;

    @FXML
    private void initialize() {
        AppContext ctx = AppContext.get();
        tripRef.setItems(FXCollections.observableArrayList(ctx.missionData.values().stream()
                .map(d -> d.ref + " · " + d.from + " → " + d.to)
                .toList()));
        type.setItems(FXCollections.observableArrayList("Ravitaillement Gazole", "Péage Autoroute (ADM)",
                "Réparation / Vulcanisation Pneumatique", "Lavage Camion / Frigo", "Repas / Nuitée"));
        tripRef.getSelectionModel().selectFirst();
        type.getSelectionModel().selectFirst();
        Ui.numberOnly(amount);
        Ui.numberOnly(liters);
    }

    @FXML
    private void submit() {
        if (!Ui.requireText(station) || !Ui.requireNumber(amount, null) || !Ui.requireNumber(liters, null)) return;
        if (tripRef.getValue() == null) { Ui.invalid(tripRef, "Sélectionnez une mission."); return; }
        AppContext ctx = AppContext.get();
        ctx.addReceipt(tripRef.getValue(), type.getValue(), station.getText(), amount.getText());
        ctx.toast("Reçu de " + amount.getText() + " MAD transmis pour validation comptable.");
        ctx.closeModal();
    }

    @FXML private void close() { AppContext.get().closeModal(); }
}
