package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import ma.translogix.dao.memory.InMemoryVehiculeDao;
import ma.translogix.model.StatutVehicule;
import ma.translogix.model.Vehicule;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VehiculeServiceTest {

    private VehiculeService service;

    @BeforeEach
    void setUp() {
        service = new VehiculeService(new InMemoryVehiculeDao());
    }

    @Test
    void creeUnVehiculeEnServiceAvecUnMatriculeEnMajuscules() {
        Vehicule v = service.creer("  12345-a-6 ", "Volvo", "FH", new BigDecimal("26"));
        assertEquals("12345-A-6", v.getMatricule());
        assertEquals(StatutVehicule.EN_SERVICE, v.getStatut());
        assertEquals("Volvo FH", v.getDesignation());
    }

    @Test
    void leMatriculeEstObligatoire() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.creer(" ", "Volvo", "FH", BigDecimal.TEN));
        assertEquals("Le matricule est obligatoire.", e.getMessage());
    }

    @Test
    void leMatriculeEstUniqueSansTenirCompteDeLaCasse() {
        service.creer("12345-A-6", "Volvo", "FH", BigDecimal.TEN);
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.creer("12345-a-6", "DAF", "XF", BigDecimal.TEN));
        assertEquals("Le matricule 12345-A-6 est déjà utilisé par un autre véhicule.", e.getMessage());
    }

    @Test
    void laCapaciteDoitEtrePositive() {
        assertThrows(ValidationException.class, () -> service.creer("1-A-1", "Volvo", "FH", null));
        assertThrows(ValidationException.class, () -> service.creer("1-A-1", "Volvo", "FH", BigDecimal.ZERO));
        assertThrows(ValidationException.class, () -> service.creer("1-A-1", "Volvo", "FH", new BigDecimal("-3")));
    }

    @Test
    void modifierGardeSonPropreMatriculeMaisRefuseCeluiDUnAutre() {
        Vehicule volvo = service.creer("12345-A-6", "Volvo", "FH", BigDecimal.TEN);
        service.creer("67890-B-2", "Scania", "R500", BigDecimal.TEN);

        service.modifier(volvo.getId(), "12345-A-6", "Volvo", "FH16", new BigDecimal("28"));
        assertEquals("FH16", service.trouver(volvo.getId()).getModele());
        assertThrows(ValidationException.class,
                () -> service.modifier(volvo.getId(), "67890-B-2", "Volvo", "FH", BigDecimal.TEN));
    }

    @Test
    void marqueEtModeleVidesSontEnregistresCommeNull() {
        Vehicule v = service.creer("1-A-1", " ", "", BigDecimal.TEN);
        assertNull(v.getMarque());
        assertNull(v.getModele());
    }

    @Test
    void changerStatutEtCompter() {
        Vehicule v = service.creer("1-A-1", "Volvo", "FH", BigDecimal.TEN);
        service.creer("2-A-2", "Volvo", "FH", BigDecimal.TEN);
        service.changerStatut(v.getId(), StatutVehicule.MAINTENANCE);

        assertEquals(StatutVehicule.MAINTENANCE, service.trouver(v.getId()).getStatut());
        assertEquals(1, service.compter(StatutVehicule.MAINTENANCE));
        assertEquals(1, service.compter(StatutVehicule.EN_SERVICE));
        assertThrows(ValidationException.class, () -> service.changerStatut(v.getId(), null));
    }

    @Test
    void listerTrieParMatricule() {
        service.creer("67890-B-2", "Scania", "R500", BigDecimal.TEN);
        service.creer("12345-A-6", "Volvo", "FH", BigDecimal.TEN);
        assertEquals(List.of("12345-A-6", "67890-B-2"), service.lister().stream().map(Vehicule::getMatricule).toList());
    }
}
