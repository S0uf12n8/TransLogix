package ma.translogix.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvExporterTest {

    @Test
    void enteteEtLignesAvecBomPointVirguleEtCrlf() {
        String csv = CsvExporter.versTexte(List.of("Nom", "Rôle"), List.of(List.of("Alami", "Gérant")));

        assertEquals("﻿Nom;Rôle\r\nAlami;Gérant\r\n", csv);
    }

    @Test
    void pointVirguleGuillemetsEtRetoursLigneSontProteges() {
        assertEquals("\"a;b\"", CsvExporter.cellule("a;b"));
        assertEquals("\"il a dit \"\"oui\"\"\"", CsvExporter.cellule("il a dit \"oui\""));
        assertEquals("\"ligne1\nligne2\"", CsvExporter.cellule("ligne1\nligne2"));
        assertEquals("\"ligne1\r\nligne2\"", CsvExporter.cellule("ligne1\r\nligne2"));
        assertEquals("simple, avec virgule", CsvExporter.cellule("simple, avec virgule"));
    }

    @Test
    void videEtNullDonnentUneCelluleVide() {
        assertEquals("", CsvExporter.cellule(null));
        assertEquals("", CsvExporter.cellule(""));
        assertEquals("﻿a;;c\r\n", CsvExporter.versTexte(Arrays.asList("a", null, "c"), List.of()));
    }

    @Test
    void formulesSontNeutralisees() {
        assertEquals("'=1+1", CsvExporter.cellule("=1+1"));
        assertEquals("'+33 6 12", CsvExporter.cellule("+33 6 12"));
        assertEquals("'@SUM(A1)", CsvExporter.cellule("@SUM(A1)"));
        assertEquals("'-cmd", CsvExporter.cellule("-cmd"));
        assertEquals("\"'=HYPERLINK(\"\"http://x\"\";\"\"y\"\")\"",
                CsvExporter.cellule("=HYPERLINK(\"http://x\";\"y\")"));
    }

    @Test
    void nombresNegatifsRestentDesNombres() {
        assertEquals("-14", CsvExporter.cellule("-14"));
        assertEquals("-1500,50", CsvExporter.cellule("-1500,50"));
        assertEquals("+3.5", CsvExporter.cellule("+3.5"));
    }

    @Test
    void ecrireProduitUnFichierUtf8AvecBom(@TempDir Path dossier) throws Exception {
        Path fichier = dossier.resolve("rapport.csv");

        CsvExporter.ecrire(fichier, List.of("Ville"), List.of(List.of("Fès")));

        byte[] octets = Files.readAllBytes(fichier);
        assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, Arrays.copyOf(octets, 3));
        String contenu = new String(octets, StandardCharsets.UTF_8);
        assertTrue(contenu.endsWith("Ville\r\nFès\r\n"));
    }
}
