package com.translogix.client.controller.dispatcher;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;

public class FleetGpsController {
    @FXML private ListView<String> gpsListView;

    @FXML
    public void initialize() {
        gpsListView.getItems().addAll(
            "🟢 12345-A-6 (Volvo FH) — A1 Autoroute Kenitra · 82 km/h",
            "⚪ 67890-B-2 (Scania R500) — Dépôt Marrakech · Moteur coupé",
            "🟢 45678-C-1 (MAN TGX) — N1 Route Nationale Salé · 60 km/h",
            "🔴 98765-B-2 (Scania R450) — Garage Central Casablanca (Atelier)"
        );
    }

    @FXML
    private void handleRefreshGps() {
        new Alert(Alert.AlertType.INFORMATION, "Positions GPS actualisées en temps réel.").showAndWait();
    }
}
