package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.model.Client;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The driver space ([O3]): only his missions, only "start" and "deliver". */
class EspaceChauffeurServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC);
    private static final Long COMPTE_ALPHA = 501L;
    private static final Long COMPTE_SANS_FICHE = 999L;

    private MissionsDeTest flotte;
    private Long client;
    private Employe beta;
    private Long compteConnecte;
    private EspaceChauffeurService espace;

    @BeforeEach
    void setUp() {
        InMemoryClientDao clientDao = new InMemoryClientDao();
        flotte = new MissionsDeTest(clientDao, new InMemoryFactureDao(), HORLOGE);
        client = clientDao.save(new Client(null, "Client Test", null, null, null, null, 30)).getId();
        flotte.employes.lierCompte(flotte.chauffeur.getId(), COMPTE_ALPHA);
        beta = flotte.nouveauChauffeur("Beta");
        compteConnecte = COMPTE_ALPHA;
        espace = new EspaceChauffeurService(flotte.missions, flotte.employeDao, () -> compteConnecte);
    }

    @Test
    void leChauffeurNeVoitQueSesMissions() {
        Mission a1 = flotte.planifiee(client);
        flotte.planifiee(client, beta);
        Mission a2 = flotte.planifiee(client);
        flotte.missions.demarrer(a2.getId());

        List<Long> vues = espace.mesMissions().stream().map(Mission::getId).toList();

        assertEquals(List.of(a2.getId(), a1.getId()), vues); // his mission on the road first
    }

    @Test
    void ilDemarrePuisLivreSaMissionViaMissionService() {
        Mission m = flotte.planifiee(client);

        espace.demarrer(m.getId());
        assertEquals(StatutMission.EN_COURS, flotte.missions.trouver(m.getId()).getStatut());
        espace.livrer(m.getId());
        assertEquals(StatutMission.LIVREE, flotte.missions.trouver(m.getId()).getStatut());
    }

    @Test
    void ilNePeutPasChangerLaMissionDUnAutreChauffeur() {
        Mission deBeta = flotte.planifiee(client, beta);

        ValidationException e = assertThrows(ValidationException.class, () -> espace.demarrer(deBeta.getId()));
        assertEquals("La mission " + deBeta.getReference() + " est affectée à un autre chauffeur.", e.getMessage());
        assertEquals(StatutMission.PLANIFIEE, flotte.missions.trouver(deBeta.getId()).getStatut());
    }

    @Test
    void ilNePeutNiFacturerNiAnnuler() {
        Mission enCours = flotte.planifiee(client);
        espace.demarrer(enCours.getId());
        Mission livree = flotte.planifiee(client);
        espace.demarrer(livree.getId());
        espace.livrer(livree.getId());

        assertThrows(ValidationException.class, () -> espace.changerStatut(enCours.getId(), StatutMission.ANNULEE));
        assertThrows(ValidationException.class, () -> espace.changerStatut(livree.getId(), StatutMission.FACTUREE));
        assertEquals(StatutMission.EN_COURS, flotte.missions.trouver(enCours.getId()).getStatut());
        assertEquals(StatutMission.LIVREE, flotte.missions.trouver(livree.getId()).getStatut());
    }

    @Test
    void lesTransitionsInterditesRestentRefuseesParMissionService() {
        Mission m = flotte.planifiee(client);

        assertThrows(ValidationException.class, () -> espace.livrer(m.getId())); // PLANIFIEE -> LIVREE
        assertEquals(StatutMission.PLANIFIEE, flotte.missions.trouver(m.getId()).getStatut());
    }

    @Test
    void unCompteSansFicheChauffeurEstRefuse() {
        compteConnecte = COMPTE_SANS_FICHE;

        assertThrows(ValidationException.class, () -> espace.mesMissions());
    }
}
