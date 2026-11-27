package ma.translogix.controller.chauffeur;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import ma.translogix.controller.common.Dialogues;

/** Fenêtre « Signaler un Incident sur la Route » (mode « incident » de la modale d'origine). */
public class IncidentModalController {
    @FXML private ComboBox<String> tripRef, incidentType, severity;
    @FXML private TextField locationField;
    @FXML private TextArea details;
    @FXML private HBox attachBox;
    @FXML private Button attachBtn;
    @FXML private Label attachName;
    private FileInput attachment;

    @FXML
    private void initialize() {
        EspaceChauffeurUi ctx = EspaceChauffeurUi.get();
        tripRef.setItems(FXCollections.observableArrayList(ctx.missionData.values().stream()
                .map(d -> d.ref + " · " + d.from + " → " + d.to)
                .toList()));
        incidentType.setItems(FXCollections.observableArrayList("Embouteillage / Bouchon Autoroute", "Panne Mécanique Camion / Moteur",
                "Accident de la Route", "Menace / Agression", "Vol (cargaison, carburant, équipement)",
                "Danger sur la Route (obstacle, météo, chaussée)", "Crevaison Pneumatique", "Problème Groupe Frigo (+4°C)",
                "Retard Contrôle Douanier / Gendarmerie"));
        severity.setItems(FXCollections.observableArrayList("Faible", "Moyenne", "Élevée", "Critique"));
        tripRef.getSelectionModel().selectFirst();
        incidentType.getSelectionModel().selectFirst();
        severity.getSelectionModel().selectFirst();
        attachment = new FileInput(attachBtn, attachName,
                new FileChooser.ExtensionFilter("Images et PDF", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp", "*.pdf"));
    }

    @FXML
    private void submit() {
        if (!Ui.requireText(locationField)) return;
        if (details.getText().isEmpty()) { Ui.invalid(details, "Veuillez renseigner ce champ."); return; }
        if (tripRef.getValue() == null) { Ui.invalid(tripRef, "Sélectionnez une mission."); return; }
        if (!Dialogues.confirmer("Signaler un incident",
                "Transmettre ce signalement en urgence au dispatching ?")) return;
        EspaceChauffeurUi ctx = EspaceChauffeurUi.get();
        ctx.addIncident(tripRef.getValue(), incidentType.getValue(), severity.getValue(), locationField.getText(),
                attachment.getFile() == null ? null : attachment.getFile().getName());
        ctx.toast("Incident (gravité : " + severity.getValue() + ") transmis en urgence au dispatching. Restez à l'écoute.");
        ctx.closeModal();
    }

    @FXML private void close() { EspaceChauffeurUi.get().closeModal(); }
}
