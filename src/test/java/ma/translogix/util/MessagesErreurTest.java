package ma.translogix.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import ma.translogix.dao.DonneesException;
import org.junit.jupiter.api.Test;

class MessagesErreurTest {

    private static DonneesException connexion() {
        return new DonneesException(DonneesException.Type.CONNEXION, "Connexion impossible.", new SQLException());
    }

    @Test
    void messageDeNosExceptionsEstMontre() {
        assertEquals("Connexion impossible.", MessagesErreur.pourUtilisateur(connexion()));
        assertEquals("Email invalide.", MessagesErreur.pourUtilisateur(new ValidationException("Email invalide.")));
    }

    @Test
    void messageRetrouveSousLesEnveloppesJavaFx() {
        Throwable enveloppe = new RuntimeException("java.lang.reflect.InvocationTargetException",
                new InvocationTargetException(connexion()));

        assertEquals("Connexion impossible.", MessagesErreur.pourUtilisateur(enveloppe));
        assertTrue(MessagesErreur.estConnexion(enveloppe));
    }

    @Test
    void autreErreurDonneUnMessageGeneriqueSansDetailTechnique() {
        String message = MessagesErreur.pourUtilisateur(new NullPointerException("Cannot invoke \"x.getId()\""));

        assertEquals(MessagesErreur.MESSAGE_INATTENDU, message);
        assertFalse(message.contains("Null"));
        assertFalse(MessagesErreur.estConnexion(new IllegalStateException()));
    }

    @Test
    void messageVideOuNullNeSertPas() {
        assertEquals(MessagesErreur.MESSAGE_INATTENDU, MessagesErreur.pourUtilisateur(new ValidationException(" ")));
        assertEquals(MessagesErreur.MESSAGE_INATTENDU, MessagesErreur.pourUtilisateur(null));
    }

    @Test
    void chaineDeCausesEnBoucleNeBloquePas() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);

        assertEquals(MessagesErreur.MESSAGE_INATTENDU, MessagesErreur.pourUtilisateur(a));
    }
}
