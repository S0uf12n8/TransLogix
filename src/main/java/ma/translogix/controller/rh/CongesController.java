package ma.translogix.controller.rh;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * "Congés & Absences": Iteration 3. Majda's sample requests are shown as a DÉMO; "Accorder", "Refuser" and
 * "Nouvelle demande" only say the module is not delivered yet (nothing is stored, no fake success).
 */
public class CongesController {

    /** Sample row from rh.html (fictional). */
    record Demande(String employe, String type, String periode, String jours, String statut) {
    }

    @FXML private TableView<Demande> table;
    @FXML private TableColumn<Demande, String> colEmploye;
    @FXML private TableColumn<Demande, String> colType;
    @FXML private TableColumn<Demande, String> colPeriode;
    @FXML private TableColumn<Demande, String> colJours;
    @FXML private TableColumn<Demande, String> colStatut;
    @FXML private TableColumn<Demande, Demande> colAction;

    @FXML
    private void initialize() {
        colEmploye.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().employe()));
        colEmploye.setCellFactory(col -> RhUi.texte("cell-strong"));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().type()));
        colType.setCellFactory(col -> RhUi.texte("cell-normal"));
        colPeriode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().periode()));
        colPeriode.setCellFactory(col -> RhUi.texte("cell-normal"));
        colJours.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().jours()));
        colJours.setCellFactory(col -> RhUi.texte("cell-normal"));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().statut()));
        colStatut.setCellFactory(col -> RhUi.badge(s -> "Approuvé".equals(s) ? "badge-green" : "badge-amber"));
        colAction.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button accorder = new Button("Accorder");
            private final Button refuser = new Button("Refuser");
            private final HBox boutons = new HBox(6, accorder, refuser);

            {
                boutons.setAlignment(Pos.CENTER_LEFT);
                accorder.getStyleClass().add("link-orange");
                refuser.getStyleClass().add("link-red");
                accorder.setMinWidth(Region.USE_PREF_SIZE);
                refuser.setMinWidth(Region.USE_PREF_SIZE);
                accorder.setOnAction(e -> RhUi.pasEncoreLivre());
                refuser.setOnAction(e -> RhUi.pasEncoreLivre());
            }

            @Override
            protected void updateItem(Demande d, boolean empty) {
                super.updateItem(d, empty);
                setText(null);
                boolean enAttente = !empty && d != null && "En attente".equals(d.statut());
                setGraphic(enAttente && !RhUi.lectureSeule() ? boutons : null);
            }
        });

        table.setItems(FXCollections.observableArrayList(
                new Demande("Youssef Amrani", "Congé Payé Annuel", "25/10/2026 → 02/11/2026", "8 jours", "Approuvé"),
                new Demande("Karim El Idrissi", "Congé Exceptionnel (Mariage)", "05/11/2026 → 09/11/2026",
                        "4 jours", "En attente"),
                new Demande("Abderrahim El Amrani", "Maladie (Justifié)", "28/10/2026 → 30/10/2026", "2 jours",
                        "En attente")));
    }

    @FXML
    private void nouvelleDemande() {
        RhUi.pasEncoreLivre();
    }
}
