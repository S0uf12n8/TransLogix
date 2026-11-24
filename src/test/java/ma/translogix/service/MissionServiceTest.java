package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryMissionDao;
import ma.translogix.dao.memory.InMemoryVehiculeDao;
import ma.translogix.model.Client;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.Poste;
import ma.translogix.model.StatutMission;
import ma.translogix.model.StatutVehicule;
import ma.translogix.model.TypeContrat;
import ma.translogix.model.Vehicule;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The no-overlap rule ([H4]) and the mission lifecycle. Fictional identifiers only. */
class MissionServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime H8 = LocalDateTime.of(2026, 11, 17, 8, 0);

    private VehiculeService vehicules;
    private EmployeService employes;
    private MissionService service;
    private Long client;
    private Vehicule camion1;
    private Vehicule camion2;
    private Employe chauffeur1;
    private Employe chauffeur2;

    @BeforeEach
    void setUp() {
        InMemoryVehiculeDao vehiculeDao = new InMemoryVehiculeDao();
        vehicules = new VehiculeService(vehiculeDao);
        employes = new EmployeService(new InMemoryEmployeDao(), HORLOGE);
        ClientService clients = new ClientService(new InMemoryClientDao(), new InMemoryFactureDao());
        service = new MissionService(new InMemoryMissionDao(), vehiculeDao, employes, clients, HORLOGE);

        client = clients.creer("Client Test", null, null, null, null, Client.DELAI_PAR_DEFAUT).getId();
        camion1 = vehicules.creer("11111-A-1", "Volvo", "FH", new BigDecimal("26"));
        camion2 = vehicules.creer("22222-B-2", "Scania", "R500", new BigDecimal("26"));
        chauffeur1 = chauffeur("Alpha", "ZZ-900001");
        chauffeur2 = chauffeur("Beta", "ZZ-900002");
    }

    private Employe chauffeur(String nom, String cin) {
        return employes.creer(nom, "Test", cin, null, Poste.CHAUFFEUR, TypeContrat.CDI, LocalDate.of(2024, 1, 1));
    }

    /** A mission from start to end hours of 17/11/2026. */
    private Mission creer(Vehicule v, Employe e, int debut, int fin) {
        return service.creer(client, v.getId(), e.getId(), "Casablanca", "Rabat", "Palettes",
                new BigDecimal("12"), H8.withHour(debut), H8.withHour(fin));
    }

    @Test
    void creeUneMissionPlanifieeAvecLaReferenceSuivante() {
        Mission a = creer(camion1, chauffeur1, 8, 12);
        Mission b = creer(camion2, chauffeur2, 8, 12);
        assertEquals("TRJ-9081", a.getReference());
        assertEquals("TRJ-9082", b.getReference());
        assertEquals(StatutMission.PLANIFIEE, a.getStatut());
    }

    @Test
    void refuseLeMemeVehiculeSurUnePeriodeQuiChevauche() {
        creer(camion1, chauffeur1, 8, 12);
        ValidationException e = assertThrows(ValidationException.class, () -> creer(camion1, chauffeur2, 11, 15));
        assertEquals("Chevauchement : le véhicule 11111-A-1 est déjà affecté à TRJ-9081 "
                + "(Casablanca → Rabat, du 17/11/2026 08:00 au 17/11/2026 12:00).", e.getMessage());
    }

    @Test
    void refuseLeMemeChauffeurSurUnePeriodeQuiChevauche() {
        creer(camion1, chauffeur1, 8, 12);
        ValidationException e = assertThrows(ValidationException.class, () -> creer(camion2, chauffeur1, 9, 10));
        assertTrue(e.getMessage().startsWith("Chevauchement : le chauffeur Test Alpha est déjà affecté à TRJ-9081"),
                e.getMessage());
    }

    @Test
    void uneMissionQuiEnglobeLAutreEstAussiRefusee() {
        creer(camion1, chauffeur1, 10, 11);
        assertThrows(ValidationException.class, () -> creer(camion1, chauffeur2, 8, 14));
    }

    @Test
    void desMissionsQuiSeTouchentSontAcceptees() {
        creer(camion1, chauffeur1, 8, 12);
        Mission suivante = creer(camion1, chauffeur1, 12, 16);
        Mission precedente = service.creer(client, camion1.getId(), chauffeur1.getId(), "Rabat", "Fès", null,
                BigDecimal.ONE, H8.withHour(6), H8);
        assertEquals("TRJ-9082", suivante.getReference());
        assertEquals("TRJ-9083", precedente.getReference());
    }

    @Test
    void uneMissionAnnuleeNeBloquePlusRien() {
        Mission a = creer(camion1, chauffeur1, 8, 12);
        service.annuler(a.getId());
        Mission b = creer(camion1, chauffeur1, 9, 11);
        assertEquals(StatutMission.PLANIFIEE, b.getStatut());
    }

    @Test
    void uneMissionLivreeBloqueToujoursSaPeriode() {
        Mission a = creer(camion1, chauffeur1, 8, 12);
        service.demarrer(a.getId());
        service.livrer(a.getId());
        assertThrows(ValidationException.class, () -> creer(camion1, chauffeur2, 10, 13));
    }

    @Test
    void modifierNeSeCompteNePasCommeUnConflitAvecElleMeme() {
        Mission a = creer(camion1, chauffeur1, 8, 12);
        service.modifier(a.getId(), client, camion1.getId(), chauffeur1.getId(), "Casablanca", "Kénitra", null,
                new BigDecimal("12"), H8.withHour(9), H8.withHour(13));
        Mission m = service.trouver(a.getId());
        assertEquals("Kénitra", m.getVilleArrivee());
        assertEquals("TRJ-9081", m.getReference());
    }

    @Test
    void modifierRefuseUnChevauchementAvecUneAutreMission() {
        creer(camion1, chauffeur1, 8, 12);
        Mission b = creer(camion2, chauffeur2, 8, 12);
        assertThrows(ValidationException.class, () -> service.modifier(b.getId(), client, camion1.getId(),
                chauffeur2.getId(), "A", "B", null, BigDecimal.ONE, H8.withHour(10), H8.withHour(11)));
    }

    @Test
    void seuleUneMissionPlanifieePeutEtreModifiee() {
        Mission a = creer(camion1, chauffeur1, 8, 12);
        service.demarrer(a.getId());
        assertThrows(ValidationException.class, () -> service.modifier(a.getId(), client, camion1.getId(),
                chauffeur1.getId(), "A", "B", null, BigDecimal.ONE, H8, H8.withHour(12)));
    }

    @Test
    void laDateDeFinDoitEtreApresLaDateDeDebut() {
        ValidationException e = assertThrows(ValidationException.class, () -> creer(camion1, chauffeur1, 12, 12));
        assertEquals("La date de fin doit être après la date de début.", e.getMessage());
        assertThrows(ValidationException.class, () -> creer(camion1, chauffeur1, 12, 9));
    }

    @Test
    void unVehiculeEnMaintenanceOuHorsServiceNePeutPasEtreAffecte() {
        vehicules.changerStatut(camion1.getId(), StatutVehicule.MAINTENANCE);
        ValidationException e = assertThrows(ValidationException.class, () -> creer(camion1, chauffeur1, 8, 12));
        assertEquals("Le véhicule 11111-A-1 est « en maintenance » : il ne peut pas être affecté.", e.getMessage());
        vehicules.changerStatut(camion1.getId(), StatutVehicule.HORS_SERVICE);
        assertThrows(ValidationException.class, () -> creer(camion1, chauffeur1, 8, 12));
    }

    @Test
    void leChauffeurDoitEtreUnChauffeurActif() {
        Employe agent = employes.creer("Gamma", "Test", "ZZ-900003", null, Poste.AGENT_DEPOT, TypeContrat.CDI,
                LocalDate.of(2024, 1, 1));
        assertThrows(ValidationException.class, () -> creer(camion1, agent, 8, 12));
        employes.desactiver(chauffeur1.getId());
        ValidationException e = assertThrows(ValidationException.class, () -> creer(camion1, chauffeur1, 8, 12));
        assertEquals("Seul un chauffeur actif peut être affecté à une mission.", e.getMessage());
    }

    @Test
    void lesChampsObligatoires() {
        assertThrows(ValidationException.class, () -> service.creer(null, camion1.getId(), chauffeur1.getId(),
                "A", "B", null, BigDecimal.ONE, H8, H8.plusHours(1)));
        assertThrows(ValidationException.class, () -> service.creer(client, null, chauffeur1.getId(),
                "A", "B", null, BigDecimal.ONE, H8, H8.plusHours(1)));
        assertThrows(ValidationException.class, () -> service.creer(client, camion1.getId(), null,
                "A", "B", null, BigDecimal.ONE, H8, H8.plusHours(1)));
        assertThrows(ValidationException.class, () -> service.creer(client, camion1.getId(), chauffeur1.getId(),
                " ", "B", null, BigDecimal.ONE, H8, H8.plusHours(1)));
        assertThrows(ValidationException.class, () -> service.creer(client, camion1.getId(), chauffeur1.getId(),
                "A", "B", null, BigDecimal.ZERO, H8, H8.plusHours(1)));
        assertThrows(ValidationException.class, () -> service.creer(client, camion1.getId(), chauffeur1.getId(),
                "A", "B", null, BigDecimal.ONE, null, H8.plusHours(1)));
        assertThrows(ValidationException.class, () -> service.creer(999L, camion1.getId(), chauffeur1.getId(),
                "A", "B", null, BigDecimal.ONE, H8, H8.plusHours(1)));
    }

    @Test
    void cycleDeVieAutorise() {
        Mission m = creer(camion1, chauffeur1, 8, 12);
        service.demarrer(m.getId());
        assertEquals(StatutMission.EN_COURS, service.trouver(m.getId()).getStatut());
        service.livrer(m.getId());
        assertEquals(StatutMission.LIVREE, service.trouver(m.getId()).getStatut());
        service.marquerFacturee(m.getId());
        assertEquals(StatutMission.FACTUREE, service.trouver(m.getId()).getStatut());
    }

    @Test
    void transitionsInterdites() {
        Mission m = creer(camion1, chauffeur1, 8, 12);
        assertThrows(ValidationException.class, () -> service.livrer(m.getId()));          // planned -> delivered
        assertThrows(ValidationException.class, () -> service.marquerFacturee(m.getId())); // planned -> invoiced
        service.demarrer(m.getId());
        assertThrows(ValidationException.class, () -> service.demarrer(m.getId()));
        service.livrer(m.getId());
        assertThrows(ValidationException.class, () -> service.annuler(m.getId()));         // delivered -> cancelled

        Mission annulee = creer(camion2, chauffeur2, 8, 12);
        service.annuler(annulee.getId());
        assertThrows(ValidationException.class, () -> service.demarrer(annulee.getId()));
    }

    @Test
    void uneMissionEnCoursPeutEtreAnnulee() {
        Mission m = creer(camion1, chauffeur1, 8, 12);
        service.demarrer(m.getId());
        service.annuler(m.getId());
        assertEquals(StatutMission.ANNULEE, service.trouver(m.getId()).getStatut());
    }

    @Test
    void missionActuelleOuProchaineDuChauffeur() {
        Mission plusTard = creer(camion1, chauffeur1, 14, 16);
        Mission prochaine = creer(camion1, chauffeur1, 11, 13);
        assertEquals(prochaine.getId(), service.missionActuelleOuProchaine(chauffeur1.getId()).orElseThrow().getId());

        service.demarrer(plusTard.getId());
        assertEquals(plusTard.getId(), service.missionActuelleOuProchaine(chauffeur1.getId()).orElseThrow().getId());
        assertEquals(plusTard.getId(), service.missionEnCoursDuVehicule(camion1.getId()).orElseThrow().getId());
        assertTrue(service.missionActuelleOuProchaine(chauffeur2.getId()).isEmpty());
    }

    @Test
    void assignerUneMissionValideChangeLeChauffeurEtLeCamion() {
        Mission a = creer(camion1, chauffeur1, 14, 16);
        creer(camion2, chauffeur2, 8, 12); // occupe les deux ressources plus tôt, sans chevauchement

        Mission assignee = service.assigner(a.getId(), camion2.getId(), chauffeur2.getId());

        assertEquals(camion2.getId(), assignee.getVehiculeId());
        assertEquals(chauffeur2.getId(), assignee.getChauffeurId());
        assertEquals("TRJ-9081", assignee.getReference());
        assertEquals(StatutMission.PLANIFIEE, assignee.getStatut());
        assertEquals(camion2.getId(), service.trouver(a.getId()).getVehiculeId()); // bien persisté dans le DAO
        assertEquals(chauffeur2.getId(), service.trouver(a.getId()).getChauffeurId());
    }

    @Test
    void assignerRefuseUnChauffeurDejaAffecteSurLaPeriode() {
        creer(camion1, chauffeur1, 8, 12);
        Mission cible = creer(camion2, chauffeur2, 10, 12); // même période, autres ressources

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.assigner(cible.getId(), camion2.getId(), chauffeur1.getId()));

        assertTrue(e.getMessage().startsWith("Chevauchement : le chauffeur Test Alpha est déjà affecté à TRJ-9081"),
                e.getMessage());
        assertEquals(chauffeur2.getId(), service.trouver(cible.getId()).getChauffeurId()); // rien n'a changé
    }

    @Test
    void assignerRefuseUnCamionDejaAffecteSurLaPeriode() {
        creer(camion1, chauffeur1, 8, 12);
        Mission cible = creer(camion2, chauffeur2, 10, 12);

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.assigner(cible.getId(), camion1.getId(), chauffeur2.getId()));

        assertEquals("Chevauchement : le véhicule 11111-A-1 est déjà affecté à TRJ-9081 "
                + "(Casablanca → Rabat, du 17/11/2026 08:00 au 17/11/2026 12:00).", e.getMessage());
        assertEquals(camion2.getId(), service.trouver(cible.getId()).getVehiculeId());
    }

    @Test
    void assignerAccepteUneMissionQuiSeToucheEtPeutGarderSesRessources() {
        creer(camion1, chauffeur1, 8, 12);
        Mission suivante = creer(camion2, chauffeur2, 12, 16);
        service.assigner(suivante.getId(), camion1.getId(), chauffeur1.getId()); // fin = début : pas de conflit
        Mission identique = service.assigner(suivante.getId(), camion1.getId(), chauffeur1.getId());

        assertEquals(chauffeur1.getId(), identique.getChauffeurId());
        assertEquals(camion1.getId(), identique.getVehiculeId());
    }

    @Test
    void assignerRefuseUneMissionQuiNestPlusPlanifiee() {
        Mission a = creer(camion1, chauffeur1, 8, 12);
        service.demarrer(a.getId());
        assertThrows(ValidationException.class,
                () -> service.assigner(a.getId(), camion2.getId(), chauffeur2.getId()));
    }
}
