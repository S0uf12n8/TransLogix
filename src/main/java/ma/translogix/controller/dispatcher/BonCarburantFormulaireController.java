package ma.translogix.controller.dispatcher;

import java.time.LocalDate;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ma.translogix.model.Mission;
import ma.translogix.service.BonCarburantService;
import ma.translogix.util.Formats;

/** Fields of the "Émettre un bon de carburant" dialog. All rules stay in BonCarburantService. */
public class BonCarburantFormulaireController implements DispatcherUi.Formulaire {
    static final String FXML = "dispatcher/bon-carburant-dialog.fxml";

    @FXML private ComboBox<Mission> champMission;
    @FXML private TextField champStation;
    @FXML private TextField champLitres;
    @FXML private TextField champMontant;
    @FXML private DatePicker champDate;

    private final BonCarburantService service = DispatcherServices.bons();
    private Mission mission;

    @FXML
    private void initialize() {
        champMission.getItems().setAll(service.missionsEligibles());
        champMission.setConverter(new StringConverter<>() {
            @Override
            public String toString(Mission m) {
                return m == null ? "" : m.getReference() + " · " + m.getItineraire();
            }

            @Override
            public Mission fromString(String texte) {
                return null; // not editable
            }
        });
        DispatcherUi.formatDate(champDate);
        champDate.setValue(LocalDate.now());
    }

    /** Pre-selects the mission of the selected row when the dialog is opened from the table. */
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
        LocalDate date = champDate.getValue() != null ? champDate.getValue()
                : lireDateSaisie();
        service.emettre(choisie == null ? null : choisie.getId(), champStation.getText(),
                DispatcherUi.lireTonnes(champLitres, "Litres"),
                DispatcherUi.lireTonnes(champMontant, "Montant"), date);
    }

    @Override
    public String titre() {
        return "Émettre un bon de carburant";
    }

    private LocalDate lireDateSaisie() {
        String texte = champDate.getEditor().getText();
        return texte == null || texte.isBlank() ? null : Formats.lireDate(texte);
    }
}
