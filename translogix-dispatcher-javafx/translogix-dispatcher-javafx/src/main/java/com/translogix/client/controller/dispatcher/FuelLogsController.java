package com.translogix.client.controller.dispatcher;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class FuelLogsController {
    @FXML private TextField searchFuelField;
    @FXML private TableView<FuelRow> fuelTable;
    @FXML private TableColumn<FuelRow, String> colVoucherId;
    @FXML private TableColumn<FuelRow, String> colTruck;
    @FXML private TableColumn<FuelRow, String> colDriver;
    @FXML private TableColumn<FuelRow, String> colStation;
    @FXML private TableColumn<FuelRow, String> colLiters;
    @FXML private TableColumn<FuelRow, String> colAmount;
    @FXML private TableColumn<FuelRow, String> colStatus;

    @FXML
    public void initialize() {
        colVoucherId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().voucherId));
        colTruck.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().truck));
        colDriver.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().driver));
        colStation.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().station));
        colLiters.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().liters));
        colAmount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().amount));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));

        // Style les badges dyal les statuts bhal l'HTML
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    if (status.contains("Validé")) badge.getStyleClass().add("badge-green");
                    else badge.getStyleClass().add("badge-amber");
                    setGraphic(badge);
                }
            }
        });

        ObservableList<FuelRow> masterData = FXCollections.observableArrayList(
                new FuelRow("BON-8841", "12345-A-6", "Mohammed Bennani", "Afriquia Casablanca Route Zénata", "250 L", "3 250 MAD", "Validé Dispatch"),
                new FuelRow("BON-8842", "67890-B-2", "Abderrahim El Amrani", "TotalEnergies Agadir Entrée", "180 L", "2 340 MAD", "Validé Dispatch"),
                new FuelRow("BON-8843", "45678-C-1", "Youssef Amrani", "Shell Rabat Autoroute", "200 L", "2 600 MAD", "En attente reçu")
        );

        FilteredList<FuelRow> filtered = new FilteredList<>(masterData, p -> true);
        if (searchFuelField != null) {
            searchFuelField.textProperty().addListener((obs, oldVal, newVal) -> {
                String q = newVal == null ? "" : newVal.toLowerCase();
                filtered.setPredicate(f -> f.voucherId.toLowerCase().contains(q)
                        || f.driver.toLowerCase().contains(q)
                        || f.station.toLowerCase().contains(q));
            });
        }

        fuelTable.setItems(filtered);
    }

    @FXML
    private void handleIssueFuelVoucher() {
        new Alert(Alert.AlertType.INFORMATION, "Formulaire d'émission d'un bon de gazole (RavitaillementCarburant) ouvert.").showAndWait();
    }

    public static class FuelRow {
        String voucherId, truck, driver, station, liters, amount, status;
        public FuelRow(String voucherId, String truck, String driver, String station, String liters, String amount, String status) {
            this.voucherId = voucherId; this.truck = truck; this.driver = driver;
            this.station = station; this.liters = liters; this.amount = amount; this.status = status;
        }
    }
}