package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.util.List;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RapportServiceTest {

    private UtilisateurService utilisateurs;
    private RapportService service;

    @BeforeEach
    void setUp() {
        AuditService audit = new AuditService(new InMemoryAuditDao(), Clock.systemUTC(), () -> "Test");
        utilisateurs = new UtilisateurService(new InMemoryUtilisateurDao(), audit);
        service = new RapportService(utilisateurs);
    }

    @Test
    void rapportPersonnelUtiliseLesVraisUtilisateurs() {
        Utilisateur sara = utilisateurs.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);
        utilisateurs.modifier(sara.getId(), "Alami", "Sara", "sara@translogix.ma", Role.DISPATCHER, false);
        utilisateurs.creer("Bennani", "Ali", "ali@translogix.ma", "motdepasse1", Role.RH);

        Rapport r = service.conformiteRh();

        assertFalse(r.demo());
        assertEquals(List.of("Nom", "Prénom", "Email", "Rôle", "Statut"), r.entetes());
        assertEquals(List.of(
                List.of("Alami", "Sara", "sara@translogix.ma", "Dispatcher", "Inactif"),
                List.of("Bennani", "Ali", "ali@translogix.ma", "Ressources humaines", "Actif")), r.lignes());
        assertFalse(r.nomFichier().contains("DEMO"));
    }

    @Test
    void rapportsSansServiceSontMarquesDemo() {
        for (Rapport r : List.of(service.rentabilite(), service.gazole())) {
            assertTrue(r.demo(), r.titre());
            assertTrue(r.nomFichier().endsWith("-DEMO.csv"), r.nomFichier());
            assertFalse(r.lignes().isEmpty());
            r.lignes().forEach(l -> assertEquals(r.entetes().size(), l.size()));
        }
    }
}
