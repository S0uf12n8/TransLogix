package ma.translogix.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;

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

    /** Number of missions per stored status, including zeros for statuses with no mission. */
    Map<StatutMission, Long> compterParStatut();

    /** Ids of the vehicles currently on an EN_COURS mission (one id even if data is inconsistent). */
    Set<Long> findVehiculesEnMission();
}
