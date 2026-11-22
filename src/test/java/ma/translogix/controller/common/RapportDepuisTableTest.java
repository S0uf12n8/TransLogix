package ma.translogix.controller.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import ma.translogix.model.Role;
import org.junit.jupiter.api.Test;

/** The text rules only (no JavaFX toolkit needed). */
class RapportDepuisTableTest {

    @Test
    void valeursSimplesDevenuesTexte() {
        assertEquals("", RapportDepuisTable.texte(null));
        assertEquals("Oui", RapportDepuisTable.texte(true));
        assertEquals("21/11/2026", RapportDepuisTable.texte(LocalDate.of(2026, 11, 21)));
        assertEquals("21/11/2026 09:05", RapportDepuisTable.texte(LocalDateTime.of(2026, 11, 21, 9, 5)));
        assertEquals("Ressources humaines", RapportDepuisTable.texte(Role.RH));
        assertEquals("ligne 1 ligne 2", RapportDepuisTable.texte("ligne 1\nligne 2"));
    }

    @Test
    void objetsComposesNeSontPasImprimes() {
        assertTrue(RapportDepuisTable.estSimple("texte"));
        assertTrue(RapportDepuisTable.estSimple(12));
        assertTrue(RapportDepuisTable.estSimple(Role.GERANT));
        assertFalse(RapportDepuisTable.estSimple(new Object()));
    }

    @Test
    void enTetesEnMajusculesAdoucis() {
        assertEquals("Réf. mission", RapportDepuisTable.enTete("RÉF. MISSION"));
        assertEquals("Date & heure", RapportDepuisTable.enTete("DATE & HEURE"));
        assertEquals("Montant HT", RapportDepuisTable.enTete("Montant HT"));
    }

    @Test
    void nomDeFichierSansAccentNiEspace() {
        String nom = RapportDepuisTable.nomFichier("Journal d'audit — Écran");

        assertTrue(nom.matches("journal-d-audit-ecran-\\d{4}-\\d{2}-\\d{2}\\.csv"), nom);
    }
}
