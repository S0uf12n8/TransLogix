package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.PasswordHasher;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UtilisateurServiceTest {

    private UtilisateurService service;

    @BeforeEach
    void setUp() {
        service = new UtilisateurService(new InMemoryUtilisateurDao());
    }

    @Test
    void creerUtilisateurValideLeSauvegardeAvecMotDePasseHache() {
        Utilisateur u = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        assertNotNull(u.getId());
        assertNotEquals("motdepasse1", u.getMotDePasseHash());
        assertTrue(PasswordHasher.verifier("motdepasse1", u.getMotDePasseHash()));
    }

    @Test
    void emailDejaUtiliseEstRefuseMemeAvecMajuscules() {
        service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        assertThrows(ValidationException.class,
                () -> service.creer("Bennani", "Ali", "SARA@translogix.ma", "motdepasse2", Role.RH));
    }

    @Test
    void emailInvalideEstRefuse() {
        assertThrows(ValidationException.class,
                () -> service.creer("Bennani", "Ali", "pas-un-email", "motdepasse2", Role.RH));
    }

    @Test
    void motDePasseTropCourtEstRefuse() {
        assertThrows(ValidationException.class,
                () -> service.creer("Bennani", "Ali", "ali@translogix.ma", "court", Role.RH));
    }

    @Test
    void dernierGerantNePeutPasEtreSupprime() {
        Utilisateur gerant = service.creer("Principal", "Admin", "admin@translogix.ma", "motdepasse1", Role.GERANT);

        assertThrows(ValidationException.class, () -> service.supprimer(gerant.getId()));
    }

    @Test
    void modifierMetAJourLesChamps() {
        Utilisateur u = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        service.modifier(u.getId(), "Alami", "Sara", "sara@translogix.ma", Role.COMPTABLE, false);

        Utilisateur modifie = service.lister().get(0);
        assertEquals(Role.COMPTABLE, modifie.getRole());
        assertEquals(false, modifie.isActif());
    }
}
