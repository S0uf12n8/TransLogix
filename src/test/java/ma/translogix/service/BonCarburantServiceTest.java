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
import ma.translogix.dao.memory.InMemoryBonCarburantDao;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryMissionDao;
import ma.translogix.dao.memory.InMemoryVehiculeDao;
import ma.translogix.model.BonCarburant;
import ma.translogix.model.Client;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.Poste;
import ma.translogix.model.StatutBonCarburant;
import ma.translogix.model.TypeContrat;
import ma.translogix.model.Vehicule;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Rules of the Dispatcher fuel vouchers. Fictional identifiers only, in-memory DAO. */
class BonCarburantServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime H8 = LocalDateTime.of(2026, 11, 17, 8, 0);

    private MissionService missions;
    private BonCarburantService service;
    private Mission enCours;

    @BeforeEach
    void setUp() {
        InMemoryVehiculeDao vehiculeDao = new InMemoryVehiculeDao();
        VehiculeService vehicules = new VehiculeService(vehiculeDao);
        EmployeService employes = new EmployeService(new InMemoryEmployeDao(), HORLOGE);
        ClientService clients = new ClientService(new InMemoryClientDao(), new InMemoryFactureDao());
        missions = new MissionService(new InMemoryMissionDao(), vehiculeDao, employes, clients, HORLOGE);
        service = new BonCarburantService(new InMemoryBonCarburantDao(), missions, HORLOGE);

        Long client = clients.creer("Client Test", null, null, null, null, Client.DELAI_PAR_DEFAUT).getId();
        Vehicule camion = vehicules.creer("33333-C-3", "Renault", "T", new BigDecimal("26"));
        Employe chauffeur = employes.creer("Delta", "Test", "ZZ-900004", null, Poste.CHAUFFEUR, TypeContrat.CDI,
                LocalDate.of(2024, 1, 1));
        Mission creee = missions.creer(client, camion.getId(), chauffeur.getId(), "Casablanca", "Rabat", "Palettes",
                BigDecimal.ONE, H8, H8.plusHours(4));
        missions.demarrer(creee.getId());
        enCours = creee;
    }

    private BonCarburant emettre() {
        return service.emettre(enCours.getId(), "Station Test", new BigDecimal("120"), new BigDecimal("900"),
                LocalDate.of(2026, 11, 17));
    }

    @Test
    void emetUnBonValidePourUneMissionEnCoursAvecLaReferenceSuivante() {
        BonCarburant bon = emettre();

        assertEquals("BON-8841", bon.getReference());
        assertEquals(enCours.getId(), bon.getMissionId());
        assertEquals(enCours.getVehiculeId(), bon.getVehiculeId());
        assertEquals(enCours.getChauffeurId(), bon.getChauffeurId());
        assertEquals(StatutBonCarburant.VALIDE, bon.getStatut());
        assertEquals(1, service.lister().size());
    }

    @Test
    void refuseUneMissionQuiNestPasEnCours() {
        missions.livrer(enCours.getId());

        ValidationException e = assertThrows(ValidationException.class, this::emettre);
        assertTrue(e.getMessage().contains("mission en cours"), e.getMessage());
        assertTrue(service.lister().isEmpty());
    }

    @Test
    void seulesLesMissionsEnCoursSontEligibles() {
        assertEquals(List.of(enCours.getId()), service.missionsEligibles().stream().map(Mission::getId).toList());

        missions.livrer(enCours.getId());
        assertTrue(service.missionsEligibles().isEmpty());
    }

    @Test
    void refuseUneDateFutureEtDesChampsInvalides() {
        ValidationException futur = assertThrows(ValidationException.class, () -> service.emettre(enCours.getId(),
                "Station Test", BigDecimal.ONE, BigDecimal.ONE, LocalDate.of(2026, 11, 18)));
        assertEquals("La date du bon ne peut pas être dans le futur.", futur.getMessage());

        assertThrows(ValidationException.class, () -> service.emettre(enCours.getId(), " ", BigDecimal.ONE,
                BigDecimal.ONE, LocalDate.of(2026, 11, 17)));
        assertThrows(ValidationException.class, () -> service.emettre(enCours.getId(), "Station Test",
                BigDecimal.ZERO, BigDecimal.ONE, LocalDate.of(2026, 11, 17)));
        assertThrows(ValidationException.class, () -> service.emettre(enCours.getId(), "Station Test",
                BigDecimal.ONE, null, LocalDate.of(2026, 11, 17)));
        assertThrows(ValidationException.class, () -> service.emettre(enCours.getId(), "Station Test",
                BigDecimal.ONE, BigDecimal.ONE, null));
        assertTrue(service.lister().isEmpty());
    }

    @Test
    void lesReferencesSuiventLaDerniereReferenceEnregistree() {
        emettre();
        assertEquals("BON-8842", emettre().getReference());
    }
}
