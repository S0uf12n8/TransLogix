package ma.translogix.controller.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import ma.translogix.model.Role;
import org.junit.jupiter.api.Test;

class NavigationMenuTest {

    @Test
    void leGerantAUnMenu() {
        assertFalse(NavigationMenu.pour(Role.GERANT).isEmpty());
    }

    @Test
    void lesEcransLivresDuGerantExistent() {
        assertNotNull(getClass().getResource(NavigationMenu.RACINE + "admin/dashboard.fxml"));
        assertNotNull(getClass().getResource(NavigationMenu.RACINE + "admin/utilisateurs.fxml"));
        assertNotNull(getClass().getResource(NavigationMenu.RACINE + "admin/utilisateur-dialog.fxml"));
        assertNotNull(getClass().getResource(NavigationMenu.RACINE + "admin/rapports.fxml"));
        assertNotNull(getClass().getResource(NavigationMenu.RACINE + "admin/audit.fxml"));
    }

    @Test
    void chaqueEntreeDuGerantAUnKickerEtUneIcone() {
        for (NavigationMenu.Entree e : NavigationMenu.pour(Role.GERANT)) {
            assertNotNull(e.kicker(), e.libelle());
            assertNotNull(e.icone(), e.libelle());
        }
    }

    @Test
    void seulLeTableauDeBordEstEnDemo() {
        assertEquals(1, NavigationMenu.pour(Role.GERANT).stream().filter(NavigationMenu.Entree::demo).count());
        assertTrue(NavigationMenu.entreePour(Role.GERANT, "admin/dashboard.fxml").orElseThrow().demo());
    }

    @Test
    void entreePourTrouveLeFichierExact() {
        assertEquals("Journal d'audit",
                NavigationMenu.entreePour(Role.GERANT, "admin/audit.fxml").orElseThrow().libelle());
    }

    @Test
    void entreePourRetombeSurLeMemeDossier() {
        assertEquals("Ressources humaines",
                NavigationMenu.entreePour(Role.GERANT, "rh/conformite.fxml").orElseThrow().libelle());
    }

    @Test
    void entreePourVidePourUnDossierInconnu() {
        assertTrue(NavigationMenu.entreePour(Role.GERANT, "chauffeur/missions.fxml").isEmpty());
        assertTrue(NavigationMenu.entreePour(Role.GERANT, "sans-dossier.fxml").isEmpty());
        assertTrue(NavigationMenu.entreePour(Role.CHAUFFEUR, "admin/audit.fxml").isEmpty());
    }

    @Test
    void leMenuRhSuitLOrdreDeLaMaquette() {
        assertEquals(List.of("rh/dashboard.fxml", "rh/employes.fxml", "rh/conformite.fxml", "rh/conges.fxml",
                        "rh/primes.fxml", "rh/recrutement.fxml"),
                NavigationMenu.pour(Role.RH).stream().map(NavigationMenu.Entree::fxml).toList());
    }

    @Test
    void lesEcransDuMenuRhExistent() {
        for (NavigationMenu.Entree e : NavigationMenu.pour(Role.RH)) {
            assertNotNull(getClass().getResource(NavigationMenu.RACINE + e.fxml()), e.fxml());
            assertNotNull(e.kicker(), e.libelle());
            assertNotNull(e.icone(), e.libelle());
        }
        assertNotNull(getClass().getResource(NavigationMenu.RACINE + "rh/employe-dialog.fxml"));
        assertNotNull(getClass().getResource(NavigationMenu.RACINE + "rh/document-dialog.fxml"));
    }

    @Test
    void seulesLesPagesRhDeLIteration3SontEnDemo() {
        assertEquals(List.of("rh/conges.fxml", "rh/primes.fxml", "rh/recrutement.fxml"),
                NavigationMenu.pour(Role.RH).stream().filter(NavigationMenu.Entree::demo)
                        .map(NavigationMenu.Entree::fxml).toList());
    }

    @Test
    void leGerantOuvreLesPagesRhLivrees() {
        assertEquals("rh/employes.fxml",
                NavigationMenu.entreePour(Role.GERANT, "rh/employes.fxml").orElseThrow().fxml());
        assertNotNull(getClass().getResource(NavigationMenu.RACINE + "rh/conformite.fxml"));
        assertEquals("Conformité & permis",
                NavigationMenu.entreePour(Role.RH, "rh/conformite.fxml").orElseThrow().libelle());
    }

    @Test
    void formeCourteDEntreeResteDisponible() {
        NavigationMenu.Entree e = new NavigationMenu.Entree("Missions", "dispatcher/missions.fxml");
        assertFalse(e.demo());
        assertEquals(null, e.kicker());
    }
}
