package ma.translogix.controller.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import ma.translogix.service.Rapport;
import ma.translogix.util.Formats;

/**
 * Turns what a table shows into a Rapport: the visible columns and the rows as filtered and sorted on screen.
 * Columns whose values are not text (action buttons, composite cells) are left out rather than printed
 * as "Mission@1a2b". Pages whose best content is not a plain table implement Imprimable instead.
 */
public final class RapportDepuisTable {

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private RapportDepuisTable() {
    }

    /** The first table of the page that has rows or columns to print, as a report titled titre. */
    public static Optional<Rapport> depuisPage(Node page, String titre) {
        return page.lookupAll(".table-view").stream()
                .filter(n -> n instanceof TableView<?> && n.isVisible())
                .map(n -> (TableView<?>) n)
                .filter(t -> !t.getVisibleLeafColumns().isEmpty())
                .findFirst()
                .map(t -> depuis(t, titre));
    }

    public static <S> Rapport depuis(TableView<S> table, String titre) {
        List<String> entetes = new ArrayList<>();
        List<List<String>> colonnes = new ArrayList<>();
        for (TableColumn<S, ?> colonne : table.getVisibleLeafColumns()) {
            String entete = colonne.getText() == null ? "" : colonne.getText().trim();
            if (entete.isEmpty() || entete.equalsIgnoreCase("actions") || entete.equalsIgnoreCase("action")) {
                continue; // buttons, not data
            }
            List<String> valeurs = valeurs(table, colonne);
            if (valeurs.stream().anyMatch(v -> !v.isEmpty()) || table.getItems().isEmpty()) {
                entetes.add(enTete(entete));
                colonnes.add(valeurs);
            }
        }
        List<List<String>> lignes = new ArrayList<>();
        for (int i = 0; i < table.getItems().size(); i++) {
            List<String> ligne = new ArrayList<>();
            for (List<String> colonne : colonnes) {
                ligne.add(colonne.get(i));
            }
            lignes.add(ligne);
        }
        int n = lignes.size();
        return new Rapport(titre, nomFichier(titre), entetes, lignes, n + (n > 1 ? " lignes" : " ligne"));
    }

    /**
     * The column's text for every row: the value itself when it is simple text, otherwise what the column's
     * cell shows on screen (its text and the labels of its graphic, e.g. "Mohammed Bennani · 12345-A-6"),
     * so composite cells print their content instead of being dropped. Buttons inside cells are ignored.
     */
    private static <S, T> List<String> valeurs(TableView<S> table, TableColumn<S, T> colonne) {
        List<String> valeurs = new ArrayList<>();
        boolean simple = table.getItems().stream()
                .map(colonne::getCellData)
                .allMatch(v -> v == null || estSimple(v));
        TableCell<S, T> cellule = simple || colonne.getCellFactory() == null ? null : colonne.getCellFactory().call(colonne);
        if (cellule != null) {
            cellule.updateTableView(table);
            cellule.updateTableColumn(colonne);
        }
        for (int i = 0; i < table.getItems().size(); i++) {
            if (cellule == null) {
                valeurs.add(texte(colonne.getCellData(table.getItems().get(i))));
            } else {
                cellule.updateIndex(i);
                valeurs.add(texteAffiche(cellule));
            }
        }
        return valeurs;
    }

    /** Text of a rendered cell: its own text plus every label of its graphic, buttons excluded. */
    static String texteAffiche(Labeled cellule) {
        List<String> morceaux = new ArrayList<>();
        if (cellule.getText() != null && !cellule.getText().isBlank()) {
            morceaux.add(cellule.getText().trim());
        }
        collecter(cellule.getGraphic(), morceaux);
        return String.join(" · ", morceaux).replace("● ", "").replace('\n', ' ').trim();
    }

    private static void collecter(Node noeud, List<String> morceaux) {
        if (noeud == null || !noeud.isVisible() || noeud instanceof ButtonBase) {
            return;
        }
        if (noeud instanceof Labeled l && l.getText() != null && !l.getText().isBlank()) {
            morceaux.add(l.getText().trim());
        }
        if (noeud instanceof Text t && t.getText() != null && !t.getText().isBlank()) {
            morceaux.add(t.getText().trim());
        }
        if (noeud instanceof Parent p && !(noeud instanceof Labeled)) {
            p.getChildrenUnmodifiable().forEach(enfant -> collecter(enfant, morceaux));
        }
    }

    static boolean estSimple(Object valeur) {
        return valeur instanceof CharSequence || valeur instanceof Number || valeur instanceof Boolean
                || valeur instanceof Enum<?> || valeur instanceof TemporalAccessor;
    }

    static String texte(Object valeur) {
        if (valeur == null) {
            return "";
        }
        if (valeur instanceof Boolean b) {
            return b ? "Oui" : "Non";
        }
        if (valeur instanceof LocalDateTime t) {
            return t.format(DATE_HEURE);
        }
        if (valeur instanceof LocalDate d) {
            return Formats.date(d);
        }
        return valeur.toString().replace('\n', ' ').trim();
    }

    /** "RÉF. MISSION" -> "Réf. mission": tables shout their headers, a document does not. */
    static String enTete(String texte) {
        String t = texte == null ? "" : texte.trim();
        if (t.isEmpty() || !t.equals(t.toUpperCase(Locale.FRENCH))) {
            return t;
        }
        String minuscules = t.toLowerCase(Locale.FRENCH);
        return Character.toUpperCase(minuscules.charAt(0)) + minuscules.substring(1);
    }

    /** "Journal d'audit" -> "journal-d-audit-2026-11-21.csv". */
    static String nomFichier(String titre) {
        String base = java.text.Normalizer.normalize(titre == null ? "rapport" : titre, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return (base.isEmpty() ? "rapport" : base) + "-" + LocalDate.now() + ".csv";
    }
}
