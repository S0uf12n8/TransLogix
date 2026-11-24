package ma.translogix.dao.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import ma.translogix.dao.DonneesException;

/**
 * Transaction boundary for multi-statement writes (e.g. "insert a payment AND mark the invoice paid").
 * One connection is opened, auto-commit is disabled, the operation uses that same connection, then either
 * commit or rollback; the connection is always closed. Any SQLException comes out as a French
 * {@link DonneesException} through {@link SqlErreurs}.
 *
 * <p>The operation must use the {@link Connection} passed to it. Calling another DAO that opens its own
 * connection would not be part of the transaction. No SQL is built here.</p>
 */
public final class Transactions {

    /** Work to run inside one transaction. */
    @FunctionalInterface
    public interface Operation<T> {
        T executer(Connection connexion) throws SQLException;
    }

    private Transactions() {
    }

    /**
     * Runs {@code operation} in a transaction and closes the connection afterwards.
     * A RuntimeException (already translated by a nested DAO, for instance) is rolled back and rethrown as-is.
     */
    public static <T> T dansTransaction(ConnexionBase base, Operation<T> operation) {
        return SqlErreurs.executer(() -> {
            try (Connection connexion = base.ouvrir()) {
                connexion.setAutoCommit(false);
                try {
                    T resultat = operation.executer(connexion);
                    connexion.commit();
                    return resultat;
                } catch (SQLException | RuntimeException erreur) {
                    try {
                        connexion.rollback();
                    } catch (SQLException rollback) {
                        erreur.addSuppressed(rollback); // keep the original, user-friendly error first
                    }
                    throw erreur;
                }
            }
        });
    }
}
