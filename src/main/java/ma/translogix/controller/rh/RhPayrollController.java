package ma.translogix.controller.rh;

import java.util.Locale;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** Decompte des primes de trajet par chauffeur. */
public class RhPayrollController {

    private record Prime(String chauffeur, String missions, String distance, String base,
                         String prime, String net) {
    }

    @FXML private TableView<Prime> table;
    @FXML private TableColumn<Prime, String> colChauffeur;
    @FXML private TableColumn<Prime, String> colMissions;
    @FXML private TableColumn<Prime, String> colDistance;
    @FXML private TableColumn<Prime, String> colBase;
    @FXML private TableColumn<Prime, String> colPrime;
    @FXML private TableColumn<Prime, String> colNet;
    @FXML private TableColumn<Prime, String> colAction;
    @FXML private TextField recherche;

    private final ObservableList<Prime> tous = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colChauffeur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().chauffeur()));
        colMissions.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().missions()));
        colDistance.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().distance()));
        colBase.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().base()));
        colPrime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().prime()));
        colPrime.setCellFactory(RhCells.texteColore(p -> "rh-accent"));
        colNet.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().net()));
        colNet.setCellFactory(RhCells.texteColore(p -> "rh-strong"));
        colAction.setCellFactory(RhCells.bouton("D\u00e9tail", "rh-link",
                prime -> RhNotify.toast("Fiche de calcul g\u00e9n\u00e9r\u00e9e pour " + prime.chauffeur() + ".")));

        tous.setAll(
                new Prime("Mohammed Bennani", "14 trajets", "4 200 km", "5 500 MAD", "3 150 MAD", "8 650 MAD"),
                new Prime("Abderrahim El Amrani", "18 trajets", "5 100 km", "5 200 MAD", "3 825 MAD", "9 025 MAD"),
                new Prime("Youssef Amrani", "10 trajets", "3 100 km", "5 500 MAD", "2 325 MAD", "7 825 MAD"));
        table.setItems(tous);

        recherche.textProperty().addListener((obs, ancien, texte) -> {
            String requete = texte == null ? "" : texte.trim().toLowerCase(Locale.FRENCH);
            table.setItems(FXCollections.observableArrayList(
                    tous.stream()
                            .filter(p -> requete.isEmpty() || p.chauffeur().toLowerCase(Locale.FRENCH).contains(requete))
                            .toList()));
        });
    }

    @FXML
    private void calculer() {
        RhNotify.toast("Calcul global des primes du mois lanc\u00e9.");
    }
}
