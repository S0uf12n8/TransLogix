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
    void aucunePageDuGerantNEstEnDemo() {
        // The dashboard reads real figures since Iteration 3 (KPI cards, charts, alerts, recent trips).
        assertEquals(0, NavigationMenu.pour(Role.GERANT).stream().filter(NavigationMenu.Entree::demo).count());
        assertFalse(NavigationMenu.entreePour(Role.GERANT, "admin/dashboard.fxml").orElseThrow().demo());
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

    @Test
    void leMenuDispatcherSuitLOrdreDeLaMaquette() {
        assertEquals(List.of("dispatcher/missions.fxml", "dispatcher/flotte.fxml", "dispatcher/planning.fxml",
                        "dispatcher/incidents.fxml", "dispatcher/carburant.fxml"),
                NavigationMenu.pour(Role.DISPATCHER).stream().map(NavigationMenu.Entree::fxml).toList());
        assertEquals("EXPLOITATION LOGISTIQUE", NavigationMenu.sousTitre(Role.DISPATCHER));
    }

    @Test
    void lesEcransDuMenuDispatcherExistent() {
        for (NavigationMenu.Entree e : NavigationMenu.pour(Role.DISPATCHER)) {
            assertNotNull(getClass().getResource(NavigationMenu.RACINE + e.fxml()), e.fxml());
            assertNotNull(e.kicker(), e.libelle());
            assertNotNull(e.icone(), e.libelle());
        }
        assertNotNull(getClass().getResource(NavigationMenu.RACINE + "dispatcher/mission-dialog.fxml"));
        assertNotNull(getClass().getResource(NavigationMenu.RACINE + "dispatcher/vehicule-dialog.fxml"));
    }

    @Test
    void seulesLesPagesDispatcherDeLIteration3SontEnDemo() {
        assertEquals(List.of("dispatcher/incidents.fxml", "dispatcher/carburant.fxml"),
                NavigationMenu.pour(Role.DISPATCHER).stream().filter(NavigationMenu.Entree::demo)
                        .map(NavigationMenu.Entree::fxml).toList());
    }

    @Test
    void leGerantOuvreLeDispatchingEtLaFlotte() {
        assertEquals("Dispatching & flotte",
                NavigationMenu.entreePour(Role.GERANT, "dispatcher/missions.fxml").orElseThrow().libelle());
        assertEquals("Dispatching & flotte",
                NavigationMenu.entreePour(Role.GERANT, "dispatcher/flotte.fxml").orElseThrow().libelle());
    }

    @Test
    void leMenuChauffeurSuitLOrdreDeLaMaquette() {
        assertEquals(List.of("Tableau de bord", "Mes ordres de transport", "Reçus & carburant",
                        "Permis & documents", "Incidents & support", "Notifications", "Détail de mission",
                        "Preuve de livraison", "Inspection du camion", "Messagerie Dispatching",
                        "Intervention exceptionnelle", "Historique des missions"),
                NavigationMenu.pour(Role.CHAUFFEUR).stream().map(NavigationMenu.Entree::libelle).toList());
        assertEquals("ESPACE CHAUFFEUR", NavigationMenu.sousTitre(Role.CHAUFFEUR));
    }

    @Test
    void tousLesEcransChauffeurExistent() {
        List<String> pages = NavigationMenu.pour(Role.CHAUFFEUR).stream()
                .map(NavigationMenu.Entree::fxml).toList();
        assertEquals(12, pages.size());
        for (NavigationMenu.Entree e : NavigationMenu.pour(Role.CHAUFFEUR)) {
            assertNotNull(getClass().getResource(NavigationMenu.RACINE + e.fxml()), e.fxml());
            assertNotNull(e.kicker(), e.libelle());
            assertNotNull(e.icone(), e.libelle());
        }
        assertTrue(pages.contains("chauffeur/missions.fxml"));
        assertTrue(pages.contains("chauffeur/mission-detail.fxml"));
        assertTrue(pages.contains("chauffeur/history.fxml"));
    }

    @Test
    void ecranDeConnexionExiste() {
        assertNotNull(getClass().getResource(NavigationMenu.RACINE + "auth/login.fxml"));
    }
}
