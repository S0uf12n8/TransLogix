package ma.translogix.controller.chauffeur;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Client;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;
import ma.translogix.service.EspaceChauffeurService;
import ma.translogix.util.Recherche;
import ma.translogix.util.ValidationException;

/** "Mes Ordres de Transport": the logged-in driver's missions, with "Démarrer" / "Livrée" on his own rows. */
public class MesMissionsController {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter JOUR_COURT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    /** One mission and its client name (looked up once per refresh). */
    record Ligne(Mission mission, String client) {
    }

    @FXML private Label titreListe;
    @FXML private Label vide;
    @FXML private TextField recherche;
    @FXML private TableView<Ligne> table;
    @FXML private TableColumn<Ligne, String> colReference;
    @FXML private TableColumn<Ligne, String> colItineraire;
    @FXML private TableColumn<Ligne, String> colClient;
    @FXML private TableColumn<Ligne, String> colCargaison;
    @FXML private TableColumn<Ligne, String> colHoraires;
    @FXML private TableColumn<Ligne, Ligne> colStatut;
    @FXML private TableColumn<Ligne, Ligne> colAction;

    private final EspaceChauffeurService espace = AppContext.espaceChauffeurService();
    private final ObservableList<Ligne> donnees = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colReference.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mission().getReference()));
        colReference.setCellFactory(col -> texte("cell-strong"));
        colItineraire.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mission().getItineraire()));
        colItineraire.setCellFactory(col -> texte("cell-strong"));
        colClient.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().client()));
        colClient.setCellFactory(col -> texte("cell-normal"));
        colCargaison.setCellValueFactory(c -> new SimpleStringProperty(cargaison(c.getValue().mission())));
        colCargaison.setCellFactory(col -> texte("cell-normal"));
        colHoraires.setCellValueFactory(c -> new SimpleStringProperty(horaires(c.getValue().mission())));
        colHoraires.setCellFactory(col -> texte("cell-normal"));
        colStatut.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colStatut.setCellFactory(col -> new CelluleStatut());
        colAction.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colAction.setCellFactory(col -> new CelluleAction());

        FilteredList<Ligne> filtres = new FilteredList<>(donnees);
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(l -> Recherche.correspond(requete, l.mission().getReference(), l.client(),
                        l.mission().getVilleDepart(), l.mission().getVilleArrivee(), l.mission().getCargaison())));
        table.setItems(filtres);
        rafraichir();
    }

    @FXML
    private void synchroniser() {
        rafraichir();
    }

    private void rafraichir() {
        List<Mission> missions;
        try {
            missions = espace.mesMissions();
        } catch (ValidationException e) {
            // Account not linked to an employee record: say so instead of an empty, silent table.
            vide.setText(e.getMessage());
            donnees.clear();
            return;
        }
        Map<Long, String> clients = AppContext.clientService().lister().stream()
                .collect(Collectors.toMap(Client::getId, Client::getNom));
        donnees.setAll(missions.stream().map(m -> new Ligne(m, clients.getOrDefault(m.getClientId(), "—"))).toList());
        long enCours = missions.stream().filter(m -> m.getStatut() == StatutMission.EN_COURS).count();
        titreListe.setText("Liste des Missions Assignées" + (enCours > 0 ? " · " + enCours + " en cours" : ""));
    }

    private void demarrer(Mission m) {
        executer(() -> espace.demarrer(m.getId()));
    }

    private void livrer(Mission m) {
        if (Dialogues.confirmer("Confirmer la livraison", "Confirmer la livraison de " + m.getReference()
                + " (" + m.getItineraire() + ") ?")) {
            executer(() -> espace.livrer(m.getId()));
        }
    }

    private void executer(Runnable action) {
        try {
            action.run();
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
        rafraichir();
    }

    /** "24 T · Produits frais", as in the mockup. */
    private static String cargaison(Mission m) {
        String poids = m.getPoidsTonnes() == null ? ""
                : m.getPoidsTonnes().stripTrailingZeros().toPlainString().replace('.', ',') + " T";
        String nature = m.getCargaison() == null ? "" : m.getCargaison();
        return poids.isEmpty() || nature.isEmpty() ? poids + nature : poids + " · " + nature;
    }

    /** "28/10/2026 · 12:00 → 18:30", or "28/10 22:00 → 29/10 06:00" over two days. */
    private static String horaires(Mission m) {
        LocalDateTime debut = m.getDateDebut();
        LocalDateTime fin = m.getDateFin();
        if (debut.toLocalDate().equals(fin.toLocalDate())) {
            return JOUR.format(debut) + " · " + HEURE.format(debut) + " → " + HEURE.format(fin);
        }
        return JOUR_COURT.format(debut) + " " + HEURE.format(debut) + " → "
                + JOUR_COURT.format(fin) + " " + HEURE.format(fin);
    }

    private static String classeStatut(StatutMission statut) {
        return switch (statut) {
            case PLANIFIEE -> "badge-blue";
            case EN_COURS -> "badge-amber";
            case LIVREE -> "badge-green";
            case FACTUREE -> "badge-purple";
            case ANNULEE -> "badge-gray";
        };
    }

    private static TableCell<Ligne, String> texte(String styleClass) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("cell-strong", "cell-normal");
                if (!empty) {
                    getStyleClass().add(styleClass);
                }
            }
        };
    }

    private static final class CelluleStatut extends TableCell<Ligne, Ligne> {
        @Override
        protected void updateItem(Ligne ligne, boolean empty) {
            super.updateItem(ligne, empty);
            setText(null);
            if (empty || ligne == null) {
                setGraphic(null);
                return;
            }
            // A delivered then invoiced mission is simply "Livrée" for the driver (invoicing is not his business).
            StatutMission statut = ligne.mission().getStatut() == StatutMission.FACTUREE
                    ? StatutMission.LIVREE : ligne.mission().getStatut();
            Label badge = new Label("● " + statut.getLibelle());
            badge.setMinWidth(Region.USE_PREF_SIZE);
            badge.getStyleClass().add(classeStatut(statut));
            setGraphic(badge);
        }
    }

    /** "Démarrer" on a planned mission, "Livrée" on the one on the road, nothing on the history. */
    private final class CelluleAction extends TableCell<Ligne, Ligne> {
        {
            setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(Ligne ligne, boolean empty) {
            super.updateItem(ligne, empty);
            setText(null);
            setGraphic(null);
            if (empty || ligne == null) {
                return;
            }
            Mission m = ligne.mission();
            if (m.getStatut() == StatutMission.PLANIFIEE) {
                setGraphic(bouton("Démarrer", "link-orange", () -> demarrer(m)));
            } else if (m.getStatut() == StatutMission.EN_COURS) {
                setGraphic(bouton("Livrée", "link-green", () -> livrer(m)));
            }
        }

        private Button bouton(String texte, String styleClass, Runnable action) {
            Button b = new Button(texte);
            b.getStyleClass().add(styleClass);
            b.setMinWidth(Region.USE_PREF_SIZE);
            b.setOnAction(e -> action.run());
            return b;
        }
    }
}
