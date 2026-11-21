package ma.translogix.controller.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout arithmetic of a printed report (no JavaFX, unit-tested): column widths and page breaks.
 * ImpressionRapport draws the pages with these numbers.
 */
public final class MiseEnPageRapport {

    /** A column never gets less than this many characters of width, nor more than the maximum. */
    static final int CARACTERES_MIN = 4;
    static final int CARACTERES_MAX = 38;

    /** Rows of a page: from (included) to (excluded), indexes in the report rows. */
    public record Tranche(int de, int a) {
        public int taille() {
            return a - de;
        }
    }

    private MiseEnPageRapport() {
    }

    /**
     * Column widths proportional to their longest text (header included), each clamped between
     * CARACTERES_MIN and CARACTERES_MAX characters, scaled so they fill exactly largeurTotale.
     */
    public static double[] largeurs(List<String> entetes, List<List<String>> lignes, double largeurTotale) {
        int n = entetes.size();
        double[] poids = new double[n];
        double somme = 0;
        for (int i = 0; i < n; i++) {
            int plusLong = longueur(entetes.get(i));
            for (List<String> ligne : lignes) {
                if (i < ligne.size()) {
                    plusLong = Math.max(plusLong, longueur(ligne.get(i)));
                }
            }
            poids[i] = Math.max(CARACTERES_MIN, Math.min(CARACTERES_MAX, plusLong));
            somme += poids[i];
        }
        double[] largeurs = new double[n];
        for (int i = 0; i < n; i++) {
            largeurs[i] = somme == 0 ? 0 : largeurTotale * poids[i] / somme;
        }
        return largeurs;
    }

    /**
     * Splits nbLignes rows into pages: the first page holds fewer rows (title block), the others more.
     * An empty report still prints one page (with "Aucune ligne").
     * @throws IllegalArgumentException if a page cannot hold at least one row
     */
    public static List<Tranche> pages(int nbLignes, int lignesPremierePage, int lignesParPage) {
        if (lignesPremierePage < 1 || lignesParPage < 1) {
            throw new IllegalArgumentException("Page trop petite pour imprimer une ligne.");
        }
        List<Tranche> pages = new ArrayList<>();
        int debut = 0;
        int capacite = lignesPremierePage;
        do {
            int fin = Math.min(nbLignes, debut + capacite);
            pages.add(new Tranche(debut, fin));
            debut = fin;
            capacite = lignesParPage;
        } while (debut < nbLignes);
        return pages;
    }

    /** How many rows of height hauteurLigne fit in hauteurDisponible (at least 1). */
    public static int lignesQuiTiennent(double hauteurDisponible, double hauteurLigne) {
        return Math.max(1, (int) Math.floor(hauteurDisponible / hauteurLigne));
    }

    /** More than 6 columns: landscape, so the table stays readable. */
    public static boolean paysage(int nbColonnes) {
        return nbColonnes > 6;
    }

    private static int longueur(String texte) {
        return texte == null ? 0 : texte.length();
    }
}
