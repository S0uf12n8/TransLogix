package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ma.translogix.dao.BonCarburantDao;
import ma.translogix.model.BonCarburant;

/** Fuel vouchers kept in memory until the JDBC implementation is plugged in by the composition root. */
public class InMemoryBonCarburantDao implements BonCarburantDao {
    private final Map<Long, BonCarburant> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public List<BonCarburant> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public Optional<BonCarburant> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public List<BonCarburant> findByMission(Long missionId) {
        return donnees.values().stream()
                .filter(b -> Objects.equals(b.getMissionId(), missionId))
                .sorted(Comparator.comparing(BonCarburant::getDate).reversed())
                .toList();
    }

    @Override
    public BonCarburant save(BonCarburant bon) {
        bon.setId(prochainId++);
        donnees.put(bon.getId(), bon);
        return bon;
    }

    @Override
    public void update(BonCarburant bon) {
        if (!donnees.containsKey(bon.getId())) {
            throw new IllegalArgumentException("Bon de carburant inconnu : " + bon.getId());
        }
        donnees.put(bon.getId(), bon);
    }

    @Override
    public void delete(Long id) {
        donnees.remove(id);
    }
}
