package ma.translogix.controller.dispatcher;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ma.translogix.AppContext;
import ma.translogix.model.Client;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutVehicule;
import ma.translogix.model.Vehicule;
import ma.translogix.service.MissionService;

/** Fields of the mission dialog. Every rule (overlap included) stays in MissionService. */
public class MissionFormulaireController implements DispatcherUi.Formulaire {
    static final String FXML = "dispatcher/mission-dialog.fxml";

    @FXML private Label titre;
    @FXML private Label description;
    @FXML private Label note;
    @FXML private ComboBox<Client> champClient;
    @FXML private ComboBox<Vehicule> champVehicule;
    @FXML private ComboBox<Employe> champChauffeur;
    @FXML private TextField champDepart;
    @FXML private TextField champArrivee;
    @FXML private TextField champCargaison;
    @FXML private TextField champPoids;
    @FXML private DatePicker champDateDebut;
    @FXML private TextField champHeureDebut;
    @FXML private DatePicker champDateFin;
    @FXML private TextField champHeureFin;

    private final MissionService service = AppContext.missionService();
    /** null when creating a new mission. */
    private Mission editee;

    @FXML
    private void initialize() {
        champClient.getItems().setAll(AppContext.clientService().lister());
        champClient.setConverter(libelles(Client::getNom));
        // Every vehicle is listed with its state, so the dispatcher sees why a truck in maintenance is refused.
        champVehicule.getItems().setAll(AppContext.vehiculeService().lister());
        champVehicule.setConverter(libelles(v -> MissionsController.vehicule(v)
                + (v.getStatut() == StatutVehicule.EN_SERVICE ? "" : " (" + v.getStatut().getLibelle().toLowerCase() + ")")));
        champChauffeur.getItems().setAll(AppContext.employeService().listerChauffeurs());
        champChauffeur.setConverter(libelles(Employe::getNomComplet));
        DispatcherUi.formatDate(champDateDebut);
        DispatcherUi.formatDate(champDateFin);
    }

    /**
     * Fills the form for an edit (mission != null), or prepares a creation,
     * optionally for one driver (the planning page's "Assigner trajet").
     */
    void preparer(Mission mission, Employe chauffeur) {
        editee = mission;
        if (mission == null) {
            if (chauffeur != null) {
                selectionner(champChauffeur, chauffeur.getId(), Employe::getId);
            }
            return;
        }
        titre.setText("Modifier " + mission.getReference());
        description.setText("Mission planifiée : changez l'affectation, l'itinéraire ou les horaires.");
        note.setText("La référence ne change pas. Un camion ou un chauffeur déjà affecté sur la période est refusé.");
        selectionner(champClient, mission.getClientId(), Client::getId);
        selectionner(champVehicule, mission.getVehiculeId(), Vehicule::getId);
        selectionner(champChauffeur, mission.getChauffeurId(), Employe::getId);
        champDepart.setText(mission.getVilleDepart());
        champArrivee.setText(mission.getVilleArrivee());
        champCargaison.setText(mission.getCargaison());
        champPoids.setText(mission.getPoidsTonnes() == null ? ""
                : mission.getPoidsTonnes().stripTrailingZeros().toPlainString().replace('.', ','));
        champDateDebut.setValue(mission.getDateDebut().toLocalDate());
        champHeureDebut.setText(DispatcherUi.heure(mission.getDateDebut()));
        champDateFin.setValue(mission.getDateFin().toLocalDate());
        champHeureFin.setText(DispatcherUi.heure(mission.getDateFin()));
    }

    @Override
    public void enregistrer() {
        LocalDateTime debut = DispatcherUi.lireDateHeure(champDateDebut, champHeureDebut, "de départ");
        LocalDateTime fin = DispatcherUi.lireDateHeure(champDateFin, champHeureFin, "d'arrivée");
        var poids = DispatcherUi.lireTonnes(champPoids, "Poids");
        Long client = champClient.getValue() == null ? null : champClient.getValue().getId();
        Long vehicule = champVehicule.getValue() == null ? null : champVehicule.getValue().getId();
        Long chauffeur = champChauffeur.getValue() == null ? null : champChauffeur.getValue().getId();
        if (editee == null) {
            service.creer(client, vehicule, chauffeur, champDepart.getText(), champArrivee.getText(),
                    champCargaison.getText(), poids, debut, fin);
        } else {
            service.modifier(editee.getId(), client, vehicule, chauffeur, champDepart.getText(),
                    champArrivee.getText(), champCargaison.getText(), poids, debut, fin);
        }
    }

    @Override
    public String titre() {
        return titre.getText();
    }

    private static <T> void selectionner(ComboBox<T> champ, Long id, Function<T, Long> idDe) {
        champ.getItems().stream().filter(t -> Objects.equals(idDe.apply(t), id)).findFirst().ifPresent(champ::setValue);
    }

    private static <T> StringConverter<T> libelles(Function<T, String> libelle) {
        return new StringConverter<>() {
            @Override
            public String toString(T objet) {
                return objet == null ? "" : libelle.apply(objet);
            }

            @Override
            public T fromString(String texte) {
                return null; // not editable
            }
        };
    }
}
