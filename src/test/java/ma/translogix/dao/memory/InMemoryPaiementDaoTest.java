package ma.translogix.dao.memory;

import ma.translogix.dao.ClientDao;
import ma.translogix.dao.FactureDao;
import ma.translogix.dao.PaiementDao;
import ma.translogix.dao.PaiementDaoContrat;

class InMemoryPaiementDaoTest extends PaiementDaoContrat {
    @Override
    protected PaiementDao daoVide() {
        return new InMemoryPaiementDao();
    }

    @Override
    protected ClientDao clientDaoVide() {
        return new InMemoryClientDao();
    }

    @Override
    protected FactureDao factureDaoVide() {
        return new InMemoryFactureDao();
    }
}
