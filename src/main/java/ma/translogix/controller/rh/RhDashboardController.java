package ma.translogix.controller.rh;

import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** Tableau de bord RH: KPI cards, quick status table and quick actions. */
public class RhDashboardController {

    private record Conducteur(String nom, String poste, String permis, String visite, String tonVisite,
                              String statut, String tonStatut) {
    }

    @FXML private TableView<Conducteur> tableConducteurs;
    @FXML private TableColumn<Conducteur, String> colEmploye;
    @FXML private TableColumn<Conducteur, String> colPoste;
    @FXML private TableColumn<Conducteur, String> colPermis;
    @FXML private TableColumn<Conducteur, String> colVisite;
    @FXML private TableColumn<Conducteur, String> colStatut;

    @FXML
    private void initialize() {
        colEmploye.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nom()));
        colPoste.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().poste()));
        colPermis.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().permis()));
        colVisite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().visite()));
        colVisite.setCellFactory(RhCells.texteColore(Conducteur::tonVisite));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().statut()));
        colStatut.setCellFactory(RhCells.statut(Conducteur::tonStatut));

        tableConducteurs.setItems(FXCollections.observableArrayList(
                new Conducteur("Mohammed Bennani", "Chauffeur Poids Lourds (EC)", "PRM-883920 (2028)",
                        "Conforme (11/2027)", "rh-positive", "En Mission", "green"),
                new Conducteur("Abderrahim El Amrani", "Chauffeur Routier (C)", "PRM-112938 (11/2026)",
                        "\u00c9ch\u00e9ance 15 jours", "rh-warning", "Disponible", "blue"),
                new Conducteur("Youssef Amrani", "Chauffeur Super Lourds (EC)", "PRM-445201 (2027)",
                        "Conforme (04/2027)", "rh-positive", "En Cong\u00e9", "amber")));
    }

    @FXML
    private void exporter() {
        RhNotify.toast("Export CSV du rapport RH g\u00e9n\u00e9r\u00e9.");
    }

    @FXML
    private void voirAnnuaire() {
        RhNotify.toast("Ouverture de l'annuaire du personnel.");
    }

    @FXML
    private void saisirConge() {
        RhFormDialog.ouvrir(null, "Saisie de Demande de Cong\u00e9",
                "Gestion RH",
                "Enregistrez une absence ou un cong\u00e9 pay\u00e9.",
                List.of(
                        RhFormDialog.Champ.texte("Salari\u00e9 concern\u00e9", "employee", "Ex: Mohammed Bennani"),
                        RhFormDialog.Champ.choix("Type de cong\u00e9", "type", List.of(
                                "Cong\u00e9 Pay\u00e9 Annuel", "Maladie / Accident",
                                "\u00c9v\u00e9nement Familial", "Absence Injustifi\u00e9e")),
                        RhFormDialog.Champ.date("Date de d\u00e9but", "start"),
                        RhFormDialog.Champ.date("Date de fin", "end")))
                .ifPresent(valeurs -> RhNotify.toast("Demande de cong\u00e9 enregistr\u00e9e pour "
                        + valeurs.get("employee") + "."));
    }

    @FXML
    private void renouvelerDocument() {
        RhFormDialog.ouvrir(null, "Renouvellement Document / Permis",
                "Conformit\u00e9",
                "Saisissez la nouvelle date d'\u00e9ch\u00e9ance du permis ou de la visite m\u00e9dicale.",
                List.of(
                        RhFormDialog.Champ.texte("Chauffeur", "driver", "Nom du chauffeur"),
                        RhFormDialog.Champ.choix("Document renouvel\u00e9", "docType", List.of(
                                "Permis de Conduire (C/EC)", "Carte FIMC/FCO", "Visite M\u00e9dicale Obligatoire")),
                        RhFormDialog.Champ.date("Nouvelle Date d'expiration", "expDate")))
                .ifPresent(valeurs -> RhNotify.toast("Mise \u00e0 jour effectu\u00e9e : "
                        + valeurs.get("docType") + " pour " + valeurs.get("driver") + "."));
    }

    @FXML
    private void ouvrirPaie() {
        RhNotify.toast("Calculateur de prime route ouvert.");
    }
}
