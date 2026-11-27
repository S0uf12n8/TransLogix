package ma.translogix.controller.dispatcher;

import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import ma.translogix.AppContext;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;
import ma.translogix.model.StatutVehicule;
import ma.translogix.model.Vehicule;
import ma.translogix.service.MissionService;
import ma.translogix.service.VehiculeService;
import ma.translogix.util.Recherche;

/** "Suivi Flotte" ([H3]): vehicles, their mission in progress, add / edit / change status. */
public class FlotteController {

    /** One vehicle with its mission in progress (looked up once per refresh). */
    record Ligne(Vehicule vehicule, Optional<Mission> mission) {
    }

    @FXML private Button boutonAjouter;
    @FXML private Label valeurEnService;
    @FXML private Label totalVehicules;
    @FXML private Label valeurMaintenance;
    @FXML private Label horsService;
    @FXML private Label valeurMissions;
    @FXML private TextField recherche;
    @FXML private TableView<Ligne> table;
    @FXML private TableColumn<Ligne, Ligne> colVehicule;
    @FXML private TableColumn<Ligne, String> colCapacite;
    @FXML private TableColumn<Ligne, String> colPosition;
    @FXML private TableColumn<Ligne, Ligne> colStatut;
    @FXML private TableColumn<Ligne, Ligne> colActions;
    @FXML private PieChart graphiqueDisponibilite;
    @FXML private Label sousTitreDisponibilite;

    private final VehiculeService service = AppContext.vehiculeService();
    private final MissionService missions = AppContext.missionService();
    private final ObservableList<Ligne> donnees = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colVehicule.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colVehicule.setCellFactory(col -> DispatcherUi.doubleLigne(l -> l.vehicule().getMatricule(),
                l -> l.vehicule().getDesignation().isEmpty() ? "—" : l.vehicule().getDesignation()));
        colCapacite.setCellValueFactory(c -> new SimpleStringProperty(
                DispatcherUi.tonnes(c.getValue().vehicule().getCapaciteTonnes())));
        colCapacite.setCellFactory(col -> DispatcherUi.texte("cell-normal"));
        colPosition.setCellValueFactory(c -> new SimpleStringProperty(position(c.getValue())));
        colPosition.setCellFactory(col -> DispatcherUi.texte("cell-normal"));
        colStatut.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colStatut.setCellFactory(col -> DispatcherUi.badge(l -> l.vehicule().getStatut().getLibelle(),
                l -> DispatcherUi.classeStatut(l.vehicule().getStatut())));
        colActions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colActions.setCellFactory(col -> DispatcherUi.liens(l -> List.of(
                new DispatcherUi.Lien("Modifier", "link-blue", () -> ouvrirFormulaire(l.vehicule())),
                new DispatcherUi.Lien("Changer statut", "link-orange", () -> changerStatut(l.vehicule())))));

        FilteredList<Ligne> filtres = new FilteredList<>(donnees);
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(l -> Recherche.correspond(requete, l.vehicule().getMatricule(),
                        l.vehicule().getMarque(), l.vehicule().getModele(),
                        l.mission().map(Mission::getReference).orElse(null))));
        table.setItems(filtres);

        if (DispatcherUi.lectureSeule()) {
            DispatcherUi.masquer(boutonAjouter);
        }
        rafraichir();
    }

    @FXML
    private void ajouter() {
        ouvrirFormulaire(null);
    }

    private void ouvrirFormulaire(Vehicule vehicule) {
        if (DispatcherUi.<VehiculeFormulaireController>ouvrirFormulaire(table, VehiculeFormulaireController.FXML,
                f -> f.preparer(vehicule))) {
            rafraichir();
        }
    }

    private void changerStatut(Vehicule v) {
        ChoiceDialog<StatutVehicule> choix = new ChoiceDialog<>(v.getStatut(), StatutVehicule.values());
        choix.setTitle("Statut du véhicule");
        choix.setHeaderText(null);
        choix.setContentText("Statut de " + v.getMatricule() + " :");
        if (table.getScene() != null) {
            choix.initOwner(table.getScene().getWindow());
        }
        choix.showAndWait()
                .filter(statut -> statut != v.getStatut())
                .ifPresent(statut -> {
                    if (DispatcherUi.executer(() -> service.changerStatut(v.getId(), statut))) {
                        rafraichir();
                    }
                });
    }

    private void rafraichir() {
        if (!DispatcherUi.executer(this::actualiserDonnees)) {
            donnees.setAll(List.of());
            table.refresh();
            valeurEnService.setText("—");
            totalVehicules.setText("");
            valeurMaintenance.setText("—");
            horsService.setText("");
            valeurMissions.setText("—");
            graphiqueDisponibilite.getData().clear();
            sousTitreDisponibilite.setText("Données indisponibles");
        }
    }

    private void actualiserDonnees() {
        List<Vehicule> vehicules = service.lister();
        donnees.setAll(vehicules.stream()
                .map(v -> new Ligne(v, missions.missionEnCoursDuVehicule(v.getId()))).toList());
        table.refresh();

        valeurEnService.setText(String.valueOf(service.compter(StatutVehicule.EN_SERVICE)));
        totalVehicules.setText("/ " + vehicules.size());
        valeurMaintenance.setText(String.valueOf(service.compter(StatutVehicule.MAINTENANCE)));
        long hs = service.compter(StatutVehicule.HORS_SERVICE);
        horsService.setText(hs == 0 ? "" : "+ " + hs + " hors service");
        valeurMissions.setText(String.valueOf(missions.compter(StatutMission.EN_COURS)));
        remplirDisponibilite();
    }

    /**
     * "Disponibilité des camions": the service combines the fleet with the vehicles the DAO reports as busy,
     * the controller only turns the returned slices into a pie chart.
     */
    private void remplirDisponibilite() {
        List<MissionService.Part> parties = missions.disponibiliteCamions();
        graphiqueDisponibilite.getData().clear();
        long total = parties.stream().mapToLong(MissionService.Part::nombre).sum();
        for (MissionService.Part partie : parties) {
            long nombre = partie.nombre();
            PieChart.Data tranche = new PieChart.Data(partie.libelle() + " (" + nombre + ")", nombre);
            graphiqueDisponibilite.getData().add(tranche);
            long pourcent = total == 0 ? 0 : Math.round(nombre * 100.0 / total);
            String detail = partie.libelle() + " : " + nombre + " camion(s), " + pourcent + " %";
            DispatcherUi.quandAffiche(tranche.nodeProperty(),
                    noeud -> Tooltip.install(noeud, new Tooltip(detail)));
        }
        sousTitreDisponibilite.setText(total + (total > 1 ? " camions" : " camion") + " · disponibilité");
    }

    /** "TRJ-9081 · Casablanca → Tanger Med", otherwise where a vehicle without a mission is. */
    private static String position(Ligne l) {
        if (l.mission().isPresent()) {
            Mission m = l.mission().get();
            return m.getReference() + " · " + m.getItineraire();
        }
        return switch (l.vehicule().getStatut()) {
            case EN_SERVICE -> "Au dépôt · disponible";
            case MAINTENANCE -> "Atelier";
            case HORS_SERVICE -> "Immobilisé";
        };
    }
}
