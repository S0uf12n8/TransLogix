package ma.translogix.dao.memory;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ma.translogix.dao.PaieDao;
import ma.translogix.model.Paie;

/** Keeps payroll records in a map. Used by the UI until the JDBC DAO exists. */
public class InMemoryPaieDao implements PaieDao {
    private final Map<Long, Paie> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public synchronized Paie save(Paie paie) {
        paie.setId(prochainId++);
        donnees.put(paie.getId(), paie);
        return paie;
    }

    @Override
    public synchronized void update(Paie paie) {
        if (!donnees.containsKey(paie.getId())) {
            throw new IllegalArgumentException("Fiche de paie inconnue : " + paie.getId());
        }
        donnees.put(paie.getId(), paie);
    }

    @Override
    public synchronized List<Paie> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public synchronized Optional<Paie> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public synchronized Optional<Paie> findByEmployeEtPeriode(Long employeId, YearMonth periode) {
        return donnees.values().stream()
                .filter(p -> Objects.equals(p.getEmployeId(), employeId) && Objects.equals(p.getPeriode(), periode))
                .findFirst();
    }

    @Override
    public synchronized Optional<Paie> findByNumero(String numero) {
        return donnees.values().stream().filter(p -> Objects.equals(p.getNumero(), numero)).findFirst();
    }
}
