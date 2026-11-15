package ma.translogix.controller.admin;

import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import ma.translogix.util.Navigator;
import ma.translogix.util.Session;

/**
 * Gerant dashboard, reproduced from admine.html.
 * KPI values, trips and alerts are DEMO data (as in the mockup) until the dispatcher,
 * comptable and RH modules exist; they will then come from a DashboardService.
 */
public class DashboardController {

    /** One row of the "Trajets recents" table. */
    public record LigneTrajet(String reference, String chauffeur, String camion,
                              String depart, String arrivee, String statut, String marge) {
    }

    private static final List<LigneTrajet> DEMO = List.of(
            new LigneTrajet("TRJ-9081", "Mohammed Bennani", "12345-A-6 · Volvo",
                    "Casablanca", "Tanger", "En cours", "8 400 MAD"),
            new LigneTrajet("TRJ-9082", "Abderrahim El Amrani", "67890-B-2 · Scania",
                    "Agadir", "Marrakech", "Terminé", "12 100 MAD"),
            new LigneTrajet("TRJ-9083", "Youssef Amrani", "45678-C-1 · MAN",
                    "Rabat", "Fès", "Planifié", "6 700 MAD"));

    private static final Map<String, String> BADGES = Map.of(
            "En cours", "badge-blue",
            "Terminé", "badge-green",
            "Planifié", "badge-amber");

    @FXML private Label salutation;
    @FXML private StackPane kpiChiffre;
    @FXML private StackPane kpiFlotte;
    @FXML private StackPane kpiCreances;
    @FXML private StackPane kpiConformite;
    @FXML private TableView<LigneTrajet> tableTrajets;
    @FXML private TableColumn<LigneTrajet, String> colReference;
    @FXML private TableColumn<LigneTrajet, String> colChauffeur;
    @FXML private TableColumn<LigneTrajet, String> colTrajet;
    @FXML private TableColumn<LigneTrajet, String> colStatut;
    @FXML private TableColumn<LigneTrajet, String> colMarge;

    @FXML
    private void initialize() {
        salutation.setText("Bonjour, " + nomAffiche());
        for (Region carte : List.of(kpiChiffre, kpiFlotte, kpiCreances, kpiConformite)) {
            arrondir(carte);
        }

        colReference.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reference()));
        colReference.setCellFactory(col -> celluleTexte("cell-strong"));

        colChauffeur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().chauffeur()));
        colChauffeur.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                LigneTrajet ligne = empty || getTableRow() == null ? null : getTableRow().getItem();
                setText(null);
                if (ligne == null) {
                    setGraphic(null);
                    return;
                }
                Label nom = new Label(ligne.chauffeur());
                nom.getStyleClass().add("cell-strong");
                Label camion = new Label(ligne.camion());
                camion.getStyleClass().add("cell-sub");
                VBox bloc = new VBox(2, nom, camion);
                bloc.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(bloc);
            }
        });

        colTrajet.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().depart() + "  →  " + c.getValue().arrivee()));
        colTrajet.setCellFactory(col -> celluleTexte("cell-normal"));

        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().statut()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                setText(null);
                if (empty || statut == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label("● " + statut);
                badge.getStyleClass().add(BADGES.getOrDefault(statut, "badge-gray"));
                setGraphic(badge);
            }
        });

        colMarge.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().marge()));
        colMarge.setCellFactory(col -> celluleTexte("cell-strong"));

        tableTrajets.setItems(FXCollections.observableArrayList(DEMO));
    }

    // Targets owned by other members. Until their FXML exists, Navigator shows "Module pas encore livré".

    /** "+ Nouvel ordre de transport", "Voir la flotte", alert "Maintenance flotte". */
    @FXML
    private void ouvrirDispatching() {
        Navigator.ouvrir("dispatcher/missions.fxml");
    }

    /** Alert "Permis à renouveler". */
    @FXML
    private void ouvrirConformite() {
        Navigator.ouvrir("rh/conformite.fxml");
    }

    /** Alert "Factures impayées". */
    @FXML
    private void ouvrirFactures() {
        Navigator.ouvrir("comptable/factures.fxml");
    }

    /** Rounded clip so the tinted circle stays inside the card, like overflow:hidden in the mockup. */
    private static void arrondir(Region carte) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(carte.widthProperty());
        clip.heightProperty().bind(carte.heightProperty());
        carte.setClip(clip);
    }

    private static TableCell<LigneTrajet, String> celluleTexte(String styleClass) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setText(empty ? null : item);
                getStyleClass().removeAll("cell-strong", "cell-normal");
                if (!empty) {
                    getStyleClass().add(styleClass);
                }
            }
        };
    }

    private static String nomAffiche() {
        return Session.estConnecte() ? Session.utilisateur().getNomComplet() : "M. Bennani";
    }
}
