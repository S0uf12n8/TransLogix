package ma.translogix.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Free-text matching for search fields: ignores case and accents ("evenement" finds "Événement"). */
public final class Recherche {
    private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");

    private Recherche() {
    }

    /** True when the query is blank, or when at least one field contains it. */
    public static boolean correspond(String requete, String... champs) {
        String cible = normaliser(requete);
        if (cible.isEmpty()) {
            return true;
        }
        for (String champ : champs) {
            if (champ != null && normaliser(champ).contains(cible)) {
                return true;
            }
        }
        return false;
    }

    static String normaliser(String texte) {
        if (texte == null) {
            return "";
        }
        String sansAccents = ACCENTS.matcher(Normalizer.normalize(texte, Normalizer.Form.NFD)).replaceAll("");
        return sansAccents.trim().toLowerCase(Locale.ROOT);
    }
}
