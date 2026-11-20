package ma.translogix.controller.admin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.service.FiltreRapport;
import ma.translogix.service.Rapport;
import ma.translogix.service.RapportService;
import ma.translogix.service.TypeRapport;
import ma.translogix.util.CsvExporter;
import ma.translogix.util.Formats;
import ma.translogix.util.ValidationException;

/**
 * "Rapports & analytics": any filter change rebuilds the report through RapportService and refreshes the preview.
 * The export writes exactly the previewed rows. Filters a report does not use are disabled.
 */
public class RapportsController {

    /** One entry of the client filter; id null = all clients. */
    private record ChoixClient(Long id, String nom) {
        @Override
        public String toString() {
            return nom;
        }
    }

    private static final ChoixClient TOUS_LES_CLIENTS = new ChoixClient(null, "Tous les clients");
    private static final String TOUS_LES_STATUTS = "Tous les statuts";

    private final RapportService service = AppContext.rapportService();

    @FXML private ComboBox<TypeRapport> choixRapport;
    @FXML private DatePicker du;
    @FXML private DatePicker au;
    @FXML private ComboBox<ChoixClient> choixClient;
    @FXML private ComboBox<String> choixStatut;
    @FXML private Label erreur;
    @FXML private Label titre;
    @FXML private Label resume;
    @FXML private Button boutonExport;
    @FXML private TableView<List<String>> table;

    private Rapport rapport;
    /** True while the controls are filled by code: their listeners must not rebuild the report. */
    private boolean miseAJour;

    @FXML
    private void initialize() {
        for (DatePicker champ : List.of(du, au)) {
            champ.setConverter(new StringConverter<>() {
                @Override
                public String toString(LocalDate date) {
                    return Formats.date(date);
                }

                /** A badly typed date keeps the previous value and shows why, instead of throwing. */
                @Override
                public LocalDate fromString(String texte) {
                    if (texte == null || texte.isBlank()) {
                        return null;
                    }
                    try {
                        return Formats.lireDate(texte);
                    } catch (ValidationException ex) {
                        montrerErreur(ex.getMessage());
                        return champ.getValue();
                    }
                }
            });
            champ.valueProperty().addListener((obs, avant, apres) -> actualiser());
        }

        List<ChoixClient> clients = new ArrayList<>();
        clients.add(TOUS_LES_CLIENTS);
        service.clients().forEach(c -> clients.add(new ChoixClient(c.getId(), c.getNom())));
        choixClient.setItems(FXCollections.observableArrayList(clients));
        choixClient.getSelectionModel().select(TOUS_LES_CLIENTS);
        choixClient.valueProperty().addListener((obs, avant, apres) -> actualiser());

        choixStatut.valueProperty().addListener((obs, avant, apres) -> actualiser());

        choixRapport.setItems(FXCollections.observableArrayList(TypeRapport.values()));
        choixRapport.valueProperty().addListener((obs, avant, apres) -> changerDeRapport(apres));
        choixRapport.getSelectionModel().select(TypeRapport.FACTURATION);
    }

    /** New report: its own statuses, only the filters it uses, then a fresh preview. */
    private void changerDeRapport(TypeRapport type) {
        miseAJour = true;
        List<String> statuts = new ArrayList<>();
        statuts.add(TOUS_LES_STATUTS);
        statuts.addAll(service.statuts(type));
        choixStatut.setItems(FXCollections.observableArrayList(statuts));
        choixStatut.getSelectionModel().select(TOUS_LES_STATUTS);
        du.setDisable(!type.utiliseDates());
        au.setDisable(!type.utiliseDates());
        choixClient.setDisable(!type.utiliseClient());
        miseAJour = false;
        actualiser();
    }

    @FXML
    private void reinitialiser() {
        miseAJour = true;
        du.setValue(null);
        au.setValue(null);
        choixClient.getSelectionModel().select(TOUS_LES_CLIENTS);
        choixStatut.getSelectionModel().select(TOUS_LES_STATUTS);
        miseAJour = false;
        actualiser();
    }

    private void actualiser() {
        TypeRapport type = choixRapport.getValue();
        if (miseAJour || type == null) {
            return;
        }
        ChoixClient client = choixClient.getValue();
        String statut = choixStatut.getValue();
        FiltreRapport filtre = new FiltreRapport(du.getValue(), au.getValue(),
                client == null ? null : client.id(),
                statut == null || TOUS_LES_STATUTS.equals(statut) ? null : statut);
        try {
            rapport = service.generer(type, filtre);
        } catch (ValidationException ex) {
            montrerErreur(ex.getMessage());
            return;
        }
        montrerErreur(null);
        afficher(rapport);
    }

    private void afficher(Rapport r) {
        titre.setText(r.titre());
        resume.setText(r.resume());
        table.getColumns().clear();
        for (int i = 0; i < r.entetes().size(); i++) {
            int colonne = i;
            TableColumn<List<String>, String> col = new TableColumn<>(r.entetes().get(i).toUpperCase());
            col.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().get(colonne)));
            int plusLong = r.lignes().stream().mapToInt(l -> l.get(colonne).length()).max().orElse(0);
            col.setPrefWidth(Math.min(320, Math.max(90, Math.max(r.entetes().get(i).length() * 8, plusLong * 8) + 30)));
            table.getColumns().add(col);
        }
        table.setItems(FXCollections.observableArrayList(r.lignes()));
        boutonExport.setDisable(r.lignes().isEmpty());
    }

    /** null hides the message. An invalid period empties the preview so nothing stale can be exported. */
    private void montrerErreur(String message) {
        boolean visible = message != null;
        erreur.setText(visible ? message : "");
        erreur.setVisible(visible);
        erreur.setManaged(visible);
        if (visible) {
            rapport = null;
            table.getItems().clear();
            resume.setText("");
            boutonExport.setDisable(true);
        }
    }

    @FXML
    private void exporter() {
        if (rapport == null) {
            return;
        }
        FileChooser choix = new FileChooser();
        choix.setTitle("Exporter « " + rapport.titre() + " »");
        choix.setInitialFileName(rapport.nomFichier());
        choix.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV (*.csv)", "*.csv"));
        File fichier = choix.showSaveDialog(table.getScene().getWindow());
        if (fichier == null) {
            return; // cancelled
        }
        if (!fichier.getName().toLowerCase().endsWith(".csv")) {
            fichier = new File(fichier.getParentFile(), fichier.getName() + ".csv");
        }
        try {
            CsvExporter.ecrire(fichier.toPath(), rapport.entetes(), rapport.lignes());
        } catch (IOException ex) {
            Dialogues.erreur("Impossible d'écrire le fichier :\n" + fichier + "\n" + ex.getMessage());
            return;
        }
        Dialogues.info("Export réussi", rapport.lignes().size() + " ligne(s) exportée(s) dans :\n" + fichier);
    }
}
