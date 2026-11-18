package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryPaiementDao;
import ma.translogix.model.Client;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClientServiceTest {

    private static final String ICE = "001234567000089";

    private ClientService service;
    private FactureService factures;

    @BeforeEach
    void setUp() {
        InMemoryClientDao clientDao = new InMemoryClientDao();
        InMemoryFactureDao factureDao = new InMemoryFactureDao();
        service = new ClientService(clientDao, factureDao);
        factures = new FactureService(factureDao, clientDao, new InMemoryPaiementDao(), Clock.systemDefaultZone());
    }

    private Client creer(String nom, String ice, String email) {
        return service.creer(nom, ice, "0522000000", email, "Casablanca", 30);
    }

    @Test
    void creationNormaliseLesChamps() {
        Client c = service.creer("  LogiMaroc SARL ", " " + ICE + " ", " ", " Compta@LogiMaroc.ma ", "", 60);

        assertEquals("LogiMaroc SARL", c.getNom());
        assertEquals(ICE, c.getIce());
        assertNull(c.getTelephone());
        assertEquals("compta@logimaroc.ma", c.getEmail());
        assertNull(c.getAdresse());
        assertEquals(60, c.getDelaiPaiementJours());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void nomObligatoire(String nom) {
        assertThrows(ValidationException.class, () -> creer(nom, null, null));
    }

    @Test
    void iceEnDoubleEstRefuse() {
        creer("LogiMaroc SARL", ICE, null);

        ValidationException e = assertThrows(ValidationException.class,
                () -> creer("Atlas Prime Transports", ICE, null));
        assertTrue(e.getMessage().contains("LogiMaroc SARL"), e.getMessage());
        assertEquals(1, service.lister().size());
    }

    @Test
    void iceVidePeutSeRepeter() {
        creer("Client A", null, null);
        creer("Client B", "  ", null);

        assertEquals(2, service.lister().size());
        assertNull(service.lister().get(1).getIce());
    }

    @Test
    void modifierSansChangerSonIceEstAccepte() {
        Client c = creer("LogiMaroc SARL", ICE, null);

        service.modifier(c.getId(), "LogiMaroc", ICE, null, null, null, 45);

        assertEquals("LogiMaroc", service.trouver(c.getId()).getNom());
        assertEquals(45, service.trouver(c.getId()).getDelaiPaiementJours());
    }

    @Test
    void modifierVersLIceDUnAutreClientEstRefuse() {
        creer("LogiMaroc SARL", ICE, null);
        Client autre = creer("Atlas Prime Transports", "009999999000011", null);

        assertThrows(ValidationException.class,
                () -> service.modifier(autre.getId(), "Atlas", ICE, null, null, null, 30));
    }

    @ParameterizedTest
    @ValueSource(strings = {"compta", "compta@", "compta@logimaroc", "@logimaroc.ma", "compta logimaroc@x.ma"})
    void emailInvalideEstRefuse(String email) {
        assertThrows(ValidationException.class, () -> creer("LogiMaroc SARL", null, email));
    }

    @Test
    void emailFacultatif() {
        assertNull(creer("LogiMaroc SARL", null, "").getEmail());
    }

    @Test
    void delaiNegatifEstRefuse() {
        assertThrows(ValidationException.class,
                () -> service.creer("LogiMaroc SARL", null, null, null, null, -1));
    }

    @Test
    void clientSansFactureEstSupprime() {
        Client c = creer("LogiMaroc SARL", null, null);

        service.supprimer(c.getId());

        assertTrue(service.lister().isEmpty());
    }

    @Test
    void clientAvecFacturesNePeutPasEtreSupprime() {
        Client c = creer("LogiMaroc SARL", null, null);
        factures.creer(c.getId(), new BigDecimal("1000"), null, null);

        assertThrows(ValidationException.class, () -> service.supprimer(c.getId()));
        assertEquals(1, service.lister().size());
    }

    @Test
    void listeTrieeParNom() {
        creer("Souss Express Logistique", null, null);
        creer("atlas Prime Transports", null, null);
        creer("LogiMaroc SARL", null, null);

        assertEquals(List.of("atlas Prime Transports", "LogiMaroc SARL", "Souss Express Logistique"),
                service.lister().stream().map(Client::getNom).toList());
    }
}
