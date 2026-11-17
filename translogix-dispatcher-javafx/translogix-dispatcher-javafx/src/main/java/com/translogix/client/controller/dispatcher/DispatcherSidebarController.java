package com.translogix.client.controller.dispatcher;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import java.util.Arrays;
import java.util.List;

public class DispatcherSidebarController {

    @FXML private Button btnOrders;
    @FXML private Button btnFleetGps;
    @FXML private Button btnDriverPlanning;
    @FXML private Button btnIncidents;
    @FXML private Button btnFuelLogs;

    private DispatcherMainController mainController;

    public void setMainController(DispatcherMainController mainController) {
        this.mainController = mainController;
    }

    private void setActiveButton(Button activeBtn) {
        List<Button> allButtons = Arrays.asList(btnOrders, btnFleetGps, btnDriverPlanning, btnIncidents, btnFuelLogs);
        for (Button btn : allButtons) {
            btn.getStyleClass().remove("nav-btn-active");
        }
        activeBtn.getStyleClass().add("nav-btn-active");
    }

    @FXML
    private void onSelectOrders() {
        setActiveButton(btnOrders);
        mainController.loadView("OrdersView.fxml", "OPÉRATIONS EN DIRECT", "Ordres & Trajets");
    }

    @FXML
    private void onSelectFleetGps() {
        setActiveButton(btnFleetGps);
        mainController.loadView("FleetGpsView.fxml", "CARTOGRAPHIE & GEOFENCING", "Suivi Flotte & GPS");
    }

    @FXML
    private void onSelectDriverPlanning() {
        setActiveButton(btnDriverPlanning);
        mainController.loadView("DriverPlanningView.fxml", "RESSOURCES & DISPONIBILITÉ", "Planning Chauffeurs");
    }

    @FXML
    private void onSelectIncidents() {
        setActiveButton(btnIncidents);
        mainController.loadView("IncidentsView.fxml", "GESTION DES ALÉAS", "Incidents & Alertes");
    }

    @FXML
    private void onSelectFuelLogs() {
        setActiveButton(btnFuelLogs);
        mainController.loadView("FuelLogsView.fxml", "CARBURANT & PÉAGES", "Carburant & Bons");
    }

    @FXML
    private void onLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vous déconnecter de l'espace Dispatcher ?");
        alert.showAndWait();
    }
}
