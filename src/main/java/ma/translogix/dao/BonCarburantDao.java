package ma.translogix.dao;

import java.util.List;
import ma.translogix.model.BonCarburant;

/** Fuel vouchers issued by the dispatcher. */
public interface BonCarburantDao extends CrudDao<BonCarburant, Long> {
    /** Vouchers of one mission, by date then reference. */
    List<BonCarburant> findByMission(Long missionId);
}
