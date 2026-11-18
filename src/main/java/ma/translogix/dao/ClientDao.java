package ma.translogix.dao;

import java.util.Optional;
import ma.translogix.model.Client;

public interface ClientDao extends CrudDao<Client, Long> {
    /** Exact match on the ICE (null or blank ICE never matches). */
    Optional<Client> findByIce(String ice);
}
