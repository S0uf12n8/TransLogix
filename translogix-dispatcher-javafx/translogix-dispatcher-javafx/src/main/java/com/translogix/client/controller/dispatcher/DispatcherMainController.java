package com.translogix.client.controller.dispatcher;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DispatcherMainController {

    @FXML private StackPane contentArea;
    @FXML private Label pageKickerLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label currentDateLabel;
    @FXML private DispatcherSidebarController sidebarController;

    @FXML
    public void initialize() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));
        currentDateLabel.setText(dateStr.substring(0, 1).toUpperCase() + dateStr.substring(1));

        if (sidebarController != null) {
            sidebarController.setMainController(this);
        }

        loadView("OrdersView.fxml", "OPÉRATIONS EN DIRECT", "Ordres & Trajets");
    }

    public void loadView(String fxmlFileName, String kicker, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dispatcher/views/" + fxmlFileName));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
            pageKickerLabel.setText(kicker);
            pageTitleLabel.setText(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleNewAssignment() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle Affectation de Transport");
        dialog.setHeaderText("Créer un ordre de mission et affecter un chauffeur + véhicule (EF-13 / EF-14)");

        ButtonType saveBtn = new ButtonType("Valider & Transmettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        TextField refField = new TextField("TRJ-9085");
        ComboBox<String> driverBox = new ComboBox<>();
        driverBox.getItems().addAll("Mohammed Bennani (En mission TRJ-9081)", "Hassan Mansouri (Disponible)", "Youssef Amrani (Planifié TRJ-9083)");
        driverBox.getSelectionModel().select(1);

        ComboBox<String> truckBox = new ComboBox<>();
        truckBox.getItems().addAll("12345-A-6 · Volvo FH (Occupé)", "45678-C-1 · MAN TGX (Libre)", "11223-D-8 · DAF XF (Libre)");
        truckBox.getSelectionModel().select(1);

        TextField originField = new TextField("Casablanca Port");
        TextField destField = new TextField("Tanger Med");
        TextField cargoField = new TextField("22 T · Produits Industriels");

        grid.addRow(0, new Label("Réf. Mission :"), refField);
        grid.addRow(1, new Label("Chauffeur :"), driverBox);
        grid.addRow(2, new Label("Véhicule :"), truckBox);
        grid.addRow(3, new Label("Départ :"), originField);
        grid.addRow(4, new Label("Arrivée :"), destField);
        grid.addRow(5, new Label("Cargaison :"), cargoField);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveBtn) {
                String selectedDriver = driverBox.getValue();
                String selectedTruck = truckBox.getValue();

                if (selectedDriver.contains("TRJ-9081") || selectedTruck.contains("Occupé")) {
                    Alert conflictAlert = new Alert(Alert.AlertType.ERROR);
                    conflictAlert.setTitle("Conflit d'affectation détecté (EF-15)");
                    conflictAlert.setHeaderText("Affectation refusée : Chevauchement de période !");
                    conflictAlert.setContentText("La ressource sélectionnée est déjà affectée à la mission en conflit [TRJ-9081 : Casablanca -> Tanger Med].\nVeuillez choisir un chauffeur et un véhicule libres.");
                    conflictAlert.showAndWait();
                } else {
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Mission Planifiée");
                    ok.setHeaderText("Ordre " + refField.getText() + " enregistré (Statut : PLANIFIÉE)");
                    ok.setContentText("Affecté à : " + selectedDriver + "\nVéhicule : " + selectedTruck);
                    ok.showAndWait();
                }
            }
        });
    }
}
