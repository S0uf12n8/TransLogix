package ma.translogix.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SessionTest {

    @AfterEach
    void tearDown() {
        Session.fermer();
    }

    private static void connecter(Role role) {
        Session.ouvrir(new Utilisateur(1L, "Test", "Test", "test@translogix.ma", null, role, true));
    }

    @Test
    void sansSessionCeNEstPasLeGerant() {
        assertFalse(Session.estGerant());
    }

    @Test
    void leGerantConnecteEstReconnu() {
        connecter(Role.GERANT);
        assertTrue(Session.estGerant());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "GERANT", mode = EnumSource.Mode.EXCLUDE)
    void lesAutresRolesNeSontPasGerant(Role role) {
        connecter(role);
        assertFalse(Session.estGerant());
    }

    @Test
    void apresDeconnexionCeNEstPlusLeGerant() {
        connecter(Role.GERANT);
        Session.fermer();
        assertFalse(Session.estGerant());
    }
}
