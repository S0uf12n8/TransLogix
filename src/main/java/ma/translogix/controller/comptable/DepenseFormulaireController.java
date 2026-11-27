package ma.translogix.controller.comptable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ma.translogix.AppContext;
import ma.translogix.model.Depense;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.TypeDepense;
import ma.translogix.service.DepenseService;
import ma.translogix.util.Formats;
import ma.translogix.util.ValidationException;

/** Fields of the "Saisir une dépense" dialog. The figures come from the real receipt, never invented. */
public class DepenseFormulaireController implements ComptableUi.Formulaire {

    static final String FXML = "comptable/depense-dialog.fxml";

    @FXML private Label titre;
    @FXML private Label apercuMission;
    @FXML private ComboBox<Mission> champMission;
    @FXML private ComboBox<Employe> champChauffeur;
    @FXML private ComboBox<TypeDepense> champType;
    @FXML private TextField champStation;
    @FXML private TextField champMontant;
    @FXML private TextField champLitres;
    @FXML private DatePicker champDate;

    private DepenseService service;

    @FXML
    private void initialize() {
        List<Mission> missions = AppContext.missionService().lister();
        champMission.setItems(FXCollections.observableArrayList(missions));
        champMission.setCellFactory(liste -> new CelluleMission());
        champMission.setButtonCell(new CelluleMission());

        champChauffeur.setItems(FXCollections.observableArrayList(AppContext.employeService().listerChauffeurs()));
        champChauffeur.setConverter(new StringConverter<>() {
            @Override
            public String toString(Employe e) {
                return e == null ? "" : DepenseService.nom(e);
            }

            @Override
            public Employe fromString(String texte) {
                return null;
            }
        });

        champType.setItems(FXCollections.observableArrayList(TypeDepense.values()));
        champType.setConverter(new StringConverter<>() {
            @Override
            public String toString(TypeDepense t) {
                return t == null ? "" : t.getLibelle();
            }

            @Override
            public TypeDepense fromString(String texte) {
                return null;
            }
        });

        champDate.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate valeur) {
                return Formats.date(valeur);
            }

            @Override
            public LocalDate fromString(String texte) {
                if (texte == null || texte.isBlank()) {
                    return null;
                }
                try {
                    return Formats.lireDate(texte);
                } catch (ValidationException e) {
                    return null;
                }
            }
        });

        champMission.valueProperty().addListener((obs, ancien, mission) -> {
            apercuMission.setText(mission == null ? "Sélectionnez une mission réelle."
                    : mission.getReference() + " · " + mission.getItineraire());
            if (mission != null && mission.getChauffeurId() != null) {
                champChauffeur.getItems().stream()
                        .filter(e -> e.getId().equals(mission.getChauffeurId()))
                        .findFirst()
                        .ifPresent(champChauffeur::setValue);
            }
        });
    }

    /** Called by the page with the same service instance so the expense lands in the page's list. */
    void preparer(DepenseService service) {
        this.service = service;
        champDate.setValue(LocalDate.now());
        champType.setValue(TypeDepense.CARBURANT);
        if (champMission.getItems().isEmpty()) {
            champMission.setPromptText("Aucune mission disponible");
        }
    }

    @Override
    public void enregistrer() {
        Mission mission = champMission.getValue();
        Employe chauffeur = champChauffeur.getValue();
        BigDecimal montant = FactureFormulaireController.lireMontant(champMontant.getText());
        BigDecimal litres = FactureFormulaireController.lireMontant(champLitres.getText());
        Depense creee = service.creer(mission == null ? null : mission.getId(),
                chauffeur == null ? null : chauffeur.getId(), champType.getValue(), champStation.getText(),
                montant, litres, champDate.getValue());
        if (creee == null) {
            throw new ValidationException("La dépense n'a pas pu être enregistrée.");
        }
    }

    @Override
    public String titre() {
        return titre.getText();
    }

    private static final class CelluleMission extends ListCell<Mission> {
        @Override
        protected void updateItem(Mission mission, boolean empty) {
            super.updateItem(mission, empty);
            setText(empty || mission == null ? null : mission.getReference() + " · " + mission.getItineraire());
        }
    }
}
