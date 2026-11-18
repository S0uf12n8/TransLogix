package ma.translogix.dao;

import java.util.List;
import ma.translogix.model.Mission;

public interface MissionDao extends CrudDao<Mission, Long> {
    /** Missions of one driver (Employe id), every status. */
    List<Mission> findByChauffeur(Long chauffeurId);

    /** Missions of one vehicle, every status. */
    List<Mission> findByVehicule(Long vehiculeId);
}
