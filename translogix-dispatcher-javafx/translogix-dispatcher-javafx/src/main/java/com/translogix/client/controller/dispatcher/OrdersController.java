package com.translogix.client.controller.dispatcher;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class OrdersController {

    @FXML private TextField searchField;
    @FXML private TableView<MissionRow> ordersTable;
    @FXML private TableColumn<MissionRow, String> colRef;
    @FXML private TableColumn<MissionRow, String> colDriverTruck;
    @FXML private TableColumn<MissionRow, String> colRoute;
    @FXML private TableColumn<MissionRow, String> colCargo;
    @FXML private TableColumn<MissionRow, String> colEta;
    @FXML private TableColumn<MissionRow, String> colStatus;
    @FXML private TableColumn<MissionRow, String> colActions;

    @FXML
    public void initialize() {
        colRef.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().ref));
        colDriverTruck.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().driverTruck));
        colRoute.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().route));
        colCargo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().cargo));
        colEta.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().eta));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    if (status.contains("Retard") || status.contains("En cours")) badge.getStyleClass().add("badge-amber");
                    else if (status.contains("Livrée") || status.contains("Terminé")) badge.getStyleClass().add("badge-green");
                    else if (status.contains("Planifiée")) badge.getStyleClass().add("badge-blue");
                    else badge.getStyleClass().add("badge-purple");
                    setGraphic(badge);
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Button btn = new Button("Contacter / Éditer");
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-cursor: hand;");
                    btn.setOnAction(e -> {
                        MissionRow row = getTableView().getItems().get(getIndex());
                        new Alert(Alert.AlertType.INFORMATION, "Notification envoyée pour la mission " + row.ref).showAndWait();
                    });
                    setGraphic(btn);
                }
            }
        });

        ObservableList<MissionRow> masterData = FXCollections.observableArrayList(
                new MissionRow("TRJ-9081", "Mohammed Bennani (12345-A-6)", "Casablanca → Tanger Med", "24 T · Produits Frais", "18:30 (Retard +25m)", "En cours (Retard)"),
                new MissionRow("TRJ-9082", "Abderrahim El Amrani (67890-B-2)", "Agadir → Marrakech", "18 T · Agrumes", "16:15 (À l'heure)", "Livrée"),
                new MissionRow("TRJ-9083", "Youssef Amrani (45678-C-1)", "Rabat → Fès", "30 T · Matériaux Const.", "Demain 09:00", "Planifiée"),
                new MissionRow("TRJ-9084", "Karim El Idrissi (11223-D-8)", "Jorf Lasfar → Casablanca", "28 T · Engrais", "Chargement quai", "Créée (Au Quai)")
        );

        FilteredList<MissionRow> filtered = new FilteredList<>(masterData, p -> true);
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                String q = newVal == null ? "" : newVal.toLowerCase();
                filtered.setPredicate(m -> m.ref.toLowerCase().contains(q)
                        || m.driverTruck.toLowerCase().contains(q)
                        || m.route.toLowerCase().contains(q));
            });
        }

        ordersTable.setItems(filtered);
    }

    @FXML
    private void handleExportCsv() {
        new Alert(Alert.AlertType.INFORMATION, "Feuille de route exportée avec succès au format CSV.").showAndWait();
    }

    public static class MissionRow {
        String ref, driverTruck, route, cargo, eta, status;
        public MissionRow(String ref, String driverTruck, String route, String cargo, String eta, String status) {
            this.ref = ref; this.driverTruck = driverTruck; this.route = route;
            this.cargo = cargo; this.eta = eta; this.status = status;
        }
    }
}