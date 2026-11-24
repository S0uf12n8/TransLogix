package ma.translogix.controller.comptable;

import java.time.LocalDate;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.ResultatAudit;
import ma.translogix.service.AuditService;
import ma.translogix.service.AuditService.CritereAudit;
import ma.translogix.util.ValidationException;

/**
 * Journal des opérations : affiche les événements réellement enregistrés
 * par AuditService. Ce journal est une traçabilité opérationnelle,
 * et non un journal comptable débit/crédit.
 */
public class JournalController {

    private static final String TOUS_MODULES = "Tous les modules";
    private static final String TOUS_RESULTATS = "Tous les résultats";
    private static final String REUSSI = "Réussi";
    private static final String REFUSE = "Refusé";

    @FXML private Region kpiTotal;
    @FXML private Region kpiReussis;
    @FXML private Region kpiRefuses;

    @FXML private Label valeurTotal;
    @FXML private Label valeurReussis;
    @FXML private Label valeurRefuses;
    @FXML private Label sousTitre;

    @FXML private TextField recherche;
    @FXML private ComboBox<String> filtreModule;
    @FXML private ComboBox<String> filtreResultat;
    @FXML private DatePicker dateDu;
    @FXML private DatePicker dateAu;

    @FXML private TableView<EvenementAudit> table;
    @FXML private TableColumn<EvenementAudit, String> colDate;
    @FXML private TableColumn<EvenementAudit, String> colUtilisateur;
    @FXML private TableColumn<EvenementAudit, String> colModule;
    @FXML private TableColumn<EvenementAudit, String> colAction;
    @FXML private TableColumn<EvenementAudit, String> colResultat;

    private final AuditService audit = AppContext.auditService();

    @FXML
    private void initialize() {
        configurerColonnes();
        configurerFiltres();

        for (Region carte : List.of(kpiTotal, kpiReussis, kpiRefuses)) {
            ComptableUi.arrondir(carte);
        }

        rafraichir();
    }

    private void configurerColonnes() {
        colDate.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getHorodatage() == null
                                ? "—"
                                : AuditService.FORMAT_DATE.format(
                                        cell.getValue().getHorodatage()
                                )
                )
        );
        colDate.setCellFactory(col -> ComptableUi.texte("cell-normal"));

        colUtilisateur.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        valeurOuTiret(cell.getValue().getUtilisateur())
                )
        );
        colUtilisateur.setCellFactory(col -> ComptableUi.texte("cell-strong"));

        colModule.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        valeurOuTiret(cell.getValue().getModule())
                )
        );
        colModule.setCellFactory(col -> ComptableUi.texte("cell-normal"));

        colAction.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        valeurOuTiret(cell.getValue().getAction())
                )
        );
        colAction.setCellFactory(col -> ComptableUi.texte("cell-normal"));

        colResultat.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        libelleResultat(cell.getValue().getResultat())
                )
        );
        colResultat.setCellFactory(col -> ComptableUi.badge(
                libelle -> REUSSI.equals(libelle) ? "badge-green" : "badge-red"
        ));
    }

    private void configurerFiltres() {
        actualiserModules();

        filtreResultat.setItems(FXCollections.observableArrayList(
                TOUS_RESULTATS, REUSSI, REFUSE
        ));
        filtreResultat.setValue(TOUS_RESULTATS);

        recherche.textProperty().addListener(
                (obs, ancien, nouveau) -> rafraichir()
        );
        filtreModule.valueProperty().addListener(
                (obs, ancien, nouveau) -> rafraichir()
        );
        filtreResultat.valueProperty().addListener(
                (obs, ancien, nouveau) -> rafraichir()
        );
        dateDu.valueProperty().addListener(
                (obs, ancien, nouveau) -> rafraichir()
        );
        dateAu.valueProperty().addListener(
                (obs, ancien, nouveau) -> rafraichir()
        );
    }

    /** Reloads the module list so events recorded by other pages appear in the filter immediately. */
    private void actualiserModules() {
        String courant = filtreModule.getValue();
        var modules = FXCollections.observableArrayList(TOUS_MODULES);
        modules.addAll(audit.modules());
        filtreModule.setItems(modules);
        filtreModule.setValue(courant != null && modules.contains(courant) ? courant : TOUS_MODULES);
    }

    /** Explicit "Actualiser" button: reloads filter choices and the table. */
    @FXML
    private void actualiser() {
        actualiserModules();
        rafraichir();
    }

    @FXML
    private void rafraichir() {
        String texte = recherche.getText();
        String module = filtreModule.getValue();
        String resultat = filtreResultat.getValue();

        ResultatAudit resultatEnum = null;
        if (REUSSI.equals(resultat)) {
            resultatEnum = ResultatAudit.REUSSI;
        } else if (REFUSE.equals(resultat)) {
            resultatEnum = ResultatAudit.REFUSE;
        }

        String moduleFiltre = module == null || TOUS_MODULES.equals(module)
                ? null : module;

        CritereAudit criteres = new CritereAudit(
                texte == null || texte.isBlank() ? null : texte.trim(),
                moduleFiltre,
                resultatEnum,
                dateDu.getValue(),
                dateAu.getValue()
        );

        try {
            List<EvenementAudit> evenements = audit.filtrer(criteres);
            table.setItems(FXCollections.observableArrayList(evenements));
            mettreAJourStatistiques(evenements);
        } catch (ValidationException e) {
            // La période saisie est invalide : on conserve les résultats affichés.
            Dialogues.erreur(e.getMessage());
        }
    }

    private void mettreAJourStatistiques(List<EvenementAudit> evenements) {
        long reussis = evenements.stream()
                .filter(e -> e.getResultat() == ResultatAudit.REUSSI)
                .count();

        long refuses = evenements.stream()
                .filter(e -> e.getResultat() == ResultatAudit.REFUSE)
                .count();

        valeurTotal.setText(Integer.toString(evenements.size()));
        valeurReussis.setText(Long.toString(reussis));
        valeurRefuses.setText(Long.toString(refuses));

        sousTitre.setText(evenements.size()
                + (evenements.size() > 1
                    ? " opérations correspondant aux filtres"
                    : " opération correspondant aux filtres"));
    }

    @FXML
    private void reinitialiser() {
        recherche.clear();
        filtreModule.setValue(TOUS_MODULES);
        filtreResultat.setValue(TOUS_RESULTATS);
        dateDu.setValue(null);
        dateAu.setValue(null);
        rafraichir();
    }

    private static String valeurOuTiret(String valeur) {
        return valeur == null || valeur.isBlank() ? "—" : valeur;
    }

    private static String libelleResultat(ResultatAudit resultat) {
        if (resultat == null) {
            return "Inconnu";
        }
        return switch (resultat) {
            case REUSSI -> REUSSI;
            case REFUSE -> REFUSE;
        };
    }
}
