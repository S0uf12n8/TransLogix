package ma.translogix.controller.rh;

import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** Annuaire du personnel: registre complet avec recherche instantanee. */
public class RhStaffController {

    private record Salarie(String nom, String matricule, String cin, String cnss, String poste,
                           String permis, String embauche, String statut, String tonStatut) {
    }

    @FXML private TableView<Salarie> table;
    @FXML private TableColumn<Salarie, String> colNom;
    @FXML private TableColumn<Salarie, String> colIdentite;
    @FXML private TableColumn<Salarie, String> colPoste;
    @FXML private TableColumn<Salarie, String> colPermis;
    @FXML private TableColumn<Salarie, String> colEmbauche;
    @FXML private TableColumn<Salarie, String> colStatut;
    @FXML private TableColumn<Salarie, String> colAction;
    @FXML private TextField recherche;

    @FXML
    private void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nom()));
        colNom.setCellFactory(RhCells.doubleLigne(Salarie::nom, Salarie::matricule));
        colIdentite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cin()));
        colIdentite.setCellFactory(RhCells.doubleLigne(Salarie::cin, s -> "CNSS: " + s.cnss()));
        colPoste.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().poste()));
        colPermis.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().permis()));
        colEmbauche.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().embauche()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().statut()));
        colStatut.setCellFactory(RhCells.statut(Salarie::tonStatut));
        colAction.setCellFactory(RhCells.bouton("Dossier", "rh-link",
                salarie -> RhNotify.toast("Affichage du dossier salari\u00e9 de " + salarie.nom() + ".")));

        ObservableList<Salarie> salaries = FXCollections.observableArrayList(
                new Salarie("Mohammed Bennani", "MAT-2021-04", "BE-889021", "198273645", "Chauffeur Routier",
                        "EC \u00b7 PRM-883920", "15/03/2021", "Actif", "green"),
                new Salarie("Abderrahim El Amrani", "MAT-2022-12", "A-443219", "209384712", "Chauffeur Poids Lourds",
                        "C \u00b7 PRM-112938", "01/06/2022", "Actif", "green"),
                new Salarie("Youssef Amrani", "MAT-2020-02", "CD-110293", "102938475", "Chauffeur Super Lourds",
                        "EC \u00b7 PRM-445201", "10/01/2020", "En Cong\u00e9", "amber"),
                new Salarie("Karima Bouazzaoui", "MAT-2023-08", "BK-667123", "334928104", "Assistante Exploitation",
                        "B \u00b7 PRM-992011", "01/02/2023", "Actif", "green"));

        FilteredList<Salarie> filtre = new FilteredList<>(salaries, s -> true);
        recherche.textProperty().addListener((obs, ancien, texte) -> {
            String requete = texte == null ? "" : texte.trim().toLowerCase(Locale.FRENCH);
            filtre.setPredicate(salarie -> requete.isEmpty() || (
                    salarie.nom() + " " + salarie.matricule() + " " + salarie.cin() + " "
                            + salarie.cnss() + " " + salarie.poste()).toLowerCase(Locale.FRENCH).contains(requete));
        });
        table.setItems(filtre);
    }

    @FXML
    private void ajouter() {
        RhFormDialog.ouvrir(null, "Nouveau Salari\u00e9",
                "Gestion RH",
                "Saisissez les informations administratives du nouveau collaborateur.",
                List.of(
                        RhFormDialog.Champ.texte("Nom & Pr\u00e9nom", "fullname", "Ex: Amine El Idrissi"),
                        RhFormDialog.Champ.texte("CIN", "cin", "Ex: BE-998822"),
                        RhFormDialog.Champ.texte("Num\u00e9ro CNSS", "cnss", "Ex: 198273645"),
                        RhFormDialog.Champ.choix("Poste Occup\u00e9", "role", List.of(
                                "Chauffeur Routier (EC)", "Chauffeur Poids Lourds (C)",
                                "Agent de D\u00e9p\u00f4t", "M\u00e9canicien Atelier", "Gestionnaire Administration")),
                        RhFormDialog.Champ.choix("Type de Contrat", "contract", List.of("CDI", "CDD", "P\u00e9riode d'essai"))))
                .ifPresent(valeurs -> RhNotify.toast("Salari\u00e9 " + valeurs.get("fullname") + " cr\u00e9\u00e9 avec succ\u00e8s."));
    }
}
