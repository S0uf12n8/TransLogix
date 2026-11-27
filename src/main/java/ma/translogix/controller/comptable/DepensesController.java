package ma.translogix.controller.comptable;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Depense;
import ma.translogix.model.Employe;
import ma.translogix.model.StatutDepense;
import ma.translogix.model.TypeDepense;
import ma.translogix.service.DepenseService;
import ma.translogix.util.Formats;
import ma.translogix.util.ValidationException;

/**
 * "Valider dépenses": the accountant reviews real receipts submitted against real missions.
 * No business rule and no SQL here: every change goes through DepenseService.
 */
public class DepensesController {

    private static final String TOUS_LES_STATUTS = "Tous les statuts";
    private static final String TOUS_LES_TYPES = "Tous les types";

    /**
     * The app switches to JDBC in AppContext, which is not ours to edit: until then this page owns its
     * in-memory store and shares the same service instance with its dialogs, so a dépense entered during
     * the session is still there when the user comes back to the page.
     */
    private static final DepenseService SERVICE = AppContext.depenseService();

    @FXML private Label kpiEnAttente;
    @FXML private Label kpiMontantAttente;
    @FXML private Label kpiValidees;
    @FXML private Label kpiRefusees;
    @FXML private Button boutonNouveau;
    @FXML private Button boutonActualiser;
    @FXML private TextField recherche;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtreType;
    @FXML private ComboBox<Employe> filtreChauffeur;
    @FXML private DatePicker filtreDu;
    @FXML private DatePicker filtreAu;
    @FXML private Button boutonReset;
    @FXML private TableView<Depense> table;
    @FXML private TableColumn<Depense, String> colMission;
    @FXML private TableColumn<Depense, String> colChauffeur;
    @FXML private TableColumn<Depense, String> colType;
    @FXML private TableColumn<Depense, String> colStation;
    @FXML private TableColumn<Depense, String> colMontant;
    @FXML private TableColumn<Depense, String> colDate;
    @FXML private TableColumn<Depense, String> colStatut;
    @FXML private TableColumn<Depense, Depense> colActions;

    private final ObservableList<Depense> donnees = FXCollections.observableArrayList();
    private final Map<Long, String> referencesMissions = new HashMap<>();
    private final Map<Long, String> nomsChauffeurs = new HashMap<>();
    private FilteredList<Depense> filtres;

    @FXML
    private void initialize() {
        colMission.setCellValueFactory(c -> new SimpleStringProperty(referencesMissions.getOrDefault(
                c.getValue().getMissionId(), "—")));
        colMission.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colChauffeur.setCellValueFactory(c -> new SimpleStringProperty(nomsChauffeurs.getOrDefault(
                c.getValue().getChauffeurId(), "—")));
        colChauffeur.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colType.setCellValueFactory(c -> new SimpleStringProperty(libelleType(c.getValue())));
        colType.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colStation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStation()));
        colStation.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colMontant.setCellValueFactory(c -> new SimpleStringProperty(Formats.mad(c.getValue().getMontant())));
        colMontant.setCellFactory(col -> ComptableUi.texte("cell-strong"));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(Formats.date(c.getValue().getDate())));
        colDate.setCellFactory(col -> ComptableUi.texte("cell-normal"));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(DepenseService.libelleStatut(c.getValue().getStatut())));
        colStatut.setCellFactory(col -> ComptableUi.badge(DepensesController::classeStatut));
        colActions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button valider = new Button("Valider");
            private final Button refuser = new Button("Refuser");
            private final HBox boutons = new HBox(6, valider, refuser);

            {
                valider.getStyleClass().add("link-orange");
                refuser.getStyleClass().add("link-red");
                valider.setMinWidth(Region.USE_PREF_SIZE);
                refuser.setMinWidth(Region.USE_PREF_SIZE);
                valider.setOnAction(e -> valider(getItem()));
                refuser.setOnAction(e -> refuser(getItem()));
            }

            @Override
            protected void updateItem(Depense d, boolean empty) {
                super.updateItem(d, empty);
                setText(null);
                boolean modifiable = estModifiable(d);
                valider.setVisible(modifiable);
                valider.setManaged(modifiable);
                refuser.setVisible(modifiable);
                refuser.setManaged(modifiable);
                setGraphic(modifiable ? boutons : null);
            }
        });
        table.setRowFactory(tv -> ligneAvecMenu());

        filtres = new FilteredList<>(donnees);
        SortedList<Depense> tries = new SortedList<>(filtres);
        tries.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(tries);

        preparerFiltres();
        if (ComptableUi.lectureSeule()) {
            ComptableUi.masquer(boutonNouveau);
        }
        rafraichir();
    }

    // ------------------------------------------------------------------ SEARCH

    private void preparerFiltres() {
        filtreStatut.setItems(FXCollections.observableArrayList(TOUS_LES_STATUTS,
                StatutDepense.EN_ATTENTE.getLibelle(), StatutDepense.VALIDEE.getLibelle(),
                StatutDepense.REFUSEE.getLibelle()));
        filtreStatut.setValue(TOUS_LES_STATUTS);

        filtreType.setItems(FXCollections.observableArrayList(TOUS_LES_TYPES));
        filtreType.getItems().addAll(java.util.Arrays.stream(TypeDepense.values()).map(TypeDepense::getLibelle).toList());
        filtreType.setValue(TOUS_LES_TYPES);

        filtreChauffeur.setItems(FXCollections.observableArrayList(AppContext.employeService().listerChauffeurs()));
        filtreChauffeur.setValue(null);
        filtreChauffeur.setPromptText("Tous les chauffeurs");
        filtreChauffeur.setConverter(new StringConverter<>() {
            @Override
            public String toString(Employe e) {
                return e == null ? "" : DepenseService.nom(e);
            }

            @Override
            public Employe fromString(String texte) {
                return null;
            }
        });

        for (DatePicker date : new DatePicker[] {filtreDu, filtreAu}) {
            date.setConverter(new StringConverter<>() {
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
        }

        recherche.textProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
        filtreStatut.valueProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
        filtreType.valueProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
        filtreChauffeur.valueProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
        filtreDu.valueProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
        filtreAu.valueProperty().addListener((obs, ancien, valeur) -> appliquerFiltres());
    }

    @FXML
    private void reinitialiser() {
        recherche.clear();
        filtreStatut.setValue(TOUS_LES_STATUTS);
        filtreType.setValue(TOUS_LES_TYPES);
        filtreChauffeur.setValue(null);
        filtreDu.setValue(null);
        filtreAu.setValue(null);
        appliquerFiltres();
    }

    @FXML
    private void actualiser() {
        rafraichir();
    }

    /** The service owns the filtering rule; the table simply keeps the matching ids. */
    private void appliquerFiltres() {
        try {
            Set<Long> correspondants = SERVICE.rechercher(critere()).stream()
                    .map(Depense::getId)
                    .collect(Collectors.toSet());
            filtres.setPredicate(d -> correspondants.contains(d.getId()));
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
    }

    private DepenseService.CritereDepense critere() {
        Employe chauffeur = filtreChauffeur.getValue();
        return new DepenseService.CritereDepense(recherche.getText(), statutFiltre(), typeFiltre(),
                chauffeur == null ? null : chauffeur.getId(), filtreDu.getValue(), filtreAu.getValue());
    }

    private StatutDepense statutFiltre() {
        String valeur = filtreStatut.getValue();
        if (valeur == null || valeur.equals(TOUS_LES_STATUTS)) {
            return null;
        }
        return java.util.Arrays.stream(StatutDepense.values())
                .filter(s -> s.getLibelle().equals(valeur)).findFirst().orElse(null);
    }

    private TypeDepense typeFiltre() {
        String valeur = filtreType.getValue();
        if (valeur == null || valeur.equals(TOUS_LES_TYPES)) {
            return null;
        }
        return java.util.Arrays.stream(TypeDepense.values())
                .filter(t -> t.getLibelle().equals(valeur)).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ ACTIONS

    @FXML
    private void nouveau() {
        if (ComptableUi.<DepenseFormulaireController>ouvrirFormulaire(table, DepenseFormulaireController.FXML,
                f -> f.preparer(SERVICE))) {
            rafraichir();
        }
    }

    private void valider(Depense depense) {
        if (!estModifiable(depense)) {
            return;
        }
        if (!Dialogues.confirmer("Valider la dépense", "Valider la dépense de "
                + Formats.mad(depense.getMontant()) + " (« " + depense.getStation() + " ») ?")) {
            return;
        }
        try {
            SERVICE.valider(depense.getId());
            rafraichir();
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
    }

    private void refuser(Depense depense) {
        if (!estModifiable(depense)) {
            return;
        }
        if (ComptableUi.<DepenseRefusController>ouvrirFormulaire(table, DepenseRefusController.FXML,
                f -> f.preparer(SERVICE, depense))) {
            rafraichir();
        }
    }

    private void voirDetails(Depense depense) {
        if (depense == null) {
            return;
        }
        StringBuilder texte = new StringBuilder()
                .append("Mission : ").append(referencesMissions.getOrDefault(depense.getMissionId(), "—")).append('\n')
                .append("Chauffeur : ").append(nomsChauffeurs.getOrDefault(depense.getChauffeurId(), "—")).append('\n')
                .append("Type : ").append(libelleType(depense)).append('\n')
                .append("Lieu : ").append(depense.getStation()).append('\n')
                .append("Montant : ").append(Formats.mad(depense.getMontant())).append('\n')
                .append("Litres : ").append(depense.getLitres() == null ? "—" : depense.getLitres() + " L").append('\n')
                .append("Date : ").append(Formats.date(depense.getDate())).append('\n')
                .append("Statut : ").append(DepenseService.libelleStatut(depense.getStatut()));
        if (depense.getMotifRefus() != null) {
            texte.append("\nMotif du refus : ").append(depense.getMotifRefus());
        }
        Dialogues.info("Détail de la dépense", texte.toString());
    }

    private TableRow<Depense> ligneAvecMenu() {
        TableRow<Depense> ligne = new TableRow<>();
        MenuItem details = new MenuItem("Voir détails");
        MenuItem valider = new MenuItem("Valider…");
        MenuItem refuser = new MenuItem("Refuser…");
        refuser.getStyleClass().add("menu-danger");
        details.setOnAction(e -> voirDetails(ligne.getItem()));
        valider.setOnAction(e -> valider(ligne.getItem()));
        refuser.setOnAction(e -> refuser(ligne.getItem()));
        ContextMenu menu = new ContextMenu(details, valider, refuser);
        menu.setOnShowing(e -> {
            boolean modifiable = estModifiable(ligne.getItem());
            valider.setVisible(modifiable);
            refuser.setVisible(modifiable);
        });
        ligne.emptyProperty().addListener((obs, avant, vide) -> ligne.setContextMenu(vide ? null : menu));
        ligne.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !ligne.isEmpty()) {
                voirDetails(ligne.getItem());
            }
        });
        return ligne;
    }

    private static boolean estModifiable(Depense depense) {
        return depense != null && depense.getStatut() == StatutDepense.EN_ATTENTE && !ComptableUi.lectureSeule();
    }

    private void rafraichir() {
        referencesMissions.clear();
        AppContext.missionService().lister().forEach(m -> referencesMissions.put(m.getId(),
                m.getReference() + " · " + m.getItineraire()));
        nomsChauffeurs.clear();
        AppContext.employeService().lister().forEach(e -> nomsChauffeurs.put(e.getId(), DepenseService.nom(e)));
        donnees.setAll(SERVICE.lister());
        DepenseService.Synthese synthese = SERVICE.synthese();
        kpiEnAttente.setText(String.valueOf(synthese.enAttente()));
        kpiMontantAttente.setText(Formats.mad(synthese.montantEnAttente()));
        kpiValidees.setText(Formats.mad(synthese.montantValide()));
        kpiRefusees.setText(String.valueOf(synthese.refusees()));
        appliquerFiltres();
    }

    private static String libelleType(Depense depense) {
        return depense.getType() == null ? "—" : depense.getType().getLibelle();
    }

    private static String classeStatut(String libelle) {
        return switch (libelle) {
            case "Validée" -> "badge-green";
            case "Refusée" -> "badge-red";
            default -> "badge-blue"; // En attente
        };
    }
}
