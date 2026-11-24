package ma.translogix.dao.memory;

import ma.translogix.dao.ClientDao;
import ma.translogix.dao.FactureDao;
import ma.translogix.dao.FactureDaoContrat;

class InMemoryFactureDaoTest extends FactureDaoContrat {
    @Override
    protected FactureDao daoVide() {
        return new InMemoryFactureDao();
    }

    @Override
    protected ClientDao clientDaoVide() {
        return new InMemoryClientDao();
    }
}
