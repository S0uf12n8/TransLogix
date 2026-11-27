package ma.translogix.controller.rh;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.DemandeConge;
import ma.translogix.model.Employe;
import ma.translogix.model.StatutConge;
import ma.translogix.service.CongeService;
import ma.translogix.util.Formats;
import ma.translogix.util.ValidationException;

/**
 * "Congés & Absences": the table and the counters come from CongeService. Every action goes through the
 * service; the Gérant sees the page read-only.
 */
public class CongesController {

    @FXML private TableView<DemandeConge> table;
    @FXML private TableColumn<DemandeConge, DemandeConge> colEmploye;
    @FXML private TableColumn<DemandeConge, String> colType;
    @FXML private TableColumn<DemandeConge, String> colPeriode;
    @FXML private TableColumn<DemandeConge, String> colJours;
    @FXML private TableColumn<DemandeConge, String> colStatut;
    @FXML private TableColumn<DemandeConge, DemandeConge> colAction;
    @FXML private Label totalAttente;
    @FXML private Label totalApprouves;
    @FXML private Label totalRefuses;
    @FXML private Button nouvelleDemande;

    private Map<Long, Employe> employesParId = Map.of();

    @FXML
    private void initialize() {
        colEmploye.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colEmploye.setCellFactory(col -> RhUi.doubleLigne(this::nom, this::matricule));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getLibelle()));
        colType.setCellFactory(col -> RhUi.texte("cell-normal"));
        colPeriode.setCellValueFactory(c -> new SimpleStringProperty(periode(c.getValue())));
        colPeriode.setCellFactory(col -> RhUi.texte("cell-normal"));
        colJours.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getNbJours())));
        colJours.setCellFactory(col -> RhUi.texte("cell-normal"));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut().getLibelle()));
        colStatut.setCellFactory(col -> RhUi.badge(CongesController::classeStatut));
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
            }

            @Override
            protected void updateItem(DemandeConge demande, boolean empty) {
                super.updateItem(demande, empty);
                setText(null);
                boolean aTraiter = !empty && demande != null
                        && demande.getStatut() == StatutConge.EN_ATTENTE && !RhUi.lectureSeule();
                if (!aTraiter) {
                    setGraphic(null);
                    return;
                }
                accorder.setOnAction(e -> traiter(demande, true));
                refuser.setOnAction(e -> traiter(demande, false));
                setGraphic(boutons);
            }
        });

        if (RhUi.lectureSeule()) {
            RhUi.masquer(nouvelleDemande);
        }
        rafraichir();
    }

    @FXML
    private void nouvelleDemande() {
        if (RhUi.<CongeFormulaireController>ouvrirFormulaire(table, CongeFormulaireController.FXML, f -> { })) {
            rafraichir();
        }
    }

    private void traiter(DemandeConge demande, boolean accorder) {
        String verbe = accorder ? "accorder" : "refuser";
        if (!Dialogues.confirmer("Décision sur une demande de congé",
                "Voulez-vous " + verbe + " la demande de " + nom(demande) + " ?")) {
            return;
        }
        try {
            if (accorder) {
                AppContext.congeService().approuver(demande.getId());
            } else {
                AppContext.congeService().refuser(demande.getId());
            }
            rafraichir();
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
    }

    private void rafraichir() {
        employesParId = AppContext.employeService().lister().stream()
                .collect(Collectors.toMap(Employe::getId, Function.identity(), (a, b) -> a));
        table.setItems(FXCollections.observableArrayList(AppContext.congeService().lister()));
        CongeService.Compteurs compteurs = AppContext.congeService().compteurs();
        totalAttente.setText(String.valueOf(compteurs.enAttente()));
        totalApprouves.setText(String.valueOf(compteurs.approuves()));
        totalRefuses.setText(String.valueOf(compteurs.refuses()));
    }

    private String nom(DemandeConge demande) {
        Employe employe = employesParId.get(demande.getEmployeId());
        return employe == null ? "Employé supprimé" : employe.getNomComplet();
    }

    private String matricule(DemandeConge demande) {
        Employe employe = employesParId.get(demande.getEmployeId());
        return employe == null ? "—" : employe.getMatricule();
    }

    private static String periode(DemandeConge demande) {
        return Formats.date(demande.getDateDebut()) + " → " + Formats.date(demande.getDateFin());
    }

    private static String classeStatut(String libelle) {
        for (StatutConge statut : StatutConge.values()) {
            if (statut.getLibelle().equals(libelle)) {
                return switch (statut) {
                    case EN_ATTENTE -> "badge-amber";
                    case APPROUVE -> "badge-green";
                    case REFUSE -> "badge-red";
                };
            }
        }
        return "badge-gray";
    }
}
