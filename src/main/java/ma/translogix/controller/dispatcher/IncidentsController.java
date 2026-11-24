package ma.translogix.controller.dispatcher;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ma.translogix.AppContext;
import ma.translogix.model.Incident;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutIncident;
import ma.translogix.service.IncidentService;

/** "Incidents & Retards de Route": real incidents declared through IncidentService. */
public class IncidentsController {

    /** One incident with the mission reference it shows. */
    record Ligne(Incident incident, String mission, String date, String statut) {
    }

    @FXML private Button boutonDeclarer;
    @FXML private Label nombreOuverts;
    @FXML private TableView<Ligne> tableIncidents;
    @FXML private TableColumn<Ligne, String> colReference;
    @FXML private TableColumn<Ligne, String> colMission;
    @FXML private TableColumn<Ligne, String> colType;
    @FXML private TableColumn<Ligne, String> colGravite;
    @FXML private TableColumn<Ligne, String> colDescription;
    @FXML private TableColumn<Ligne, String> colDate;
    @FXML private TableColumn<Ligne, Ligne> colStatut;

    private final IncidentService service = DispatcherServices.incidents();
    private final ObservableList<Ligne> donnees = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colonne(colReference, l -> l.incident().getReference(), "cell-strong");
        colonne(colMission, Ligne::mission, "cell-normal");
        colonne(colType, l -> l.incident().getType() == null ? "—" : l.incident().getType().getLibelle(),
                "cell-normal");
        colonne(colGravite, l -> l.incident().getGravite() == null ? "—" : l.incident().getGravite().getLibelle(),
                "cell-normal");
        colonne(colDescription, l -> l.incident().getDescription(), "cell-normal");
        colonne(colDate, Ligne::date, "cell-normal");
        colStatut.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colStatut.setCellFactory(col -> DispatcherUi.badge(Ligne::statut,
                l -> l.incident().getStatut() == StatutIncident.OUVERT ? "badge-red" : "badge-green"));
        tableIncidents.setItems(donnees);

        if (DispatcherUi.lectureSeule()) {
            DispatcherUi.masquer(boutonDeclarer);
        }
        rafraichir();
    }

    @FXML
    private void declarer() {
        Ligne selection = tableIncidents.getSelectionModel().getSelectedItem();
        Mission mission = selection == null ? null : mission(selection.incident().getMissionId());
        if (DispatcherUi.<IncidentFormulaireController>ouvrirFormulaire(tableIncidents,
                IncidentFormulaireController.FXML, f -> f.preparer(mission))) {
            rafraichir();
        }
    }

    /** Kept for the assistance / customer actions that are still out of this iteration's scope. */
    @FXML
    private void pasEncoreLivre() {
        DispatcherUi.pasEncoreLivre();
    }

    private void rafraichir() {
        Map<Long, Mission> missions = AppContext.missionService().lister().stream()
                .collect(Collectors.toMap(Mission::getId, Function.identity()));
        var incidents = service.lister();
        donnees.setAll(incidents.stream().map(i -> ligne(i, missions)).toList());
        long ouverts = incidents.stream().filter(i -> i.getStatut() == StatutIncident.OUVERT).count();
        nombreOuverts.setText(ouverts + (ouverts > 1 ? " ouverts" : " ouvert"));
        tableIncidents.refresh();
    }

    private static Ligne ligne(Incident incident, Map<Long, Mission> missions) {
        Mission mission = missions.get(incident.getMissionId());
        String reference = mission == null ? "—" : mission.getReference();
        String date = incident.getDateSignalement() == null ? "—"
                : DispatcherUi.momentRelatif(incident.getDateSignalement(), LocalDate.now());
        String statut = incident.getStatut() == null ? "—" : incident.getStatut().getLibelle();
        return new Ligne(incident, reference, date, statut);
    }

    private static Mission mission(Long id) {
        if (id == null) {
            return null;
        }
        return AppContext.missionService().lister().stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
    }

    private static void colonne(TableColumn<Ligne, String> colonne, Function<Ligne, String> valeur,
                                String styleClass) {
        colonne.setCellValueFactory(c -> new SimpleStringProperty(valeur.apply(c.getValue())));
        colonne.setCellFactory(col -> DispatcherUi.texte(styleClass));
    }
}
