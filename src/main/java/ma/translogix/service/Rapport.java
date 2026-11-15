package ma.translogix.service;

import java.util.List;

/** A report ready to export: a table plus a suggested file name. demo=true means sample values, not real data. */
public record Rapport(String titre, String nomFichier, List<String> entetes,
                      List<List<String>> lignes, boolean demo) {
    public Rapport {
        entetes = List.copyOf(entetes);
        lignes = lignes.stream().map(List::copyOf).toList();
    }
}
