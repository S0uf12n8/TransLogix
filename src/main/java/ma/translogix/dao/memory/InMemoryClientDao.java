package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ma.translogix.dao.ClientDao;
import ma.translogix.model.Client;

/** Keeps clients in a map. Used by tests and by the UI until the JDBC DAO exists ([C5]). */
public class InMemoryClientDao implements ClientDao {
    private final Map<Long, Client> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public List<Client> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public Optional<Client> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public Optional<Client> findByIce(String ice) {
        if (ice == null || ice.isBlank()) {
            return Optional.empty();
        }
        return donnees.values().stream().filter(c -> ice.equals(c.getIce())).findFirst();
    }

    @Override
    public Client save(Client client) {
        client.setId(prochainId++);
        donnees.put(client.getId(), client);
        return client;
    }

    @Override
    public void update(Client client) {
        if (!donnees.containsKey(client.getId())) {
            throw new IllegalArgumentException("Client inconnu : " + client.getId());
        }
        donnees.put(client.getId(), client);
    }

    @Override
    public void delete(Long id) {
        donnees.remove(id);
    }
}
