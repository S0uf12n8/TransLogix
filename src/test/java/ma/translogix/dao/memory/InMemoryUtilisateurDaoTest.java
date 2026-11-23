package ma.translogix.dao.memory;

import ma.translogix.dao.UtilisateurDao;
import ma.translogix.dao.UtilisateurDaoContrat;

class InMemoryUtilisateurDaoTest extends UtilisateurDaoContrat {
    @Override
    protected UtilisateurDao daoVide() {
        return new InMemoryUtilisateurDao();
    }
}
