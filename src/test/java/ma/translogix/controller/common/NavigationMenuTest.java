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
    void formeCourteDEntreeResteDisponible() {
        NavigationMenu.Entree e = new NavigationMenu.Entree("Missions", "dispatcher/missions.fxml");
        assertFalse(e.demo());
        assertEquals(null, e.kicker());
    }
    @Test
    void menuComptableDansLOrdreDeLaMaquette() {
        assertEquals(List.of("Tableau de bord", "Facturation clients", "Clients", "Valider dépenses",
                        "Créances & recouvrement", "Trésorerie & bilan", "Paiement des employés", "Journal comptable"),
                NavigationMenu.pour(Role.COMPTABLE).stream().map(NavigationMenu.Entree::libelle).toList());
        for (NavigationMenu.Entree e : NavigationMenu.pour(Role.COMPTABLE)) {
            assertNotNull(e.kicker(), e.libelle());
            assertNotNull(e.icone(), e.libelle());
        }
    }

    @Test
    void lesEcransLivresDuComptableExistent() {
        for (String page : List.of("dashboard", "factures", "facture-dialog", "paiement-dialog", "clients",
                "client-dialog")) {
            assertNotNull(getClass().getResource(NavigationMenu.RACINE + "comptable/" + page + ".fxml"), page);
        }
    }

    @Test
    void comptabiliteDuGerantOuvreLaFacturation() {
        assertTrue(NavigationMenu.pour(Role.GERANT).stream()
                .anyMatch(e -> e.libelle().equals("Comptabilité & finance") && e.fxml().equals("comptable/factures.fxml")));
    }
}
