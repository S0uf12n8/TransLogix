package ma.translogix.controller.rh;

import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** Suivi des permis, cartes FIMC/FCO et visites medicales des conducteurs. */
public class RhComplianceController {

    private static final String NEUTRE = "rh-plain";

    private record Dossier(String chauffeur, String numero, String categorie, String expiration, String tonExpiration,
                           String fimc, String tonFimc, String visite, String tonVisite,
                           String statut, String tonStatut) {
    }

    @FXML private TableView<Dossier> table;
    @FXML private TableColumn<Dossier, String> colChauffeur;
    @FXML private TableColumn<Dossier, String> colNumero;
    @FXML private TableColumn<Dossier, String> colCategorie;
    @FXML private TableColumn<Dossier, String> colExpiration;
    @FXML private TableColumn<Dossier, String> colFimc;
    @FXML private TableColumn<Dossier, String> colVisite;
    @FXML private TableColumn<Dossier, String> colStatut;
    @FXML private TableColumn<Dossier, String> colAction;
    @FXML private TextField recherche;

    private final javafx.collections.ObservableList<Dossier> tous = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colChauffeur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().chauffeur()));
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().numero()));
        colCategorie.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().categorie()));
        colExpiration.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().expiration()));
        colExpiration.setCellFactory(RhCells.texteColore(Dossier::tonExpiration));
        colFimc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fimc()));
        colFimc.setCellFactory(RhCells.texteColore(Dossier::tonFimc));
        colVisite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().visite()));
        colVisite.setCellFactory(RhCells.texteColore(Dossier::tonVisite));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().statut()));
        colStatut.setCellFactory(RhCells.statut(Dossier::tonStatut));
        colAction.setCellFactory(RhCells.bouton("Renouveler", "rh-btn-primary", dossier -> renouveler(dossier.chauffeur())));

        table.setItems(tous);
        tous.setAll(
                new Dossier("Abderrahim El Amrani", "PRM-112938", "C / EC",
                        "04/11/2026 (Expire bient\u00f4t)", "rh-negative",
                        "Valide (2027)", NEUTRE,
                        "15 jours restants", "rh-warning",
                        "Urgent", "red"),
                new Dossier("Mohammed Bennani", "PRM-883920", "EC",
                        "12/10/2028", NEUTRE,
                        "Valide (2028)", NEUTRE,
                        "11/2027", NEUTRE,
                        "Conforme", "green"),
                new Dossier("Youssef Amrani", "PRM-445201", "EC",
                        "21/08/2027", NEUTRE,
                        "\u00c0 renouveler (30j)", "rh-warning",
                        "04/2027", NEUTRE,
                        "\u00c0 suivre", "amber"));

        recherche.textProperty().addListener((obs, ancien, texte) -> {
            String requete = texte == null ? "" : texte.trim().toLowerCase(java.util.Locale.FRENCH);
            table.setItems(FXCollections.observableArrayList(
                    tous.stream()
                            .filter(d -> requete.isEmpty() || d.chauffeur().toLowerCase(java.util.Locale.FRENCH).contains(requete))
                            .toList()));
        });
    }

    @FXML
    private void renouveler() {
        renouveler("");
    }

    private void renouveler(String chauffeur) {
        List<RhFormDialog.Champ> champs = List.of(
                RhFormDialog.Champ.texte("Chauffeur", "driver", "Nom du chauffeur").avecValeur(chauffeur),
                RhFormDialog.Champ.choix("Document renouvel\u00e9", "docType", List.of(
                        "Permis de Conduire (C/EC)", "Carte FIMC/FCO", "Visite M\u00e9dicale Obligatoire")),
                RhFormDialog.Champ.date("Nouvelle Date d'expiration", "expDate"));
        RhFormDialog.ouvrir(null, "Renouvellement Document / Permis",
                        "Conformit\u00e9",
                        "Saisissez la nouvelle date d'\u00e9ch\u00e9ance du permis ou de la visite m\u00e9dicale.",
                        champs)
                .ifPresent(valeurs -> RhNotify.toast("Mise \u00e0 jour effectu\u00e9e : "
                        + valeurs.get("docType") + " pour " + valeurs.get("driver") + "."));
    }
}
