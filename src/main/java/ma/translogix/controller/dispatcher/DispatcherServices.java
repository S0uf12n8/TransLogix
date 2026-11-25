package ma.translogix.controller.dispatcher;

import java.time.Clock;
import ma.translogix.AppContext;
import ma.translogix.dao.BonCarburantDao;
import ma.translogix.dao.IncidentDao;
import ma.translogix.dao.memory.InMemoryBonCarburantDao;
import ma.translogix.dao.memory.InMemoryIncidentDao;
import ma.translogix.service.BonCarburantService;
import ma.translogix.service.IncidentService;
import ma.translogix.service.MissionService;

/**
 * Composition root of the Dispatcher-only services. It keeps them alive for the whole session without
 * touching AppContext, which is shared with the other modules. Swap the two in-memory DAOs for their
 * JDBC counterparts once Souf agrees to wire the shared context.
 */
final class DispatcherServices {

    private static final Clock HORLOGE = Clock.systemDefaultZone();
    private static final MissionService MISSIONS = AppContext.missionService();
    private static final BonCarburantDao BONS_DAO = new InMemoryBonCarburantDao();
    private static final IncidentDao INCIDENTS_DAO = new InMemoryIncidentDao();
    private static final BonCarburantService BONS = new BonCarburantService(BONS_DAO, MISSIONS, HORLOGE);
    private static final IncidentService INCIDENTS = new IncidentService(INCIDENTS_DAO, MISSIONS, HORLOGE);

    private DispatcherServices() {
    }

    static BonCarburantService bons() {
        return BONS;
    }

    static IncidentService incidents() {
        return INCIDENTS;
    }
}
