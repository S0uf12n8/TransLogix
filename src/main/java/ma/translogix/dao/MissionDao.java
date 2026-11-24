package ma.translogix.dao;

import java.util.List;
import ma.translogix.model.Mission;

public interface MissionDao extends CrudDao<Mission, Long> {
    /** Missions of one driver (Employe id), every status. */
    List<Mission> findByChauffeur(Long chauffeurId);

    /** Missions of one vehicle, every status. */
    List<Mission> findByVehicule(Long vehiculeId);

    /**
     * Missions matching all the supplied criteria. A JDBC implementation adds each WHERE clause
     * conditionally and passes every value as a PreparedStatement parameter (never string-built SQL).
     */
    List<Mission> search(CritereMission critere);
}
