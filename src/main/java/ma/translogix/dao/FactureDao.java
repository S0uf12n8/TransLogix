package ma.translogix.dao;

import java.util.List;
import java.util.Optional;
import ma.translogix.model.Facture;

public interface FactureDao extends CrudDao<Facture, Long> {
    List<Facture> findByClient(Long clientId);

    /** The invoice of a mission, cancelled ones included (facture.mission_id is UNIQUE). */
    Optional<Facture> findByMission(Long missionId);

    /**
     * Invoices matching all the supplied criteria. A JDBC implementation adds each WHERE clause
     * conditionally and passes every value as a PreparedStatement parameter (never string-built SQL).
     */
    List<Facture> search(CritereFacture critere);
}
