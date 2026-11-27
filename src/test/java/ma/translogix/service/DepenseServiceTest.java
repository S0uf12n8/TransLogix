package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryDepenseDao;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryMissionDao;
import ma.translogix.dao.memory.InMemoryVehiculeDao;
import ma.translogix.model.Client;
import ma.translogix.model.Depense;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.Poste;
import ma.translogix.model.StatutDepense;
import ma.translogix.model.TypeContrat;
import ma.translogix.model.TypeDepense;
import ma.translogix.model.Vehicule;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Rules of expense validation ([C4]). Fictional identifiers only, fixed clock for every date rule. */
class DepenseServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 20);

    private DepenseService service;
    private AuditService audit;
    private EmployeService employes;
    private Mission mission;
    private Employe chauffeur;

    @BeforeEach
    void setUp() {
        InMemoryVehiculeDao vehiculeDao = new InMemoryVehiculeDao();
        VehiculeService vehicules = new VehiculeService(vehiculeDao);
        employes = new EmployeService(new InMemoryEmployeDao(), HORLOGE);
        ClientService clients = new ClientService(new InMemoryClientDao(), new InMemoryFactureDao());
        MissionService missions = new MissionService(new InMemoryMissionDao(), vehiculeDao, employes, clients, HORLOGE);
        audit = new AuditService(new InMemoryAuditDao(), HORLOGE, () -> "comptable@translogix.ma");
        service = new DepenseService(new InMemoryDepenseDao(), audit, missions, employes, HORLOGE,
                () -> "Comptable Test");

        Long client = clients.creer("Client ZZ", null, null, null, null, Client.DELAI_PAR_DEFAUT).getId();
        Vehicule camion = vehicules.creer("ZZ-123-AA", "Volvo", "FH", new BigDecimal("26"));
        chauffeur = employes.creer("Test", "Alpha", "ZZ-900001", null, Poste.CHAUFFEUR, TypeContrat.CDI,
                LocalDate.of(2024, 1, 1));
        mission = missions.creer(client, camion.getId(), chauffeur.getId(), "Casablanca", "Rabat", "Palettes",
                new BigDecimal("12"), AUJOURDHUI.minusDays(2).atTime(8, 0),
                AUJOURDHUI.minusDays(2).atTime(16, 0));
    }

    private Depense carburant() {
        return service.creer(mission.getId(), chauffeur.getId(), TypeDepense.CARBURANT, "Station ZZ Casablanca",
                new BigDecimal("850.50"), new BigDecimal("62.40"), AUJOURDHUI.minusDays(2));
    }

    @Test
    void creeUneDepenseEnAttenteRattacheeAUneMissionReelle() {
        Depense depense = carburant();
        assertNotNull(depense.getId());
        assertEquals(StatutDepense.EN_ATTENTE, depense.getStatut());
        assertEquals(mission.getId(), depense.getMissionId());
        assertEquals(chauffeur.getId(), depense.getChauffeurId());
        assertEquals(new BigDecimal("850.50"), depense.getMontant());
        assertEquals(1, service.lister().size());
    }

    @Test
    void refuseUneDepensePourUnChauffeurEtrangerALaMission() {
        Employe autre = employes.creer("ZZ-Test", "Beta", "ZZ-900002", null, Poste.CHAUFFEUR, TypeContrat.CDI,
                LocalDate.of(2024, 1, 1));
        ValidationException e = assertThrows(ValidationException.class, () -> service.creer(mission.getId(),
                autre.getId(), TypeDepense.CARBURANT, "Station ZZ", new BigDecimal("100.00"), null, AUJOURDHUI));
        assertTrue(e.getMessage().contains("n'est pas affecté à la mission"));
    }

    @Test
    void refuseUnEmployeQuiNestPasUnChauffeurActif() {
        Employe mecanicien = employes.creer("ZZ-Test", "Gamma", "ZZ-900003", null, Poste.MECANICIEN,
                TypeContrat.CDI, LocalDate.of(2024, 1, 1));
        assertThrows(ValidationException.class, () -> service.creer(mission.getId(), mecanicien.getId(),
                TypeDepense.REPAS, "Cantine ZZ", new BigDecimal("100.00"), null, AUJOURDHUI));
    }

    @Test
    void refuseUnMontantNulOuTropPrecis() {
        assertThrows(ValidationException.class, () -> service.creer(mission.getId(), chauffeur.getId(),
                TypeDepense.REPAS, "Restaurant ZZ", BigDecimal.ZERO, null, AUJOURDHUI));
        assertThrows(ValidationException.class, () -> service.creer(mission.getId(), chauffeur.getId(),
                TypeDepense.REPAS, "Restaurant ZZ", new BigDecimal("10.123"), null, AUJOURDHUI));
    }

    @Test
    void refuseUnMontantTropEleve() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.creer(mission.getId(),
                chauffeur.getId(), TypeDepense.AUTRE, "Station ZZ", new BigDecimal("1000001"), null, AUJOURDHUI));
        assertTrue(e.getMessage().contains("limite autorisée"));
    }

    @Test
    void refuseLesLitresSurUneDepenseAutreQueCarburant() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.creer(mission.getId(),
                chauffeur.getId(), TypeDepense.PEAGE, "Péage ZZ", new BigDecimal("35.00"), new BigDecimal("10"),
                AUJOURDHUI));
        assertTrue(e.getMessage().contains("carburant"));
    }

    @Test
    void refuseUneDateFuture() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.creer(mission.getId(),
                chauffeur.getId(), TypeDepense.CARBURANT, "Station ZZ", new BigDecimal("100.00"),
                new BigDecimal("10"), AUJOURDHUI.plusDays(1)));
        assertTrue(e.getMessage().contains("futur"));
    }

    @Test
    void refuseUneDateAvantLeDebutDeLaMission() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.creer(mission.getId(),
                chauffeur.getId(), TypeDepense.CARBURANT, "Station ZZ", new BigDecimal("100.00"),
                new BigDecimal("10"), AUJOURDHUI.minusDays(3)));
        assertTrue(e.getMessage().contains("début de la mission"));
    }

    @Test
    void refuseUnDoublonExact() {
        carburant();
        ValidationException e = assertThrows(ValidationException.class, this::carburant);
        assertTrue(e.getMessage().contains("déjà été saisie"));
        assertEquals(1, service.lister().size());
    }

    @Test
    void valideUneDepenseEtInscritLAudit() {
        Depense depense = carburant();
        service.valider(depense.getId());
        assertEquals(StatutDepense.VALIDEE, service.lister().get(0).getStatut());
        assertEquals(1, audit.rechercher("Dépense validée").size());
    }

    @Test
    void refuseDeValiderDeuxFoisLaMemeDepense() {
        Depense depense = carburant();
        service.valider(depense.getId());
        ValidationException e = assertThrows(ValidationException.class, () -> service.valider(depense.getId()));
        assertTrue(e.getMessage().contains("en attente"));
    }

    @Test
    void refuseUneDepenseAvecMotifObligatoire() {
        Depense depense = carburant();
        assertThrows(ValidationException.class, () -> service.refuser(depense.getId(), "  "));
        service.refuser(depense.getId(), "  Reçu illisible  ");
        Depense refusee = service.lister().get(0);
        assertEquals(StatutDepense.REFUSEE, refusee.getStatut());
        assertEquals("Reçu illisible", refusee.getMotifRefus());
    }

    @Test
    void neRefusePasUneDepenseDejaValidee() {
        Depense depense = carburant();
        service.valider(depense.getId());
        assertThrows(ValidationException.class, () -> service.refuser(depense.getId(), "Trop tard"));
    }

    @Test
    void filtreParStatutTypeEtPeriode() {
        carburant();
        service.creer(mission.getId(), chauffeur.getId(), TypeDepense.REPAS, "Restaurant ZZ",
                new BigDecimal("120.00"), null, AUJOURDHUI);

        assertEquals(2, service.rechercher(DepenseService.CritereDepense.AUCUN).size());
        assertEquals(2, service.rechercher(new DepenseService.CritereDepense(null, StatutDepense.EN_ATTENTE,
                null, null, null, null)).size());
        assertEquals(1, service.rechercher(new DepenseService.CritereDepense(null, null, TypeDepense.CARBURANT,
                null, AUJOURDHUI.minusDays(2), AUJOURDHUI.minusDays(2))).size());
        assertEquals(1, service.rechercher(new DepenseService.CritereDepense(null, null, null, null,
                AUJOURDHUI, AUJOURDHUI)).size());
        assertThrows(ValidationException.class, () -> service.rechercher(
                new DepenseService.CritereDepense(null, null, null, null, AUJOURDHUI, AUJOURDHUI.minusDays(1))));
    }

    @Test
    void laRechercheIgnoreLesAccentsEtTrouveLaMission() {
        carburant();
        assertEquals(1, service.rechercher(new DepenseService.CritereDepense("station zz", null, null,
                null, null, null)).size());
        assertEquals(1, service.rechercher(new DepenseService.CritereDepense(mission.getReference(), null, null,
                null, null, null)).size());
    }

    @Test
    void laSyntheseAdditionneSeulementLesMontantsEnregistres() {
        carburant();
        Depense repas = service.creer(mission.getId(), chauffeur.getId(), TypeDepense.REPAS, "Restaurant ZZ",
                new BigDecimal("120.00"), null, AUJOURDHUI);
        service.valider(repas.getId());

        DepenseService.Synthese synthese = service.synthese();
        assertEquals(1, synthese.enAttente());
        assertEquals(new BigDecimal("850.50"), synthese.montantEnAttente());
        assertEquals(new BigDecimal("120.00"), synthese.montantValide());
        assertEquals(0, synthese.refusees());
    }
}
