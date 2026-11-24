package ma.translogix.dao.memory;

import ma.translogix.dao.DepenseDao;
import ma.translogix.dao.DepenseDaoContrat;

class InMemoryDepenseDaoTest extends DepenseDaoContrat {

    @Override
    protected DepenseDao daoVide() {
        return new InMemoryDepenseDao();
    }
}
