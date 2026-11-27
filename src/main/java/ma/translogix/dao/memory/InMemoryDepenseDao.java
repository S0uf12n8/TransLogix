package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ma.translogix.dao.DepenseDao;
import ma.translogix.model.Depense;

/** In-memory implementation used by the accountant page until AppContext switches to JDBC. */
public class InMemoryDepenseDao implements DepenseDao {
    private final Map<Long, Depense> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public synchronized Depense save(Depense depense) {
        depense.setId(prochainId++);
        donnees.put(depense.getId(), depense);
        return depense;
    }

    @Override
    public synchronized void update(Depense depense) {
        if (!donnees.containsKey(depense.getId())) {
            throw new IllegalArgumentException("Dépense inconnue : " + depense.getId());
        }
        donnees.put(depense.getId(), depense);
    }

    @Override
    public synchronized List<Depense> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public synchronized Optional<Depense> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }
}
