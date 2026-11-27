package ma.translogix.controller.dispatcher;

import java.time.LocalDateTime;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ma.translogix.model.GraviteIncident;
import ma.translogix.model.Mission;
import ma.translogix.model.TypeIncident;
import ma.translogix.service.IncidentService;

/** Fields of the "Déclarer un incident" dialog. All rules stay in IncidentService. */
public class IncidentFormulaireController implements DispatcherUi.Formulaire {
    static final String FXML = "dispatcher/incident-dialog.fxml";

    @FXML private ComboBox<Mission> champMission;
    @FXML private ComboBox<TypeIncident> champType;
    @FXML private ComboBox<GraviteIncident> champGravite;
    @FXML private TextField champLocalisation;
    @FXML private TextArea champDescription;
    @FXML private DatePicker champDate;
    @FXML private TextField champHeure;

    private final IncidentService service = DispatcherServices.incidents();
    private Mission mission;

    @FXML
    private void initialize() {
        champMission.getItems().setAll(service.missionsEligibles());
        champMission.setConverter(convertisseurMission());
        champType.getItems().setAll(TypeIncident.values());
        champGravite.getItems().setAll(GraviteIncident.values());
        DispatcherUi.formatDate(champDate);
        LocalDateTime maintenant = LocalDateTime.now();
        champDate.setValue(maintenant.toLocalDate());
        champHeure.setText(DispatcherUi.heure(maintenant));
    }

    /** Pre-selects the mission when the dialog is opened from a row. */
    void preparer(Mission selection) {
        mission = selection;
        if (selection != null) {
            champMission.getItems().stream()
                    .filter(m -> Objects.equals(m.getId(), selection.getId()))
                    .findFirst()
                    .ifPresent(champMission::setValue);
        }
    }

    @Override
    public void enregistrer() {
        Mission choisie = champMission.getValue() == null ? mission : champMission.getValue();
        service.declarer(choisie == null ? null : choisie.getId(), champType.getValue(), champGravite.getValue(),
                champDescription.getText(), champLocalisation.getText(),
                DispatcherUi.lireDateHeure(champDate, champHeure, "de signalement"));
    }

    @Override
    public String titre() {
        return "Déclarer un incident";
    }

    private static StringConverter<Mission> convertisseurMission() {
        return new StringConverter<>() {
            @Override
            public String toString(Mission m) {
                return m == null ? "" : m.getReference() + " · " + m.getItineraire();
            }

            @Override
            public Mission fromString(String texte) {
                return null; // not editable
            }
        };
    }
}
