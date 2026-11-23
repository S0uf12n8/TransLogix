package ma.translogix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What every UtilisateurDao must do, whatever the storage. Each implementation has a test class that
 * extends this one and only says how to build an empty DAO: memory and MySQL pass the same tests.
 */
public abstract class UtilisateurDaoContrat {

    protected UtilisateurDao dao;

    /** A DAO with no account in it. */
    protected abstract UtilisateurDao daoVide();

    @BeforeEach
    void preparer() {
        dao = daoVide();
    }

    protected static Utilisateur compte(String email, Role role) {
        return new Utilisateur(null, "Alami", "Sara", email, "hash-de-test", role, true);
    }

    @Test
    void saveDonneUnIdEtRelitTousLesChamps() {
        Utilisateur cree = dao.save(new Utilisateur(null, "El Idrissi", "Amine", "amine@translogix.ma",
                "hash-de-test", Role.DISPATCHER, false));

        assertNotNull(cree.getId());
        Utilisateur relu = dao.findById(cree.getId()).orElseThrow();
        assertEquals("El Idrissi", relu.getNom());
        assertEquals("Amine", relu.getPrenom());
        assertEquals("amine@translogix.ma", relu.getEmail());
        assertEquals("hash-de-test", relu.getMotDePasseHash());
        assertEquals(Role.DISPATCHER, relu.getRole());
        assertFalse(relu.isActif());
    }

    @Test
    void idsDistinctsEtFindAllRenvoieTout() {
        Utilisateur a = dao.save(compte("a@translogix.ma", Role.RH));
        Utilisateur b = dao.save(compte("b@translogix.ma", Role.COMPTABLE));

        assertNotEquals(a.getId(), b.getId());
        assertEquals(List.of("a@translogix.ma", "b@translogix.ma"),
                dao.findAll().stream().map(Utilisateur::getEmail).sorted().toList());
    }

    @Test
    void accentsConserves() {
        Utilisateur cree = dao.save(new Utilisateur(null, "Élhadi", "Noûr", "nour@translogix.ma", "h",
                Role.RH, true));

        assertEquals("Élhadi", dao.findById(cree.getId()).orElseThrow().getNom());
        assertEquals("Noûr", dao.findById(cree.getId()).orElseThrow().getPrenom());
    }

    @Test
    void findByEmailIgnoreLaCasse() {
        dao.save(compte("sara@translogix.ma", Role.DISPATCHER));

        assertTrue(dao.findByEmail("SARA@Translogix.MA").isPresent());
        assertTrue(dao.findByEmail("autre@translogix.ma").isEmpty());
    }

    @Test
    void idInconnuDonneVide() {
        assertTrue(dao.findById(999_999L).isEmpty());
    }

    @Test
    void updateRemplaceLesChamps() {
        Utilisateur u = dao.save(compte("sara@translogix.ma", Role.DISPATCHER));
        u.setNom("Bennani");
        u.setEmail("sara.bennani@translogix.ma");
        u.setRole(Role.COMPTABLE);
        u.setActif(false);
        u.setMotDePasseHash("nouveau-hash");

        dao.update(u);

        Utilisateur relu = dao.findById(u.getId()).orElseThrow();
        assertEquals("Bennani", relu.getNom());
        assertEquals("sara.bennani@translogix.ma", relu.getEmail());
        assertEquals(Role.COMPTABLE, relu.getRole());
        assertFalse(relu.isActif());
        assertEquals("nouveau-hash", relu.getMotDePasseHash());
    }

    @Test
    void updateDUnIdInconnuEstRefuse() {
        Utilisateur fantome = compte("fantome@translogix.ma", Role.RH);
        fantome.setId(999_999L);

        assertThrows(IllegalArgumentException.class, () -> dao.update(fantome));
    }

    @Test
    void deleteSupprimeEtIdInconnuNeFaitRien() {
        Utilisateur u = dao.save(compte("sara@translogix.ma", Role.DISPATCHER));

        dao.delete(u.getId());
        dao.delete(999_999L);

        assertTrue(dao.findById(u.getId()).isEmpty());
        assertTrue(dao.findAll().isEmpty());
    }
}
