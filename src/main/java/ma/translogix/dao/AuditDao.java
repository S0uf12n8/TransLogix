package ma.translogix.dao;

import java.util.List;
import ma.translogix.model.EvenementAudit;

/**
 * Audit log storage. Append-only on purpose: it does NOT extend CrudDao,
 * so no code path can update or delete a recorded event.
 */
public interface AuditDao {
    EvenementAudit save(EvenementAudit evenement);

    /** All events, in insertion order. */
    List<EvenementAudit> findAll();
}
