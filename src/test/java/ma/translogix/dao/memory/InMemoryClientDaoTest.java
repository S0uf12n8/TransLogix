package ma.translogix.dao.memory;

import ma.translogix.dao.ClientDao;
import ma.translogix.dao.ClientDaoContrat;

class InMemoryClientDaoTest extends ClientDaoContrat {
    @Override
    protected ClientDao daoVide() {
        return new InMemoryClientDao();
    }
}
