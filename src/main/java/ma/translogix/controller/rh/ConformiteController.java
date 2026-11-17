package ma.translogix.controller.rh;

import java.util.Optional;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import ma.translogix.AppContext;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.Employe;
import ma.translogix.model.StatutConformite;
import ma.translogix.model.TypeDocument;
import ma.translogix.service.ConformiteService;
import ma.translogix.service.EmployeService;
import ma.translogix.util.Formats;
import ma.translogix.util.Recherche;

/** "Conformité & Permis" ([M5]): one row per active driver, statuses computed by ConformiteService. */
public class ConformiteController {

    /** One driver with their three documents (looked up once per refresh). */
    record Ligne(Employe employe, Optional<DocumentConformite> permis, Optional<DocumentConformite> fimc,
                 Optional<DocumentConformite> visite, Optional<StatutConformite> global) {
    }

    @FXML private Button boutonMettreAJour;
    @FXML private HBox banniere;
    @FXML private StackPane banniereIcone;
    @FXML private Label banniereTexte;
    @FXML private TextField recherche;
    @FXML private TableView<Ligne> table;
    @FXML private TableColumn<Ligne, Ligne> colChauffeur;
    @FXML private TableColumn<Ligne, String> colNumero;
    @FXML private TableColumn<Ligne, String> colCategorie;
    @FXML private TableColumn<Ligne, String> colExpiration;
    @FXML private TableColumn<Ligne, String> colFimc;
    @FXML private TableColumn<Ligne, String> colVisite;
    @FXML private TableColumn<Ligne, String> colStatut;
    @FXML private TableColumn<Ligne, Ligne> colAction;

    private final EmployeService employes = AppContext.employeService();
    private final ConformiteService service = AppContext.conformiteService();
    private final ObservableList<Ligne> donnees = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colChauffeur.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colChauffeur.setCellFactory(col -> RhUi.doubleLigne(l -> l.employe().getNomComplet(),
                l -> l.employe().getMatricule()));
        colChauffeur.setSortable(false);
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().permis().map(DocumentConformite::getNumero).map(RhUi::tiret).orElse("—")));
        colNumero.setCellFactory(col -> RhUi.texte("cell-normal"));
        colCategorie.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().permis().map(DocumentConformite::getCategorie).map(RhUi::tiret).orElse("—")));
        colCategorie.setCellFactory(col -> RhUi.texte("cell-normal"));
        colonneDocument(colExpiration, Ligne::permis);
        colonneDocument(colFimc, Ligne::fimc);
        colonneDocument(colVisite, Ligne::visite);
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().global().map(StatutConformite::getLibelle).orElse("Non renseigné")));
        colStatut.setCellFactory(col -> RhUi.badge(RhUi::classeBadge));
        colAction.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button renouveler = new Button("Renouveler");

            {
                renouveler.getStyleClass().add("link-orange");
                renouveler.setMinWidth(Region.USE_PREF_SIZE);
                renouveler.setOnAction(e -> renouveler(getItem()));
            }

            @Override
            protected void updateItem(Ligne l, boolean empty) {
                super.updateItem(l, empty);
                setText(null);
                setGraphic(empty || l == null || RhUi.lectureSeule() ? null : renouveler);
            }
        });

        FilteredList<Ligne> filtres = new FilteredList<>(donnees);
        recherche.textProperty().addListener((obs, ancien, requete) ->
                filtres.setPredicate(l -> Recherche.correspond(requete, l.employe().getNom(), l.employe().getPrenom(),
                        l.employe().getMatricule(), l.permis().map(DocumentConformite::getNumero).orElse(null))));
        table.setItems(filtres);

        if (RhUi.lectureSeule()) {
            RhUi.masquer(boutonMettreAJour);
        }
        rafraichir();
    }

    @FXML
    private void mettreAJour() {
        ouvrirFormulaire(null, null);
    }

    /** Opens the dialog on the driver's most urgent document (the licence when everything is valid). */
    private void renouveler(Ligne ligne) {
        if (ligne == null) {
            return;
        }
        TypeDocument type = service.documentsDe(ligne.employe().getId()).stream()
                .min(java.util.Comparator.comparing(DocumentConformite::getDateExpiration))
                .filter(d -> service.statut(d) != StatutConformite.VALIDE)
                .map(DocumentConformite::getType)
                .orElse(TypeDocument.PERMIS);
        ouvrirFormulaire(ligne.employe(), type);
    }

    private void ouvrirFormulaire(Employe employe, TypeDocument type) {
        if (RhUi.<DocumentFormulaireController>ouvrirFormulaire(table, DocumentFormulaireController.FXML,
                f -> f.preparer(employe, type))) {
            rafraichir();
        }
    }

    private void rafraichir() {
        donnees.setAll(employes.listerChauffeurs().stream().map(this::ligne).toList());

        int alertes = service.alertes().size();
        boolean urgent = alertes > 0;
        banniere.getStyleClass().setAll(urgent ? "banner-red" : "banner-green");
        banniereIcone.getStyleClass().setAll(urgent ? "banner-icon-red" : "banner-icon-green");
        banniereTexte.setText(urgent
                ? alertes + (alertes > 1 ? " éléments nécessitent" : " élément nécessite")
                        + " une action urgente avant suspension de la conduite."
                : "Aucune échéance dans les " + ConformiteService.SEUIL_RENOUVELLEMENT_JOURS
                        + " prochains jours : tous les documents sont conformes.");
    }

    private Ligne ligne(Employe e) {
        return new Ligne(e, service.document(e.getId(), TypeDocument.PERMIS),
                service.document(e.getId(), TypeDocument.FIMC),
                service.document(e.getId(), TypeDocument.VISITE_MEDICALE),
                service.statutGlobal(e.getId()));
    }

    /** Expiry text coloured by status: "12/10/2028", "04/12/2026 · J-15", "Expiré le 01/11/2026". */
    private void colonneDocument(TableColumn<Ligne, String> colonne,
                                 java.util.function.Function<Ligne, Optional<DocumentConformite>> document) {
        colonne.setCellValueFactory(c -> new SimpleStringProperty(
                document.apply(c.getValue()).map(this::echeance).orElse("Non renseigné")));
        colonne.setCellFactory(col -> RhUi.texteColore(l -> document.apply(l)
                .map(d -> RhUi.classeTexte(service.statut(d)))
                .orElse("cell-normal")));
    }

    private String echeance(DocumentConformite d) {
        String date = Formats.date(d.getDateExpiration());
        return switch (service.statut(d)) {
            case EXPIRE -> "Expiré le " + date;
            case A_RENOUVELER -> {
                long jours = service.joursRestants(d);
                yield jours == 0 ? date + " · aujourd'hui" : date + " · J-" + jours;
            }
            case VALIDE -> date;
        };
    }
}
