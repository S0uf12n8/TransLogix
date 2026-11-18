package ma.translogix.controller.dispatcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Client;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;
import ma.translogix.model.StatutVehicule;
import ma.translogix.model.Vehicule;
import ma.translogix.service.MissionService;
import ma.translogix.util.Recherche;

/** "Ordres & Trajets" ([H5]): Hamid's orders table on real missions, with the status actions of MissionService. */
public class MissionsController {

    /** One mission with the names it shows (looked up once per refresh). */
    record Ligne(Mission mission, String chauffeur, String vehicule, String client) {
    }

    @FXML private Button boutonNouvelle;
    @FXML private Label valeurEnCours;
    @FXML private Label piedEnCours;
    @FXML private Label valeurDispo;
    @FXML private Label totalVehicules;
    @FXML private ProgressBar progressionDispo;
    @FXML private TextField recherche;
    @FXML private TableView<Ligne> table;
    @FXML private TableColumn<Ligne, String> colReference;
    @FXML private TableColumn<Ligne, Ligne> colChauffeur;
    @FXML private TableColumn<Ligne, Ligne> colItineraire;
    @FXML private TableColumn<Ligne, String> colCargaison;
    @FXML private TableColumn<Ligne, Ligne> colEta;
    @FXML private TableColumn<Ligne, Ligne> colStatut;
    @FXML private TableColumn<Ligne, Ligne> colActions;

    private final MissionService service = AppContext.missionService();
    private final ObservableList<Ligne> donnees = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colReference.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mission().getReference()));
        colReference.setCellFactory(col -> DispatcherUi.texte("cell-strong"));
        colChauffeur.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colChauffeur.setCellFactory(col -> DispatcherUi.doubleLigne(Ligne::chauffeur, Ligne::vehicule));
        colItineraire.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colItineraire.setCellFactory(col -> DispatcherUi.doubleLigne(l -> l.mission().getItineraire(), Ligne::client));
        colCargaison.setCellValueFactory(c -> new SimpleStringProperty(cargaison(c.getValue().mission())));
        colCargaison.setCellFactory(col -> DispatcherUi.texte("cell-normal"));
        colEta.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colEta.setCellFactory(col -> DispatcherUi.doubleLigne(
                l -> DispatcherUi.momentRelatif(l.mission().getDateFin(), LocalDate.now()),
                l -> "Départ " + DispatcherUi.momentRelatif(l.mission().getDateDebut(), LocalDate.now())));
        colStatut.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colStatut.setCellFactory(col -> DispatcherUi.badge(l -> l.mission().getStatut().getLibelle(),
                l -> DispatcherUi.classeStatut(l.mission().getStatut())));
        colActions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colActions.setCellFactory(col -> DispatcherUi.liens(this::actions));

        FilteredList<Ligne> filtres = new FilteredList<>(donnees);
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(l -> Recherche.correspond(requete, l.mission().getReference(), l.chauffeur(),
                        l.vehicule(), l.client(), l.mission().getVilleDepart(), l.mission().getVilleArrivee(),
                        l.mission().getCargaison())));
        table.setItems(filtres);

        if (DispatcherUi.lectureSeule()) {
            DispatcherUi.masquer(boutonNouvelle);
        }
        rafraichir();
    }

    @FXML
    private void nouvelle() {
        ouvrirFormulaire(null);
    }

    @FXML
    private void exporter() {
        DispatcherUi.pasEncoreLivre();
    }

    /** dispatcher.html actions, mapped to the lifecycle: planned -> edit / start / cancel, in progress -> delivered / cancel. */
    private List<DispatcherUi.Lien> actions(Ligne l) {
        Mission m = l.mission();
        List<DispatcherUi.Lien> liens = new ArrayList<>();
        if (m.getStatut() == StatutMission.PLANIFIEE) {
            liens.add(new DispatcherUi.Lien("Éditer", "link-blue", () -> ouvrirFormulaire(m)));
            liens.add(new DispatcherUi.Lien("Démarrer", "link-orange", () -> changer(() -> service.demarrer(m.getId()))));
        }
        if (m.getStatut() == StatutMission.EN_COURS) {
            liens.add(new DispatcherUi.Lien("Livrée", "link-green", () -> changer(() -> service.livrer(m.getId()))));
        }
        if (m.getStatut() == StatutMission.PLANIFIEE || m.getStatut() == StatutMission.EN_COURS) {
            liens.add(new DispatcherUi.Lien("Annuler", "link-red", () -> annuler(m)));
        }
        return liens;
    }

    private void annuler(Mission m) {
        if (Dialogues.confirmer("Annuler la mission", "Annuler la mission " + m.getReference() + " ("
                + m.getItineraire() + ") ? Le camion et le chauffeur redeviennent libres sur cette période.")) {
            changer(() -> service.annuler(m.getId()));
        }
    }

    private void changer(Runnable action) {
        if (DispatcherUi.executer(action)) {
            rafraichir();
        }
    }

    private void ouvrirFormulaire(Mission mission) {
        if (DispatcherUi.<MissionFormulaireController>ouvrirFormulaire(table, MissionFormulaireController.FXML,
                f -> f.preparer(mission, null))) {
            rafraichir();
        }
    }

    private void rafraichir() {
        Map<Long, Vehicule> vehicules = AppContext.vehiculeService().lister().stream()
                .collect(Collectors.toMap(Vehicule::getId, Function.identity()));
        Map<Long, Employe> employes = AppContext.employeService().lister().stream()
                .collect(Collectors.toMap(Employe::getId, Function.identity()));
        Map<Long, Client> clients = AppContext.clientService().lister().stream()
                .collect(Collectors.toMap(Client::getId, Function.identity()));
        List<Mission> missions = service.lister();

        donnees.setAll(missions.stream().map(m -> new Ligne(m,
                nom(employes.get(m.getChauffeurId())),
                vehicule(vehicules.get(m.getVehiculeId())),
                clients.containsKey(m.getClientId()) ? clients.get(m.getClientId()).getNom() : "—")).toList());
        table.refresh();

        long enCours = service.compter(StatutMission.EN_COURS);
        long planifiees = service.compter(StatutMission.PLANIFIEE);
        valeurEnCours.setText(String.valueOf(enCours));
        piedEnCours.setText(planifiees == 0 ? "Aucune mission planifiée"
                : planifiees + (planifiees > 1 ? " missions planifiées" : " mission planifiée"));

        long enService = vehicules.values().stream().filter(v -> v.getStatut() == StatutVehicule.EN_SERVICE).count();
        long occupes = missions.stream().filter(m -> m.getStatut() == StatutMission.EN_COURS)
                .map(Mission::getVehiculeId).distinct().count();
        long dispo = Math.max(0, enService - occupes);
        valeurDispo.setText(String.valueOf(dispo));
        totalVehicules.setText("/ " + vehicules.size());
        progressionDispo.setProgress(vehicules.isEmpty() ? 0 : (double) dispo / vehicules.size());
    }

    static String nom(Employe e) {
        return e == null ? "—" : e.getNomComplet();
    }

    /** "12345-A-6 · Volvo FH". */
    static String vehicule(Vehicule v) {
        if (v == null) {
            return "—";
        }
        return v.getDesignation().isEmpty() ? v.getMatricule() : v.getMatricule() + " · " + v.getDesignation();
    }

    /** "24 T · Produits frais", as in the mockup. */
    private static String cargaison(Mission m) {
        String poids = DispatcherUi.tonnes(m.getPoidsTonnes()).replace(" t", " T");
        return m.getCargaison() == null ? poids : poids + " · " + m.getCargaison();
    }
}
