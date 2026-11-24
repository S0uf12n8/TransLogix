package ma.translogix.dao.memory;

import ma.translogix.dao.EmployeDao;
import ma.translogix.dao.EmployeDaoContrat;

/** Runs the shared EmployeDao contract against the in-memory implementation. */
class InMemoryEmployeDaoTest extends EmployeDaoContrat {

    @Override
    protected EmployeDao daoVide() {
        return new InMemoryEmployeDao();
    }
}
