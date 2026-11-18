package ma.translogix.controller.rh;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.model.Employe;
import ma.translogix.model.TypeDocument;
import ma.translogix.service.ConformiteService;
import ma.translogix.service.EmployeService;
import ma.translogix.util.Formats;
import ma.translogix.util.Recherche;
import ma.translogix.util.ValidationException;

/** "Chauffeurs & Employés" ([M3]): staff register, live search, add / edit in a dialog, deactivate. */
public class EmployesController {

    @FXML private Button boutonAjouter;
    @FXML private Label valeurChauffeurs;
    @FXML private Label valeurAdministratif;
    @FXML private Label valeurContrats;
    @FXML private TextField recherche;
    @FXML private TableView<Employe> table;
    @FXML private TableColumn<Employe, Employe> colNom;
    @FXML private TableColumn<Employe, Employe> colIdentite;
    @FXML private TableColumn<Employe, String> colPoste;
    @FXML private TableColumn<Employe, String> colPermis;
    @FXML private TableColumn<Employe, String> colEmbauche;
    @FXML private TableColumn<Employe, String> colStatut;
    @FXML private TableColumn<Employe, Employe> colActions;

    private final EmployeService service = AppContext.employeService();
    private final ConformiteService conformite = AppContext.conformiteService();
    private final ObservableList<Employe> donnees = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colNom.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colNom.setCellFactory(col -> RhUi.doubleLigne(Employe::getNomComplet, Employe::getMatricule));
        colNom.setSortable(false);
        colIdentite.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colIdentite.setCellFactory(col -> RhUi.doubleLigne(Employe::getCin, e -> "CNSS : " + RhUi.tiret(e.getCnss())));
        colIdentite.setSortable(false);
        colPoste.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPoste().getLibelle()
                + " · " + c.getValue().getTypeContrat().getLibelle()));
        colPoste.setCellFactory(col -> RhUi.texte("cell-normal"));
        colPermis.setCellValueFactory(c -> new SimpleStringProperty(permis(c.getValue())));
        colPermis.setCellFactory(col -> RhUi.texte("cell-normal"));
        colEmbauche.setCellValueFactory(c -> new SimpleStringProperty(Formats.date(c.getValue().getDateEmbauche())));
        colEmbauche.setCellFactory(col -> RhUi.texte("cell-normal"));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActif() ? "Actif" : "Inactif"));
        colStatut.setCellFactory(col -> RhUi.badge(statut -> "Actif".equals(statut) ? "badge-green" : "badge-gray"));
        colActions.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button modifier = new Button("Modifier");
            private final Button desactiver = new Button("Désactiver");
            private final HBox boutons = new HBox(6, modifier, desactiver);

            {
                boutons.setAlignment(Pos.CENTER_LEFT);
                modifier.getStyleClass().add("link-orange");
                desactiver.getStyleClass().add("link-red");
                modifier.setMinWidth(Region.USE_PREF_SIZE);
                desactiver.setMinWidth(Region.USE_PREF_SIZE);
                modifier.setOnAction(e -> ouvrirFormulaire(getItem()));
                desactiver.setOnAction(e -> desactiver(getItem()));
            }

            @Override
            protected void updateItem(Employe e, boolean empty) {
                super.updateItem(e, empty);
                setText(null);
                if (empty || e == null || RhUi.lectureSeule()) {
                    setGraphic(null);
                    return;
                }
                desactiver.setVisible(e.isActif());
                desactiver.setManaged(e.isActif());
                setGraphic(boutons);
            }
        });

        FilteredList<Employe> filtres = new FilteredList<>(donnees);
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(e -> Recherche.correspond(requete, e.getNom(), e.getPrenom(), e.getMatricule(),
                        e.getCin(), e.getCnss(), e.getPoste().getLibelle())));
        SortedList<Employe> tries = new SortedList<>(filtres);
        tries.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(tries);

        if (RhUi.lectureSeule()) {
            RhUi.masquer(boutonAjouter);
        }
        rafraichir();
    }

    @FXML
    private void ajouter() {
        ouvrirFormulaire(null);
    }

    private void ouvrirFormulaire(Employe employe) {
        if (RhUi.<EmployeFormulaireController>ouvrirFormulaire(table, EmployeFormulaireController.FXML,
                f -> f.preparer(employe))) {
            rafraichir();
        }
    }

    private void desactiver(Employe employe) {
        if (employe == null || !Dialogues.confirmer("Désactiver",
                "Désactiver " + employe.getNomComplet() + " (" + employe.getMatricule() + ") ?\n"
                        + "L'employé reste dans le registre mais ne fait plus partie de l'effectif actif.")) {
            return;
        }
        try {
            service.desactiver(employe.getId());
            rafraichir();
        } catch (ValidationException e) {
            Dialogues.erreur(e.getMessage());
        }
    }

    private void rafraichir() {
        donnees.setAll(service.lister());
        table.refresh(); // the licence column is read from another service
        EmployeService.Effectifs e = service.effectifs();
        valeurChauffeurs.setText(String.valueOf(e.chauffeursActifs()));
        valeurAdministratif.setText(String.valueOf(e.administratifEtDepot()));
        valeurContrats.setText(e.cdi() + " CDI / " + e.cdd() + " CDD" + (e.essai() > 0 ? " / " + e.essai() + " essai" : ""));
    }

    /** "EC · PRM-883920" from the employee's licence, "—" when there is none. */
    private String permis(Employe e) {
        return conformite.document(e.getId(), TypeDocument.PERMIS)
                .map(p -> (p.getCategorie() == null ? "" : p.getCategorie() + " · ") + RhUi.tiret(p.getNumero()))
                .orElse("—");
    }
}
