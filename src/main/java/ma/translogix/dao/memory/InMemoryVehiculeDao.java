package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ma.translogix.dao.VehiculeDao;
import ma.translogix.model.Vehicule;

/** Keeps vehicles in a map. Used by tests and by the UI until the JDBC DAO of [H6] exists. */
public class InMemoryVehiculeDao implements VehiculeDao {
    private final Map<Long, Vehicule> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public List<Vehicule> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public Optional<Vehicule> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public Optional<Vehicule> findByMatricule(String matricule) {
        return donnees.values().stream().filter(v -> v.getMatricule().equals(matricule)).findFirst();
    }

    @Override
    public Vehicule save(Vehicule vehicule) {
        vehicule.setId(prochainId++);
        donnees.put(vehicule.getId(), vehicule);
        return vehicule;
    }

    @Override
    public void update(Vehicule vehicule) {
        if (!donnees.containsKey(vehicule.getId())) {
            throw new IllegalArgumentException("Véhicule inconnu : " + vehicule.getId());
        }
        donnees.put(vehicule.getId(), vehicule);
    }

    @Override
    public void delete(Long id) {
        donnees.remove(id);
    }
}
