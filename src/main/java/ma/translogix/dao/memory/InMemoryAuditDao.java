package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.List;
import ma.translogix.dao.AuditDao;
import ma.translogix.model.EvenementAudit;

/** Keeps audit events in a list. Lost when the app stops, until the JDBC DAO exists. */
public class InMemoryAuditDao implements AuditDao {
    private final List<EvenementAudit> donnees = new ArrayList<>();
    private long prochainId = 1;

    @Override
    public synchronized EvenementAudit save(EvenementAudit evenement) {
        evenement.setId(prochainId++);
        donnees.add(evenement);
        return evenement;
    }

    @Override
    public synchronized List<EvenementAudit> findAll() {
        return new ArrayList<>(donnees);
    }
}
