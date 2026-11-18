package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ma.translogix.dao.FactureDao;
import ma.translogix.model.Facture;

/** Keeps invoices in a map. Used by tests and by the UI until the JDBC DAO exists ([C5]). */
public class InMemoryFactureDao implements FactureDao {
    private final Map<Long, Facture> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public List<Facture> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public Optional<Facture> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public List<Facture> findByClient(Long clientId) {
        return donnees.values().stream().filter(f -> clientId.equals(f.getClientId())).toList();
    }

    @Override
    public Facture save(Facture facture) {
        facture.setId(prochainId++);
        donnees.put(facture.getId(), facture);
        return facture;
    }

    @Override
    public void update(Facture facture) {
        if (!donnees.containsKey(facture.getId())) {
            throw new IllegalArgumentException("Facture inconnue : " + facture.getId());
        }
        donnees.put(facture.getId(), facture);
    }

    @Override
    public void delete(Long id) {
        donnees.remove(id);
    }
}
