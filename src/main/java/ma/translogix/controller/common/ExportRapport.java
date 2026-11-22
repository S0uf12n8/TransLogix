package ma.translogix.controller.common;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ma.translogix.util.CsvExporter;

/** The two ways out of a report, shared by every "Imprimer" and "Exporter" button: print/PDF, or CSV for Excel. */
public final class ExportRapport {

    private ExportRapport() {
    }

    /** Asks "Imprimer / PDF" or "CSV (Excel)", then does it. */
    public static void proposer(ImpressionRapport.Document document, Window fenetre) {
        ButtonType imprimer = new ButtonType("Imprimer / PDF", ButtonBar.ButtonData.OK_DONE);
        ButtonType csv = new ButtonType("CSV (Excel)", ButtonBar.ButtonData.OTHER);
        ButtonType annuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        int n = document.rapport().lignes().size();
        Alert choix = new Alert(Alert.AlertType.CONFIRMATION,
                "« " + document.rapport().titre() + " » : " + n + (n > 1 ? " lignes" : " ligne") + ".\n\n"
                        + "Imprimer / PDF : document mis en page (en-tête, tableau, numéros de page). "
                        + "Pour un PDF, choisissez l'imprimante « Microsoft Print to PDF ».\n"
                        + "CSV : fichier qui s'ouvre dans Excel.",
                imprimer, csv, annuler);
        choix.setTitle("Exporter");
        choix.setHeaderText(null);
        choix.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        if (fenetre != null) {
            choix.initOwner(fenetre);
        }
        Optional<ButtonType> reponse = choix.showAndWait();
        if (reponse.isEmpty() || reponse.get() == annuler) {
            return;
        }
        if (reponse.get() == imprimer) {
            ImpressionRapport.imprimer(document, fenetre);
        } else {
            exporterCsv(document, fenetre);
        }
    }

    /** Asks where to save, then writes the CSV (UTF-8, ";", Excel-friendly). */
    public static void exporterCsv(ImpressionRapport.Document document, Window fenetre) {
        var rapport = document.rapport();
        FileChooser choix = new FileChooser();
        choix.setTitle("Exporter « " + rapport.titre() + " »");
        choix.setInitialFileName(rapport.nomFichier());
        choix.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV (*.csv)", "*.csv"));
        File fichier = choix.showSaveDialog(fenetre);
        if (fichier == null) {
            return; // cancelled
        }
        if (!fichier.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            fichier = new File(fichier.getParentFile(), fichier.getName() + ".csv");
        }
        try {
            CsvExporter.ecrire(fichier.toPath(), rapport.entetes(), rapport.lignes());
        } catch (IOException ex) {
            Dialogues.erreur("Impossible d'écrire le fichier :\n" + fichier + "\n" + ex.getMessage());
            return;
        }
        Dialogues.info("Export réussi", rapport.lignes().size() + " ligne(s) exportée(s) dans :\n" + fichier
                + (document.demo() ? "\n\nAttention : données de démonstration." : ""));
    }
}
