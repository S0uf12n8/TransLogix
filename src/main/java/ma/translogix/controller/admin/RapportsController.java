package ma.translogix.controller.admin;

import java.io.File;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import ma.translogix.AppContext;
import ma.translogix.controller.common.Dialogues;
import ma.translogix.service.Rapport;
import ma.translogix.service.RapportService;
import ma.translogix.util.CsvExporter;

/** "Rapports & analytics": each card asks where to save, then writes the CSV. Data comes from RapportService. */
public class RapportsController {

    private final RapportService service = AppContext.rapportService();

    @FXML
    private void exporterRentabilite(ActionEvent e) {
        exporter(service.rentabilite(), (Node) e.getSource());
    }

    @FXML
    private void exporterGazole(ActionEvent e) {
        exporter(service.gazole(), (Node) e.getSource());
    }

    @FXML
    private void exporterConformiteRh(ActionEvent e) {
        exporter(service.conformiteRh(), (Node) e.getSource());
    }

    private void exporter(Rapport rapport, Node source) {
        FileChooser choix = new FileChooser();
        choix.setTitle("Exporter « " + rapport.titre() + " »");
        choix.setInitialFileName(rapport.nomFichier());
        choix.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV (*.csv)", "*.csv"));
        File fichier = choix.showSaveDialog(source.getScene().getWindow());
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
        Dialogues.info("Export réussi",
                rapport.lignes().size() + " ligne(s) exportée(s) dans :\n" + fichier
                        + (rapport.demo() ? "\n\nAttention : données de démonstration." : ""));
    }
}
