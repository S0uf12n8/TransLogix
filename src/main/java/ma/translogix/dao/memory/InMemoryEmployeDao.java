package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ma.translogix.dao.EmployeDao;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;

/** Keeps employees in a map. Used by tests and by the UI until the JDBC DAO of [M6] exists. */
public class InMemoryEmployeDao implements EmployeDao {
    private final Map<Long, Employe> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public List<Employe> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public Optional<Employe> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public List<Employe> findByPoste(Poste poste) {
        return donnees.values().stream().filter(e -> e.getPoste() == poste).toList();
    }

    @Override
    public Optional<Employe> findByUtilisateurId(Long utilisateurId) {
        if (utilisateurId == null) {
            return Optional.empty();
        }
        return donnees.values().stream()
                .filter(e -> Objects.equals(e.getUtilisateurId(), utilisateurId))
                .findFirst();
    }

    @Override
    public Optional<Employe> findByCin(String cin) {
        return donnees.values().stream().filter(e -> e.getCin().equals(cin)).findFirst();
    }

    @Override
    public Optional<Employe> findByMatricule(String matricule) {
        return donnees.values().stream().filter(e -> e.getMatricule().equals(matricule)).findFirst();
    }

    @Override
    public Employe save(Employe employe) {
        employe.setId(prochainId++);
        donnees.put(employe.getId(), employe);
        return employe;
    }

    @Override
    public void update(Employe employe) {
        if (!donnees.containsKey(employe.getId())) {
            throw new IllegalArgumentException("Employé inconnu : " + employe.getId());
        }
        donnees.put(employe.getId(), employe);
    }

    @Override
    public void delete(Long id) {
        donnees.remove(id);
    }
}
