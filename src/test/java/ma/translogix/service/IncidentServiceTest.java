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
import java.util.List;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryIncidentDao;
import ma.translogix.dao.memory.InMemoryMissionDao;
import ma.translogix.dao.memory.InMemoryVehiculeDao;
import ma.translogix.model.Client;
import ma.translogix.model.Employe;
import ma.translogix.model.GraviteIncident;
import ma.translogix.model.Incident;
import ma.translogix.model.Mission;
import ma.translogix.model.Poste;
import ma.translogix.model.StatutIncident;
import ma.translogix.model.TypeContrat;
import ma.translogix.model.TypeIncident;
import ma.translogix.model.Vehicule;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Rules of the Dispatcher road incidents. Fictional identifiers only, in-memory DAO. */
class IncidentServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime H8 = LocalDateTime.of(2026, 11, 17, 8, 0);

    private MissionService missions;
    private IncidentService service;
    private Mission planifiee;

    @BeforeEach
    void setUp() {
        InMemoryVehiculeDao vehiculeDao = new InMemoryVehiculeDao();
        VehiculeService vehicules = new VehiculeService(vehiculeDao);
        EmployeService employes = new EmployeService(new InMemoryEmployeDao(), HORLOGE);
        ClientService clients = new ClientService(new InMemoryClientDao(), new InMemoryFactureDao());
        missions = new MissionService(new InMemoryMissionDao(), vehiculeDao, employes, clients, HORLOGE);
        service = new IncidentService(new InMemoryIncidentDao(), missions, HORLOGE);

        Long client = clients.creer("Client Test", null, null, null, null, Client.DELAI_PAR_DEFAUT).getId();
        Vehicule camion = vehicules.creer("44444-D-4", "Volvo", "FH", new BigDecimal("26"));
        Employe chauffeur = employes.creer("Echo", "Test", "ZZ-900005", null, Poste.CHAUFFEUR, TypeContrat.CDI,
                LocalDate.of(2024, 1, 1));
        planifiee = missions.creer(client, camion.getId(), chauffeur.getId(), "Casablanca", "Rabat", "Palettes",
                BigDecimal.ONE, H8, H8.plusHours(4));
    }

    private Incident declarer() {
        return service.declarer(planifiee.getId(), TypeIncident.PANNE, GraviteIncident.URGENTE, "Panne moteur",
                "Autoroute A1", H8.plusHours(1));
    }

    @Test
    void declareUnIncidentOuvertSurUneMissionPlanifiee() {
        Incident incident = declarer();

        assertEquals("INC-5001", incident.getReference());
        assertEquals(planifiee.getId(), incident.getMissionId());
        assertEquals(TypeIncident.PANNE, incident.getType());
        assertEquals(GraviteIncident.URGENTE, incident.getGravite());
        assertEquals(StatutIncident.OUVERT, incident.getStatut());
        assertEquals(1, service.lister().size());
    }

    @Test
    void accepteAussiUneMissionEnCours() {
        missions.demarrer(planifiee.getId());
        assertEquals("INC-5001", declarer().getReference());
    }

    @Test
    void refuseUneMissionLivreeOuFacturee() {
        missions.demarrer(planifiee.getId());
        missions.livrer(planifiee.getId());

        ValidationException e = assertThrows(ValidationException.class, this::declarer);
        assertTrue(e.getMessage().contains("planifiée ou en cours"), e.getMessage());
        assertTrue(service.lister().isEmpty());
    }

    @Test
    void seulesLesMissionsPlanifieesOuEnCoursSontEligibles() {
        assertEquals(List.of(planifiee.getId()), service.missionsEligibles().stream().map(Mission::getId).toList());

        missions.demarrer(planifiee.getId());
        assertEquals(List.of(planifiee.getId()), service.missionsEligibles().stream().map(Mission::getId).toList());

        missions.livrer(planifiee.getId());
        assertTrue(service.missionsEligibles().isEmpty());
    }

    @Test
    void refuseLesChampsObligatoiresEtUnSignalementFutur() {
        ValidationException futur = assertThrows(ValidationException.class, () -> service.declarer(planifiee.getId(),
                TypeIncident.PANNE, GraviteIncident.URGENTE, "Panne moteur", "Autoroute A1", H8.plusHours(3)));
        assertEquals("Le signalement ne peut pas être dans le futur.", futur.getMessage());

        assertThrows(ValidationException.class, () -> service.declarer(planifiee.getId(), null,
                GraviteIncident.URGENTE, "Panne moteur", "Autoroute A1", H8.plusHours(1)));
        assertThrows(ValidationException.class, () -> service.declarer(planifiee.getId(), TypeIncident.PANNE, null,
                "Panne moteur", "Autoroute A1", H8.plusHours(1)));
        assertThrows(ValidationException.class, () -> service.declarer(planifiee.getId(), TypeIncident.PANNE,
                GraviteIncident.URGENTE, " ", "Autoroute A1", H8.plusHours(1)));
        assertThrows(ValidationException.class, () -> service.declarer(planifiee.getId(), TypeIncident.PANNE,
                GraviteIncident.URGENTE, "Panne moteur", null, H8.plusHours(1)));
        assertTrue(service.lister().isEmpty());
    }

    @Test
    void lesReferencesSuiventLaDerniereReferenceEnregistree() {
        declarer();
        assertEquals("INC-5002", declarer().getReference());
    }
}
