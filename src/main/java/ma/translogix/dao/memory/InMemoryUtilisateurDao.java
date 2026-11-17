package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ma.translogix.dao.UtilisateurDao;
import ma.translogix.model.Utilisateur;

/** Keeps users in a map. Used by tests and by the UI until the JDBC DAO exists. */
public class InMemoryUtilisateurDao implements UtilisateurDao {
    private final Map<Long, Utilisateur> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public List<Utilisateur> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public Optional<Utilisateur> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public Optional<Utilisateur> findByEmail(String email) {
        return donnees.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public Utilisateur save(Utilisateur utilisateur) {
        utilisateur.setId(prochainId++);
        donnees.put(utilisateur.getId(), utilisateur);
        return utilisateur;
    }

    @Override
    public void update(Utilisateur utilisateur) {
        if (!donnees.containsKey(utilisateur.getId())) {
            throw new IllegalArgumentException("Utilisateur inconnu : " + utilisateur.getId());
        }
        donnees.put(utilisateur.getId(), utilisateur);
    }

    @Override
    public void delete(Long id) {
        donnees.remove(id);
    }
}
