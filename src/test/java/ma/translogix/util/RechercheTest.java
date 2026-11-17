package ma.translogix.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RechercheTest {

    @Test
    void ignoreCasseEtAccents() {
        assertTrue(Recherche.correspond("evenement", "Un Événement"));
        assertTrue(Recherche.correspond("GÉRANT", "gerant"));
        assertTrue(Recherche.correspond("  fes ", "Rabat", "Fès"));
    }

    @Test
    void requeteVideCorrespondATout() {
        assertTrue(Recherche.correspond("", "x"));
        assertTrue(Recherche.correspond(null, "x"));
        assertTrue(Recherche.correspond("   "));
    }

    @Test
    void aucunChampNeContientLaRequete() {
        assertFalse(Recherche.correspond("tanger", "Casablanca", null, "Agadir"));
        assertFalse(Recherche.correspond("x"));
    }
}
