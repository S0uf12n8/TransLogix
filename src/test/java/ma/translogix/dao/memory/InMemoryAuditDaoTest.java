package ma.translogix.dao.memory;

import ma.translogix.dao.AuditDao;
import ma.translogix.dao.AuditDaoContrat;

class InMemoryAuditDaoTest extends AuditDaoContrat {
    @Override
    protected AuditDao daoVide() {
        return new InMemoryAuditDao();
    }
}
