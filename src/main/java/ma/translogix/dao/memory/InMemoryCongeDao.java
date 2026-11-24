package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ma.translogix.dao.CongeDao;
import ma.translogix.model.DemandeConge;
import ma.translogix.model.StatutConge;

/** Keeps leave requests in a map. Used by the UI and the tests until a JDBC DAO exists. */
public class InMemoryCongeDao implements CongeDao {
    private final Map<Long, DemandeConge> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public List<DemandeConge> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public Optional<DemandeConge> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public List<DemandeConge> findByEmploye(Long employeId) {
        return donnees.values().stream()
                .filter(d -> Objects.equals(d.getEmployeId(), employeId))
                .toList();
    }

    @Override
    public List<DemandeConge> findByStatut(StatutConge statut) {
        return donnees.values().stream().filter(d -> d.getStatut() == statut).toList();
    }

    @Override
    public DemandeConge save(DemandeConge demande) {
        demande.setId(prochainId++);
        donnees.put(demande.getId(), demande);
        return demande;
    }

    @Override
    public void update(DemandeConge demande) {
        if (!donnees.containsKey(demande.getId())) {
            throw new IllegalArgumentException("Demande de congé inconnue : " + demande.getId());
        }
        donnees.put(demande.getId(), demande);
    }

    @Override
    public void delete(Long id) {
        donnees.remove(id);
    }
}
