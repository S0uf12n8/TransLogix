package ma.translogix.controller.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.translogix.AppContext;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.ResultatAudit;
import ma.translogix.service.AuditService;
import ma.translogix.service.AuditService.CritereAudit;
import ma.translogix.util.ValidationException;

/** Read-only view of the audit log, most recent first, with a multi-criteria search (text, module, result, period). */
public class AuditController {

    private static final String TOUS_LES_MODULES = "Tous les modules";
    private static final String TOUS_LES_RESULTATS = "Tous les résultats";

    @FXML private TextField recherche;
    @FXML private ComboBox<String> filtreModule;
    @FXML private ComboBox<String> filtreResultat;
    @FXML private DatePicker du;
    @FXML private DatePicker au;
    @FXML private Label erreur;
    @FXML private Label compteur;
    @FXML private Label videTexte;
    @FXML private TableView<EvenementAudit> table;
    @FXML private TableColumn<EvenementAudit, String> colDate;
    @FXML private TableColumn<EvenementAudit, String> colUtilisateur;
    @FXML private TableColumn<EvenementAudit, String> colModule;
    @FXML private TableColumn<EvenementAudit, String> colAction;
    @FXML private TableColumn<EvenementAudit, String> colResultat;

    private final AuditService service = AppContext.auditService();
    private List<EvenementAudit> tous = List.of();
    private FilteredList<EvenementAudit> filtres;

    @FXML
    private void initialize() {
        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getHorodatage().format(AuditService.FORMAT_DATE)));
        colUtilisateur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUtilisateur()));
        colUtilisateur.setCellFactory(col -> texteGras());
        colModule.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getModule()));
        colAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        colResultat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getResultat().getLibelle()));
        colResultat.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String resultat, boolean empty) {
                super.updateItem(resultat, empty);
                setText(null);
                if (empty || resultat == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label("● " + resultat);
                badge.getStyleClass().add(ResultatAudit.REUSSI.getLibelle().equals(resultat)
                        ? "badge-green" : "badge-red");
                setGraphic(badge);
            }
        });

        List<String> modules = new ArrayList<>();
        modules.add(TOUS_LES_MODULES);
        modules.addAll(service.modules());
        filtreModule.setItems(FXCollections.observableArrayList(modules));
        List<String> resultats = new ArrayList<>();
        resultats.add(TOUS_LES_RESULTATS);
        Arrays.stream(ResultatAudit.values()).map(ResultatAudit::getLibelle).forEach(resultats::add);
        filtreResultat.setItems(FXCollections.observableArrayList(resultats));
        filtreModule.getSelectionModel().select(TOUS_LES_MODULES);
        filtreResultat.getSelectionModel().select(TOUS_LES_RESULTATS);
        ChampDate.configurer(du, this::montrerErreur);
        ChampDate.configurer(au, this::montrerErreur);

        tous = service.lister();
        filtres = new FilteredList<>(FXCollections.observableArrayList(tous));
        recherche.textProperty().addListener((obs, avant, apres) -> appliquerFiltres());
        filtreModule.valueProperty().addListener((obs, avant, apres) -> appliquerFiltres());
        filtreResultat.valueProperty().addListener((obs, avant, apres) -> appliquerFiltres());
        du.valueProperty().addListener((obs, avant, apres) -> appliquerFiltres());
        au.valueProperty().addListener((obs, avant, apres) -> appliquerFiltres());
        table.setItems(filtres);
        appliquerFiltres();
    }

    /** All criteria combined (AND); the rule itself lives in AuditService.correspond(event, criteria). */
    private void appliquerFiltres() {
        String module = filtreModule.getValue();
        ResultatAudit resultat = Arrays.stream(ResultatAudit.values())
                .filter(r -> r.getLibelle().equals(filtreResultat.getValue())).findFirst().orElse(null);
        CritereAudit critere = new CritereAudit(recherche.getText(),
                module == null || TOUS_LES_MODULES.equals(module) ? null : module,
                resultat, du.getValue(), au.getValue());
        try {
            AuditService.verifierPeriode(critere);
        } catch (ValidationException ex) {
            montrerErreur(ex.getMessage());
            filtres.setPredicate(e -> false);
            majCompteur(0);
            return;
        }
        montrerErreur(null);
        filtres.setPredicate(e -> AuditService.correspond(e, critere));
        videTexte.setText(tous.isEmpty()
                ? "Aucune action enregistrée pour cette session."
                : "Aucun événement ne correspond aux filtres.");
        majCompteur(filtres.size());
    }

    @FXML
    private void reinitialiser() {
        recherche.clear();
        filtreModule.getSelectionModel().select(TOUS_LES_MODULES);
        filtreResultat.getSelectionModel().select(TOUS_LES_RESULTATS);
        du.setValue(null);
        au.setValue(null);
    }

    /** null hides the message. */
    private void montrerErreur(String message) {
        erreur.setText(message == null ? "" : message);
        erreur.setVisible(message != null);
        erreur.setManaged(message != null);
    }

    private void majCompteur(int n) {
        compteur.setText(n + (n > 1 ? " événements" : " événement") + " sur " + tous.size());
    }

    private static TableCell<EvenementAudit, String> texteGras() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().remove("cell-strong");
                if (!empty) {
                    getStyleClass().add("cell-strong");
                }
            }
        };
    }
}
