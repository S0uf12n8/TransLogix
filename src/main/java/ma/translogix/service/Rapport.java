package ma.translogix.service;

import java.util.List;

/** A report ready to show and export: a table, a one-line summary and a suggested file name. */
public record Rapport(String titre, String nomFichier, List<String> entetes,
                      List<List<String>> lignes, String resume) {
    public Rapport {
        entetes = List.copyOf(entetes);
        lignes = lignes.stream().map(List::copyOf).toList();
    }
}
