package com.translogix.client.controller.dispatcher;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class IncidentsController {
    @FXML
    private void handleDeclareIncident() {
        new Alert(Alert.AlertType.WARNING, "Formulaire de déclaration d'incident (IncidentRoute) ouvert.").showAndWait();
    }
}
