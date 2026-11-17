package ma.translogix.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NavigatorTest {

    /** Records what the Navigator asked the shell to do. */
    private static final class FauxHote implements Navigator.Hote {
        final List<String> affichees = new ArrayList<>();
        final List<String> nonLivrees = new ArrayList<>();

        @Override public void afficher(String fxml) { affichees.add(fxml); }
        @Override public void signalerNonLivre(String fxml) { nonLivrees.add(fxml); }
    }

    private FauxHote hote;

    @BeforeEach
    void setUp() {
        hote = new FauxHote();
        Navigator.enregistrer(hote);
    }

    @AfterEach
    void tearDown() {
        Navigator.liberer(hote);
    }

    @Test
    void pageExistanteEstAffichee() {
        assertTrue(Navigator.ouvrir("admin/dashboard.fxml"));

        assertEquals(List.of("admin/dashboard.fxml"), hote.affichees);
        assertTrue(hote.nonLivrees.isEmpty());
    }

    @Test
    void pageAbsenteEstSignaleeNonLivree() {
        assertFalse(Navigator.ouvrir("module-inexistant/page.fxml"));

        assertEquals(List.of("module-inexistant/page.fxml"), hote.nonLivrees);
        assertTrue(hote.affichees.isEmpty());
    }

    @Test
    void cheminInvalideEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> Navigator.ouvrir(null));
        assertThrows(IllegalArgumentException.class, () -> Navigator.ouvrir(" "));
        assertThrows(IllegalArgumentException.class, () -> Navigator.ouvrir("../css/app.css"));
        assertThrows(IllegalArgumentException.class, () -> Navigator.ouvrir("/admin/dashboard.fxml"));
    }

    @Test
    void sansHoteEnregistreOuvrirEchoue() {
        Navigator.liberer(hote);

        assertThrows(IllegalStateException.class, () -> Navigator.ouvrir("admin/dashboard.fxml"));
    }

    @Test
    void libererUnAncienHoteNeRetirePasLeNouveau() {
        FauxHote nouveau = new FauxHote();
        Navigator.enregistrer(nouveau);
        try {
            Navigator.liberer(hote); // old shell, e.g. after a re-login

            Navigator.ouvrir("admin/dashboard.fxml");
            assertEquals(1, nouveau.affichees.size());
            assertTrue(hote.affichees.isEmpty());
        } finally {
            Navigator.liberer(nouveau);
        }
    }
}
