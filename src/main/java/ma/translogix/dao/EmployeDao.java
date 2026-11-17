package ma.translogix.dao;

import java.util.List;
import java.util.Optional;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;

public interface EmployeDao extends CrudDao<Employe, Long> {
    List<Employe> findByPoste(Poste poste);

    Optional<Employe> findByUtilisateurId(Long utilisateurId);

    /** Exact match on the stored (upper-case) CIN. */
    Optional<Employe> findByCin(String cin);

    Optional<Employe> findByMatricule(String matricule);
}
