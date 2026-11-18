package ma.translogix.controller.dispatcher;

import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.translogix.util.Recherche;

/** "Bons de Gazole & Cartes Autoroute": Iteration 3 page, Hamid's sample vouchers (DÉMO), search only. */
public class CarburantController {

    /** One sample fuel voucher from dispatcher.html. */
    record Bon(String numero, String camion, String chauffeur, String station, String litres, String montant,
               String statut) {
    }

    private static final List<Bon> DEMO = List.of(
            new Bon("BON-8841", "12345-A-6", "Mohammed Bennani", "Afriquia Casablanca Route Zénata", "250 L",
                    "3 250 MAD", "Validé Dispatch"),
            new Bon("BON-8842", "67890-B-2", "Abderrahim El Amrani", "TotalEnergies Agadir Entrée", "180 L",
                    "2 340 MAD", "Validé Dispatch"),
            new Bon("BON-8843", "45678-C-1", "Youssef Amrani", "Shell Rabat Autoroute", "200 L",
                    "2 600 MAD", "En attente reçu"));

    @FXML private TextField recherche;
    @FXML private TableView<Bon> table;
    @FXML private TableColumn<Bon, String> colBon;
    @FXML private TableColumn<Bon, String> colCamion;
    @FXML private TableColumn<Bon, String> colChauffeur;
    @FXML private TableColumn<Bon, String> colStation;
    @FXML private TableColumn<Bon, String> colLitres;
    @FXML private TableColumn<Bon, String> colMontant;
    @FXML private TableColumn<Bon, Bon> colStatut;

    @FXML
    private void initialize() {
        colonne(colBon, Bon::numero, "cell-strong");
        colonne(colCamion, Bon::camion, "cell-normal");
        colonne(colChauffeur, Bon::chauffeur, "cell-normal");
        colonne(colStation, Bon::station, "cell-normal");
        colonne(colLitres, Bon::litres, "cell-normal");
        colonne(colMontant, Bon::montant, "cell-strong");
        colStatut.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colStatut.setCellFactory(col -> DispatcherUi.badge(Bon::statut,
                b -> b.statut().startsWith("Validé") ? "badge-green" : "badge-amber"));

        FilteredList<Bon> filtres = new FilteredList<>(FXCollections.observableArrayList(DEMO));
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(b -> Recherche.correspond(requete, b.numero(), b.chauffeur(), b.station(),
                        b.camion())));
        table.setItems(filtres);
    }

    @FXML
    private void emettre() {
        DispatcherUi.pasEncoreLivre();
    }

    private static void colonne(TableColumn<Bon, String> colonne, java.util.function.Function<Bon, String> valeur,
                                String styleClass) {
        colonne.setCellValueFactory(c -> new SimpleStringProperty(valeur.apply(c.getValue())));
        colonne.setCellFactory(col -> DispatcherUi.texte(styleClass));
    }
}
