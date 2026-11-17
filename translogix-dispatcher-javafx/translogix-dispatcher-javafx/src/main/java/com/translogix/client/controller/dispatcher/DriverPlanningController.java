package com.translogix.client.controller.dispatcher;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class DriverPlanningController {
    @FXML private TableView<String[]> planningTable;
    @FXML private TableColumn<String[], String> colDriverName, colAssignedTruck, colLicense, colDrivingHours, colAvailability;

    @FXML
    public void initialize() {
        colDriverName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colAssignedTruck.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colLicense.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        colDrivingHours.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));
        colAvailability.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[4]));

        planningTable.setItems(FXCollections.observableArrayList(
            new String[]{"Mohammed Bennani", "12345-A-6 (Volvo)", "Cat. EC · Valide 2028", "5h 30m / 9h", "En Conduite"},
            new String[]{"Abderrahim El Amrani", "67890-B-2 (Scania)", "Cat. C · Valide 2026", "8h 15m / 9h", "Repos Requis"},
            new String[]{"Hassan Mansouri", "Non assigné", "Cat. EC · Valide 2027", "0h 00m / 9h", "Disponible"}
        ));
    }

    @FXML
    private void handleOpenAssignDialog() {
        new Alert(Alert.AlertType.INFORMATION, "Utilisez le bouton '+ Nouvelle affectation' en haut à droite pour lier un chauffeur et un camion.").showAndWait();
    }
}
