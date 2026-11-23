package ma.translogix.controller.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;
import ma.translogix.controller.common.MiseEnPageRapport.Tranche;
import ma.translogix.service.Rapport;
import ma.translogix.util.Session;

/**
 * Prints a Rapport as a real document, not a picture of the screen: letterhead, title, filters, summary,
 * a paginated table (header row repeated on every page) and "Page x / n". Choosing the
 * "Microsoft Print to PDF" printer in the print dialog saves it as a PDF (no extra library needed).
 * Pages are drawn with inline styles: they are not part of the app's scene, so app.css does not apply.
 */
public final class ImpressionRapport {

    /** What to print. details: one line per active filter; demo: sample values, said on every page. */
    public record Document(Rapport rapport, List<String> details, boolean demo) {
        public Document {
            details = details == null ? List.of() : List.copyOf(details);
        }

        public Document(Rapport rapport) {
            this(rapport, List.of(), false);
        }
    }

    private static final double HAUTEUR_LIGNE = 17;
    private static final double HAUTEUR_ENTETE_TABLEAU = 20;
    private static final double TAILLE_POLICE = 8.5;
    private static final String ORANGE = "#f97316";
    private static final String GRIS = "#64748b";
    private static final String BORDURE = "#e2e8f0";
    private static final DateTimeFormatter EDITION = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    private static final Pattern NOMBRE = Pattern.compile("-?[\\d ]+([.,]\\d+)?( ?%| MAD)?");

    private ImpressionRapport() {
    }

    /** Opens the print dialog, then prints every page. Errors are shown in a dialog. */
    public static void imprimer(Document document, Window fenetre) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            Dialogues.erreur("Aucune imprimante n'est disponible sur ce poste "
                    + "(même « Microsoft Print to PDF » est absente).");
            return;
        }
        Printer imprimante = job.getPrinter();
        boolean paysage = MiseEnPageRapport.paysage(document.rapport().entetes().size());
        job.getJobSettings().setPageLayout(imprimante.createPageLayout(Paper.A4,
                paysage ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT));
        job.getJobSettings().setJobName("TransLogix - " + document.rapport().titre());
        if (!job.showPrintDialog(fenetre)) {
            return; // cancelled
        }
        PageLayout format = job.getJobSettings().getPageLayout(); // the user may have changed printer or paper
        List<Region> pages = pages(document, format.getPrintableWidth(), format.getPrintableHeight(),
                LocalDateTime.now(), auteur());
        for (Region page : pages) {
            if (!job.printPage(format, page)) {
                job.cancelJob();
                Dialogues.erreur("L'impression a échoué. Vérifiez l'imprimante puis réessayez.");
                return;
            }
        }
        job.endJob();
    }

    /** The printed pages, laid out for a printable area of largeur x hauteur points. Also used for previews. */
    public static List<Region> pages(Document document, double largeur, double hauteur,
                                     LocalDateTime edition, String auteur) {
        Rapport r = document.rapport();
        double[] largeurs = MiseEnPageRapport.largeurs(r.entetes(), r.lignes(), largeur);
        boolean[] aDroite = colonnesNumeriques(r);

        double hauteurFixe = mesurer(bandeau(edition, auteur), largeur) + mesurer(pied(r.titre(), 1, 1), largeur)
                + HAUTEUR_ENTETE_TABLEAU + 16; // spacing between blocks
        double hauteurTitre = mesurer(blocTitre(document), largeur) + 8;
        int premiere = MiseEnPageRapport.lignesQuiTiennent(hauteur - hauteurFixe - hauteurTitre, HAUTEUR_LIGNE);
        int suivantes = MiseEnPageRapport.lignesQuiTiennent(hauteur - hauteurFixe, HAUTEUR_LIGNE);
        List<Tranche> tranches = MiseEnPageRapport.pages(r.lignes().size(), premiere, suivantes);

        List<Region> pages = new ArrayList<>();
        for (int i = 0; i < tranches.size(); i++) {
            VBox page = new VBox(8);
            page.setStyle("-fx-background-color: white;");
            page.getChildren().add(bandeau(edition, auteur));
            if (i == 0) {
                page.getChildren().add(blocTitre(document));
            }
            page.getChildren().add(tableau(r, tranches.get(i), largeurs, aDroite));
            Region vide = new Region();
            VBox.setVgrow(vide, Priority.ALWAYS);
            page.getChildren().addAll(vide, pied(r.titre() + (document.demo() ? " — DÉMO" : ""), i + 1,
                    tranches.size()));
            page.setMinSize(largeur, hauteur);
            page.setPrefSize(largeur, hauteur);
            page.setMaxSize(largeur, hauteur);
            preparer(page);
            pages.add(page);
        }
        return pages;
    }

    // ------------------------------------------------------------------ blocks

    private static HBox bandeau(LocalDateTime edition, String auteur) {
        Label nom = texte("TransLogix", 14, true, "#172033");
        Label activite = texte("Gestion des opérations de transport", 8, false, GRIS);
        VBox gauche = new VBox(1, nom, activite);
        Label quand = texte("Édité le " + edition.format(EDITION), 8, false, GRIS);
        Label qui = texte("par " + auteur, 8, false, GRIS);
        VBox droite = new VBox(1, quand, qui);
        droite.setAlignment(Pos.TOP_RIGHT);
        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox bandeau = new HBox(gauche, espace, droite);
        bandeau.setPadding(new Insets(0, 0, 6, 0));
        bandeau.setStyle("-fx-border-color: transparent transparent " + ORANGE + " transparent; -fx-border-width: 0 0 2 0;");
        return bandeau;
    }

    private static VBox blocTitre(Document document) {
        Rapport r = document.rapport();
        VBox bloc = new VBox(3);
        bloc.getChildren().add(texte(r.titre(), 16, true, "#172033"));
        for (String detail : document.details()) {
            bloc.getChildren().add(texte(detail, 9, false, GRIS));
        }
        if (r.resume() != null && !r.resume().isBlank()) {
            bloc.getChildren().add(texte(r.resume(), 9, true, "#172033"));
        }
        if (document.demo()) {
            bloc.getChildren().add(texte("Attention : cette page contient des données de démonstration, "
                    + "pas des données réelles de l'entreprise.", 9, true, "#dc2626"));
        }
        bloc.getChildren().forEach(n -> ((Label) n).setWrapText(true));
        return bloc;
    }

    private static VBox tableau(Rapport r, Tranche tranche, double[] largeurs, boolean[] aDroite) {
        VBox tableau = new VBox();
        tableau.setStyle("-fx-border-color: " + BORDURE + "; -fx-border-width: 1;");
        HBox entete = ligne(r.entetes(), largeurs, aDroite, HAUTEUR_ENTETE_TABLEAU, true);
        entete.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: transparent transparent "
                + BORDURE + " transparent;");
        tableau.getChildren().add(entete);
        if (r.lignes().isEmpty()) {
            Label vide = texte("Aucune ligne ne correspond aux critères.", TAILLE_POLICE, false, GRIS);
            vide.setPadding(new Insets(6));
            tableau.getChildren().add(vide);
        }
        for (int i = tranche.de(); i < tranche.a(); i++) {
            HBox ligne = ligne(r.lignes().get(i), largeurs, aDroite, HAUTEUR_LIGNE, false);
            if (i % 2 == 1) {
                ligne.setStyle("-fx-background-color: #f8fafc;");
            }
            tableau.getChildren().add(ligne);
        }
        return tableau;
    }

    private static HBox ligne(List<String> cellules, double[] largeurs, boolean[] aDroite, double hauteur,
                              boolean entete) {
        HBox ligne = new HBox();
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setMinHeight(hauteur);
        ligne.setPrefHeight(hauteur);
        ligne.setMaxHeight(hauteur);
        for (int i = 0; i < largeurs.length; i++) {
            String valeur = i < cellules.size() && cellules.get(i) != null ? cellules.get(i) : "";
            Label cellule = texte(entete ? valeur.toUpperCase() : valeur, entete ? 7.5 : TAILLE_POLICE, entete,
                    entete ? "#475569" : "#172033");
            cellule.setTextOverrun(OverrunStyle.ELLIPSIS);
            cellule.setPadding(new Insets(0, 4, 0, 4));
            cellule.setMinWidth(largeurs[i]);
            cellule.setPrefWidth(largeurs[i]);
            cellule.setMaxWidth(largeurs[i]);
            cellule.setAlignment(aDroite[i] && !entete ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            ligne.getChildren().add(cellule);
        }
        return ligne;
    }

    private static HBox pied(String titre, int numero, int total) {
        Label gauche = texte("TransLogix · " + titre, 7.5, false, GRIS);
        Label droite = texte("Page " + numero + " / " + total, 7.5, false, GRIS);
        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox pied = new HBox(gauche, espace, droite);
        pied.setPadding(new Insets(4, 0, 0, 0));
        pied.setStyle("-fx-border-color: " + BORDURE + " transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        return pied;
    }

    // ------------------------------------------------------------------ helpers

    /** A column is right-aligned when every non-empty value looks like a number or an amount. */
    private static boolean[] colonnesNumeriques(Rapport r) {
        boolean[] resultat = new boolean[r.entetes().size()];
        for (int i = 0; i < resultat.length; i++) {
            int colonne = i;
            List<String> valeurs = r.lignes().stream()
                    .map(l -> colonne < l.size() ? l.get(colonne) : null)
                    .filter(v -> v != null && !v.isBlank())
                    .toList();
            resultat[i] = !valeurs.isEmpty() && valeurs.stream().allMatch(v -> NOMBRE.matcher(v.trim()).matches());
        }
        return resultat;
    }

    private static Label texte(String valeur, double taille, boolean gras, String couleur) {
        Label label = new Label(valeur);
        label.setFont(Font.font("System", gras ? FontWeight.BOLD : FontWeight.NORMAL, taille));
        label.setStyle("-fx-text-fill: " + couleur + ";");
        return label;
    }

    /** Height a block takes at this width. */
    private static double mesurer(Region bloc, double largeur) {
        preparer(bloc);
        return bloc.prefHeight(largeur);
    }

    /** A node outside any window needs a scene to get its styles and sizes before printing. */
    private static void preparer(Region region) {
        if (region.getScene() == null) {
            new Scene(new Group(region));
        }
        region.applyCss();
        region.layout();
    }

    private static String auteur() {
        return Session.estConnecte() ? Session.utilisateur().getNomComplet() : "—";
    }
}
