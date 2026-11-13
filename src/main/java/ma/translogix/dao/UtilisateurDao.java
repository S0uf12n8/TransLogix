package ma.translogix.dao;

import java.util.Optional;
import ma.translogix.model.Utilisateur;

public interface UtilisateurDao extends CrudDao<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
}
