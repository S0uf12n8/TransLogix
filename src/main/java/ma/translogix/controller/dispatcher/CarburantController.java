package ma.translogix.controller.dispatcher;

import java.math.BigDecimal;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.translogix.AppContext;
import ma.translogix.model.BonCarburant;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutBonCarburant;
import ma.translogix.model.Vehicule;
import ma.translogix.service.BonCarburantService;
import ma.translogix.util.Formats;
import ma.translogix.util.Recherche;

/** "Bons de Gazole & Cartes Autoroute": real vouchers written through BonCarburantService. */
public class CarburantController {

    /** One voucher with the names it shows (looked up once per refresh). */
    record Ligne(BonCarburant bon, String mission, String camion, String chauffeur, String litres, String montant,
                 String statut) {
    }

    @FXML private Button boutonEmettre;
    @FXML private TextField recherche;
    @FXML private TableView<Ligne> table;
    @FXML private TableColumn<Ligne, String> colBon;
    @FXML private TableColumn<Ligne, String> colCamion;
    @FXML private TableColumn<Ligne, String> colChauffeur;
    @FXML private TableColumn<Ligne, String> colStation;
    @FXML private TableColumn<Ligne, String> colLitres;
    @FXML private TableColumn<Ligne, String> colMontant;
    @FXML private TableColumn<Ligne, Ligne> colStatut;

    private final BonCarburantService service = DispatcherServices.bons();
    private final ObservableList<Ligne> donnees = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colonne(colBon, l -> l.bon().getReference(), "cell-strong");
        colonne(colCamion, Ligne::camion, "cell-normal");
        colonne(colChauffeur, Ligne::chauffeur, "cell-normal");
        colonne(colStation, l -> l.bon().getStation(), "cell-normal");
        colonne(colLitres, Ligne::litres, "cell-normal");
        colonne(colMontant, Ligne::montant, "cell-strong");
        colStatut.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colStatut.setCellFactory(col -> DispatcherUi.badge(Ligne::statut,
                l -> l.bon().getStatut() == StatutBonCarburant.VALIDE ? "badge-green" : "badge-amber"));

        FilteredList<Ligne> filtres = new FilteredList<>(donnees);
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(l -> Recherche.correspond(requete, l.bon().getReference(), l.mission(),
                        l.chauffeur(), l.camion(), l.bon().getStation())));
        table.setItems(filtres);

        if (DispatcherUi.lectureSeule()) {
            DispatcherUi.masquer(boutonEmettre);
        }
        rafraichir();
    }

    @FXML
    private void emettre() {
        Ligne selection = table.getSelectionModel().getSelectedItem();
        Mission mission = selection == null ? null : mission(selection.bon().getMissionId());
        if (DispatcherUi.<BonCarburantFormulaireController>ouvrirFormulaire(table,
                BonCarburantFormulaireController.FXML, f -> f.preparer(mission))) {
            rafraichir();
        }
    }

    private void rafraichir() {
        Map<Long, Mission> missions = AppContext.missionService().lister().stream()
                .collect(Collectors.toMap(Mission::getId, Function.identity()));
        Map<Long, Vehicule> vehicules = AppContext.vehiculeService().lister().stream()
                .collect(Collectors.toMap(Vehicule::getId, Function.identity()));
        Map<Long, Employe> employes = AppContext.employeService().lister().stream()
                .collect(Collectors.toMap(Employe::getId, Function.identity()));
        donnees.setAll(service.lister().stream().map(b -> ligne(b, missions, vehicules, employes)).toList());
        table.refresh();
    }

    private static Ligne ligne(BonCarburant bon, Map<Long, Mission> missions, Map<Long, Vehicule> vehicules,
                               Map<Long, Employe> employes) {
        Mission mission = missions.get(bon.getMissionId());
        Vehicule vehicule = mission == null ? null : vehicules.get(mission.getVehiculeId());
        Employe chauffeur = mission == null ? null : employes.get(mission.getChauffeurId());
        String camion = vehicule == null ? "—" : vehicule.getMatricule();
        String nomChauffeur = chauffeur == null ? "—" : chauffeur.getNomComplet();
        String referenceMission = mission == null ? "—" : mission.getReference();
        return new Ligne(bon, referenceMission, camion, nomChauffeur, litres(bon.getLitres()),
                Formats.mad(bon.getMontant()), bon.getStatut() == null ? "—" : bon.getStatut().getLibelle());
    }

    private static Mission mission(Long id) {
        if (id == null) {
            return null;
        }
        return AppContext.missionService().lister().stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
    }

    private static String litres(BigDecimal valeur) {
        if (valeur == null) {
            return "—";
        }
        return valeur.stripTrailingZeros().toPlainString().replace('.', ',') + " L";
    }

    private static void colonne(TableColumn<Ligne, String> colonne, Function<Ligne, String> valeur,
                                String styleClass) {
        colonne.setCellValueFactory(c -> new SimpleStringProperty(valeur.apply(c.getValue())));
        colonne.setCellFactory(col -> DispatcherUi.texte(styleClass));
    }
}
