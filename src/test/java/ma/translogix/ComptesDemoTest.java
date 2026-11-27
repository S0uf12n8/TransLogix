package ma.translogix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.util.List;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.service.AuditService;
import ma.translogix.service.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ComptesDemoTest {

    private InMemoryUtilisateurDao dao;
    private AuditService audit;
    private UtilisateurService service;

    @BeforeEach
    void preparer() {
        dao = new InMemoryUtilisateurDao();
        audit = new AuditService(new InMemoryAuditDao(), Clock.systemDefaultZone(), () -> "Système");
        service = new UtilisateurService(dao, audit);
    }

    @Test
    void baseVideRecoitUnCompteParRole() {
        assertTrue(ComptesDemo.semer(dao, service));

        List<Role> roles = dao.findAll().stream().map(Utilisateur::getRole).sorted().toList();
        assertEquals(List.of(Role.values()).stream().sorted().toList(), roles);
        assertTrue(dao.findByEmail(ComptesDemo.EMAIL_CHAUFFEUR).isPresent());
        assertEquals(5, audit.lister().size());
    }

    @Test
    void secondDemarrageNeRecreeRienEtNeLeveAucuneErreur() {
        ComptesDemo.semer(dao, service);

        assertFalse(ComptesDemo.semer(dao, service));

        assertEquals(5, dao.findAll().size());
        assertEquals(5, audit.lister().size());
    }

    @Test
    void unCompteDemoModifieParLeGerantNEstPasReinitialise() {
        ComptesDemo.semer(dao, service);
        Utilisateur rh = dao.findByEmail("rh@translogix.ma").orElseThrow();
        String hashModifie = rh.getMotDePasseHash();
        service.changerStatut(rh.getId(), false);

        ComptesDemo.semer(dao, service);

        Utilisateur apres = dao.findByEmail("rh@translogix.ma").orElseThrow();
        assertFalse(apres.isActif(), "un compte désactivé reste désactivé après redémarrage");
        assertEquals(hashModifie, apres.getMotDePasseHash());
    }

    @Test
    void baseAvecUnSeulCompteReelNeRecoitPasLesComptesDemo() {
        service.creer("Alaoui", "Nadia", "n.alaoui@exemple.ma", "motdepasse1", Role.GERANT);

        assertFalse(ComptesDemo.semer(dao, service));

        assertEquals(1, dao.findAll().size());
    }
}
