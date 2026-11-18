package ma.translogix.dao;

import java.util.List;
import ma.translogix.model.Facture;

public interface FactureDao extends CrudDao<Facture, Long> {
    List<Facture> findByClient(Long clientId);
}
