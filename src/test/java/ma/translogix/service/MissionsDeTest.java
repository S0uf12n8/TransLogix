package ma.translogix.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryMissionDao;
import ma.translogix.dao.memory.InMemoryVehiculeDao;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.Poste;
import ma.translogix.model.TypeContrat;

/**
 * Test fixture: the dispatcher stack in memory, with one truck and one driver, to create missions that never
 * overlap (each one 2 h, one after the other). Used by the invoicing and driver-space tests. Fictional data only.
 */
final class MissionsDeTest {

    final InMemoryEmployeDao employeDao = new InMemoryEmployeDao();
    final EmployeService employes;
    final VehiculeService vehicules;
    final MissionService missions;
    final Employe chauffeur;
    private final Long vehicule;
    private LocalDateTime prochainDepart = LocalDateTime.of(2026, 1, 5, 8, 0);
    private int numeroCin = 900100;

    MissionsDeTest(InMemoryClientDao clientDao, InMemoryFactureDao factureDao, Clock horloge) {
        InMemoryVehiculeDao vehiculeDao = new InMemoryVehiculeDao();
        employes = new EmployeService(employeDao, horloge);
        vehicules = new VehiculeService(vehiculeDao);
        missions = new MissionService(new InMemoryMissionDao(), vehiculeDao, employes,
                new ClientService(clientDao, factureDao), horloge);
        vehicule = vehicules.creer("11111-A-1", "Volvo", "FH", new BigDecimal("26")).getId();
        chauffeur = nouveauChauffeur("Alpha");
    }

    Employe nouveauChauffeur(String nom) {
        return employes.creer(nom, "Test", "ZZ-" + numeroCin++, null, Poste.CHAUFFEUR, TypeContrat.CDI,
                LocalDate.of(2024, 1, 1));
    }

    /** A PLANIFIEE mission for this driver, in the next free 2-hour slot. */
    Mission planifiee(Long clientId, Employe pour) {
        LocalDateTime debut = prochainDepart;
        prochainDepart = prochainDepart.plusHours(3);
        return missions.creer(clientId, vehicule, pour.getId(), "Casablanca", "Rabat", "Palettes",
                new BigDecimal("12"), debut, debut.plusHours(2));
    }

    Mission planifiee(Long clientId) {
        return planifiee(clientId, chauffeur);
    }

    /** A mission already started and delivered: ready to be invoiced. */
    Mission livree(Long clientId) {
        Mission m = planifiee(clientId);
        missions.demarrer(m.getId());
        missions.livrer(m.getId());
        return missions.trouver(m.getId());
    }
}
