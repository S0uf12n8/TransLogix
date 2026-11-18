package ma.translogix.dao;

import java.util.Optional;
import ma.translogix.model.Vehicule;

public interface VehiculeDao extends CrudDao<Vehicule, Long> {
    /** Exact match on the stored (upper-case) matricule. */
    Optional<Vehicule> findByMatricule(String matricule);
}
