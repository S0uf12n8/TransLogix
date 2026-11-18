package ma.translogix.controller.rh;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import ma.translogix.util.Recherche;

/**
 * "Paie & Prime Trajet": Iteration 3. Majda's sample figures are shown as a DÉMO (fictional, not computed);
 * "Calculer les Primes" and "Détail" only say the module is not delivered yet.
 */
public class PrimesController {

    /** Sample row from rh.html (fictional). */
    record Prime(String chauffeur, String missions, String distance, String base, String prime, String net) {
    }

    @FXML private TextField recherche;
    @FXML private TableView<Prime> table;
    @FXML private TableColumn<Prime, String> colChauffeur;
    @FXML private TableColumn<Prime, String> colMissions;
    @FXML private TableColumn<Prime, String> colDistance;
    @FXML private TableColumn<Prime, String> colBase;
    @FXML private TableColumn<Prime, String> colPrime;
    @FXML private TableColumn<Prime, String> colNet;
    @FXML private TableColumn<Prime, Prime> colAction;

    @FXML
    private void initialize() {
        colChauffeur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().chauffeur()));
        colChauffeur.setCellFactory(col -> RhUi.texte("cell-strong"));
        colMissions.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().missions()));
        colMissions.setCellFactory(col -> RhUi.texte("cell-normal"));
        colDistance.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().distance()));
        colDistance.setCellFactory(col -> RhUi.texte("cell-normal"));
        colBase.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().base()));
        colBase.setCellFactory(col -> RhUi.texte("cell-normal"));
        colPrime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().prime()));
        colPrime.setCellFactory(col -> RhUi.texteColore(p -> "cell-orange"));
        colNet.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().net()));
        colNet.setCellFactory(col -> RhUi.texte("cell-strong"));
        colAction.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button detail = new Button("Détail");

            {
                detail.getStyleClass().add("link-orange");
                detail.setMinWidth(Region.USE_PREF_SIZE);
                detail.setOnAction(e -> RhUi.pasEncoreLivre());
            }

            @Override
            protected void updateItem(Prime p, boolean empty) {
                super.updateItem(p, empty);
                setText(null);
                setGraphic(empty || p == null ? null : detail);
            }
        });

        ObservableList<Prime> primes = FXCollections.observableArrayList(
                new Prime("Mohammed Bennani", "14 trajets", "4 200 km", "5 500 MAD", "3 150 MAD", "8 650 MAD"),
                new Prime("Abderrahim El Amrani", "18 trajets", "5 100 km", "5 200 MAD", "3 825 MAD", "9 025 MAD"),
                new Prime("Youssef Amrani", "10 trajets", "3 100 km", "5 500 MAD", "2 325 MAD", "7 825 MAD"));
        FilteredList<Prime> filtres = new FilteredList<>(primes);
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(p -> Recherche.correspond(requete, p.chauffeur())));
        table.setItems(filtres);
    }

    @FXML
    private void calculer() {
        RhUi.pasEncoreLivre();
    }
}
