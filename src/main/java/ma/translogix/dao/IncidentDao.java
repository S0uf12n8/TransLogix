package ma.translogix.dao;

import java.util.List;
import ma.translogix.model.Incident;

/** Road incidents reported by the dispatcher. */
public interface IncidentDao extends CrudDao<Incident, Long> {
    /** Incidents of one mission, by report time. */
    List<Incident> findByMission(Long missionId);
}
