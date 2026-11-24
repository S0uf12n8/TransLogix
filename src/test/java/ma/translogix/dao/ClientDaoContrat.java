package ma.translogix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import ma.translogix.model.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What every ClientDao must do, whatever the storage. The memory and MySQL implementations pass the
 * same tests; only the way an empty DAO is built changes.
 */
public abstract class ClientDaoContrat {

    protected ClientDao dao;

    /** A DAO with no client in it. */
    protected abstract ClientDao daoVide();

    @BeforeEach
    void preparer() {
        dao = daoVide();
    }

    protected static Client client(String nom, String ice) {
        return new Client(null, nom, ice, "05-00-00-00-01", "client@exemple.test",
                "1 rue fictive, Casablanca", 30);
    }

    @Test
    void saveDonneUnIdEtRelitTousLesChamps() {
        Client cree = dao.save(new Client(null, "El Idrissi", "ICE-0001", "06-11-22-33-44",
                "amine@exemple.test", "12 rue fictive, Rabat", 45));

        assertNotNull(cree.getId());
        Client relu = dao.findById(cree.getId()).orElseThrow();
        assertEquals("El Idrissi", relu.getNom());
        assertEquals("ICE-0001", relu.getIce());
        assertEquals("06-11-22-33-44", relu.getTelephone());
        assertEquals("amine@exemple.test", relu.getEmail());
        assertEquals("12 rue fictive, Rabat", relu.getAdresse());
        assertEquals(45, relu.getDelaiPaiementJours());
    }

    @Test
    void idsDistinctsEtFindAllRenvoieTout() {
        dao.save(client("Alami", "ICE-0002"));
        dao.save(client("Bennani", "ICE-0003"));

        List<Client> clients = dao.findAll();
        assertNotEquals(clients.get(0).getId(), clients.get(1).getId());
        assertEquals(List.of("Alami", "Bennani"), clients.stream().map(Client::getNom).sorted().toList());
    }

    @Test
    void findByIceTrouveLeClientEtIgnoreUnIceVide() {
        Client cree = dao.save(client("Cherkaoui", "ICE-0004"));

        assertEquals(cree.getId(), dao.findByIce("ICE-0004").orElseThrow().getId());
        assertTrue(dao.findByIce("ICE-9999").isEmpty());
        assertTrue(dao.findByIce(null).isEmpty());
        assertTrue(dao.findByIce("   ").isEmpty());
    }

    @Test
    void accentsConserves() {
        Client cree = dao.save(new Client(null, "Élhadi", "ICE-0005", "05-00-00-00-02",
                "nour@exemple.test", "Noûr, 8 avenue fictive", 30));

        Client relu = dao.findById(cree.getId()).orElseThrow();
        assertEquals("Élhadi", relu.getNom());
        assertEquals("Noûr, 8 avenue fictive", relu.getAdresse());
    }

    @Test
    void idInconnuDonneVide() {
        assertTrue(dao.findById(999_999L).isEmpty());
    }

    @Test
    void updateRemplaceLesChamps() {
        Client cree = dao.save(client("Dahmani", "ICE-0006"));
        cree.setNom("El Fassi");
        cree.setIce("ICE-0007");
        cree.setTelephone("06-55-44-33-22");
        cree.setEmail("nouveau@exemple.test");
        cree.setAdresse("9 rue fictive, Marrakech");
        cree.setDelaiPaiementJours(60);

        dao.update(cree);

        Client relu = dao.findById(cree.getId()).orElseThrow();
        assertEquals("El Fassi", relu.getNom());
        assertEquals("ICE-0007", relu.getIce());
        assertEquals("06-55-44-33-22", relu.getTelephone());
        assertEquals("nouveau@exemple.test", relu.getEmail());
        assertEquals("9 rue fictive, Marrakech", relu.getAdresse());
        assertEquals(60, relu.getDelaiPaiementJours());
    }

    @Test
    void updateDUnIdInconnuEstRefuse() {
        Client fantome = client("Fantome", "ICE-9998");
        fantome.setId(999_999L);

        assertThrows(IllegalArgumentException.class, () -> dao.update(fantome));
    }

    @Test
    void deleteSupprimeEtIdInconnuNeFaitRien() {
        Client cree = dao.save(client("Ghazi", "ICE-0008"));

        dao.delete(cree.getId());
        dao.delete(999_999L);

        assertTrue(dao.findById(cree.getId()).isEmpty());
        assertTrue(dao.findAll().isEmpty());
    }
}
