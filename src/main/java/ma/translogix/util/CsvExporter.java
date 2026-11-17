package ma.translogix.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Writes CSV files that Excel (French locale) opens correctly:
 * UTF-8 with BOM, ";" separator, CRLF line ends, RFC 4180 quoting.
 * Text cells starting with = + - @ TAB or CR get a leading apostrophe so Excel does not run
 * them as formulas (CSV injection); plain numbers such as -14 or 1500,5 are left untouched.
 */
public final class CsvExporter {
    public static final char SEPARATEUR = ';';
    public static final String BOM = "﻿";
    private static final String FIN_LIGNE = "\r\n";
    private static final String DEBUTS_DANGEREUX = "=+-@\t\r";
    private static final Pattern NOMBRE = Pattern.compile("[-+]?\\d+([.,]\\d+)?");

    private CsvExporter() {
    }

    public static String versTexte(List<String> entetes, List<List<String>> lignes) {
        StringBuilder sb = new StringBuilder(BOM);
        List<List<String>> toutes = new ArrayList<>();
        toutes.add(entetes);
        toutes.addAll(lignes);
        for (List<String> ligne : toutes) {
            for (int i = 0; i < ligne.size(); i++) {
                if (i > 0) {
                    sb.append(SEPARATEUR);
                }
                sb.append(cellule(ligne.get(i)));
            }
            sb.append(FIN_LIGNE);
        }
        return sb.toString();
    }

    public static void ecrire(Path fichier, List<String> entetes, List<List<String>> lignes) throws IOException {
        Files.writeString(fichier, versTexte(entetes, lignes), StandardCharsets.UTF_8);
    }

    static String cellule(String valeur) {
        if (valeur == null || valeur.isEmpty()) {
            return "";
        }
        String v = valeur;
        if (DEBUTS_DANGEREUX.indexOf(v.charAt(0)) >= 0 && !NOMBRE.matcher(v).matches()) {
            v = "'" + v;
        }
        boolean aProteger = v.indexOf(SEPARATEUR) >= 0 || v.indexOf('"') >= 0
                || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0;
        return aProteger ? '"' + v.replace("\"", "\"\"") + '"' : v;
    }
}
