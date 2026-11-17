package ma.translogix.controller.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.translogix.AppContext;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.ResultatAudit;
import ma.translogix.service.AuditService;

/** Read-only view of the audit log, most recent first, with a search field. */
public class AuditController {

    @FXML private TextField recherche;
    @FXML private Label compteur;
    @FXML private Label videTexte;
    @FXML private TableView<EvenementAudit> table;
    @FXML private TableColumn<EvenementAudit, String> colDate;
    @FXML private TableColumn<EvenementAudit, String> colUtilisateur;
    @FXML private TableColumn<EvenementAudit, String> colModule;
    @FXML private TableColumn<EvenementAudit, String> colAction;
    @FXML private TableColumn<EvenementAudit, String> colResultat;

    private final AuditService service = AppContext.auditService();

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

        FilteredList<EvenementAudit> filtres =
                new FilteredList<>(FXCollections.observableArrayList(service.lister()));
        recherche.textProperty().addListener((obs, ancien, requete) -> {
            filtres.setPredicate(e -> AuditService.correspond(e, requete));
            videTexte.setText(requete == null || requete.isBlank()
                    ? "Aucune action enregistrée pour cette session."
                    : "Aucun événement ne correspond à la recherche.");
            majCompteur(filtres.size());
        });
        table.setItems(filtres);
        majCompteur(filtres.size());
    }

    private void majCompteur(int n) {
        compteur.setText(n + (n > 1 ? " événements" : " événement"));
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
