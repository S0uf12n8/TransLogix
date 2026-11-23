package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import ma.translogix.dao.DonneesException;
import ma.translogix.dao.DonneesException.Type;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Transaction behaviour is tested with a fake JDBC driver: no MySQL needed, and no SQL is executed. */
class TransactionsTest {

    private static final String URL = "jdbc:translogix-test:transactions";
    private static final Pilote PILOTE = new Pilote();

    private Connexion journal;

    @BeforeAll
    static void enregistrerPilote() throws SQLException {
        DriverManager.registerDriver(PILOTE);
    }

    @AfterAll
    static void retirerPilote() throws SQLException {
        DriverManager.deregisterDriver(PILOTE);
    }

    @BeforeEach
    void preparer() {
        journal = new Connexion();
        PILOTE.journal = journal;
    }

    private static ConnexionBase base() {
        return new ConnexionBase(URL, "test", "test");
    }

    @Test
    void succesValidePuisFermeLaConnexion() {
        String resultat = Transactions.dansTransaction(base(), connexion -> "ok");

        assertEquals("ok", resultat);
        assertEquals(List.of("setAutoCommit(false)", "commit", "close"), journal.appels);
    }

    @Test
    void erreurSqlEstTraduiteEnFrancaisAvecRollback() {
        SQLException sql = new SQLException("Duplicate entry 'X1' for key 'k'", "23000", 1062);

        DonneesException erreur = assertThrows(DonneesException.class,
                () -> Transactions.dansTransaction(base(), connexion -> {
                    throw sql;
                }));

        assertEquals(Type.DOUBLON, erreur.getType());
        assertSame(sql, erreur.getCause());
        assertEquals(List.of("setAutoCommit(false)", "rollback", "close"), journal.appels);
    }

    @Test
    void erreurDejaTraduiteEstRenvoyeeTelleQuelleAvecRollback() {
        DonneesException initiale = new DonneesException(Type.CONNEXION,
                "Connexion à la base de données impossible.", new SQLException("link failure"));

        DonneesException erreur = assertThrows(DonneesException.class,
                () -> Transactions.dansTransaction(base(), connexion -> {
                    throw initiale;
                }));

        assertSame(initiale, erreur);
        assertEquals(List.of("setAutoCommit(false)", "rollback", "close"), journal.appels);
    }

    @Test
    void echecDuRollbackNeCachePasLerreurInitiale() {
        SQLException sql = new SQLException("foreign key", "23000", 1452);
        journal.erreurRollback = new SQLException("rollback impossible");

        DonneesException erreur = assertThrows(DonneesException.class,
                () -> Transactions.dansTransaction(base(), connexion -> {
                    throw sql;
                }));

        assertEquals(Type.REFERENCE_INCONNUE, erreur.getType());
        assertEquals(1, erreur.getCause().getSuppressed().length);
        assertEquals("rollback impossible", erreur.getCause().getSuppressed()[0].getMessage());
        assertEquals(List.of("setAutoCommit(false)", "rollback", "close"), journal.appels);
    }

    /** Records the transaction calls; every other Connection method returns a harmless default. */
    private static final class Connexion implements InvocationHandler {
        final List<String> appels = new ArrayList<>();
        SQLException erreurRollback;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "ConnexionDeTest";
                case "setAutoCommit" -> journal("setAutoCommit(" + args[0] + ")");
                case "commit" -> journal("commit");
                case "rollback" -> {
                    journal("rollback");
                    if (erreurRollback != null) {
                        throw erreurRollback;
                    }
                    yield null;
                }
                case "close" -> journal("close");
                case "isClosed" -> appels.contains("close");
                default -> valeurParDefaut(method.getReturnType());
            };
        }

        private Object journal(String appel) {
            appels.add(appel);
            return null;
        }
    }

    /** Minimal JDBC driver returning the recorded fake connection for the test URL only. */
    private static final class Pilote implements Driver {
        Connexion journal;

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class}, journal);
        }

        @Override
        public boolean acceptsURL(String url) {
            return URL.equals(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
    }

    private static Object valeurParDefaut(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        return 0d;
    }
}
