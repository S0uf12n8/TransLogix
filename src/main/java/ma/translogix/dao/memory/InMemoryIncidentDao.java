package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ma.translogix.dao.IncidentDao;
import ma.translogix.model.Incident;

/** Incidents kept in memory until the JDBC implementation is plugged in by the composition root. */
public class InMemoryIncidentDao implements IncidentDao {
    private final Map<Long, Incident> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public List<Incident> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public Optional<Incident> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public List<Incident> findByMission(Long missionId) {
        return donnees.values().stream()
                .filter(i -> Objects.equals(i.getMissionId(), missionId))
                .sorted(Comparator.comparing(Incident::getDateSignalement).reversed())
                .toList();
    }

    @Override
    public Incident save(Incident incident) {
        incident.setId(prochainId++);
        donnees.put(incident.getId(), incident);
        return incident;
    }

    @Override
    public void update(Incident incident) {
        if (!donnees.containsKey(incident.getId())) {
            throw new IllegalArgumentException("Incident inconnu : " + incident.getId());
        }
        donnees.put(incident.getId(), incident);
    }

    @Override
    public void delete(Long id) {
        donnees.remove(id);
    }
}
