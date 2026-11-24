package ma.translogix.dao.memory;

import ma.translogix.dao.DocumentConformiteDao;
import ma.translogix.dao.DocumentConformiteDaoContrat;
import ma.translogix.dao.EmployeDao;

/** Runs the shared DocumentConformiteDao contract against the in-memory implementation. */
class InMemoryDocumentConformiteDaoTest extends DocumentConformiteDaoContrat {

    @Override
    protected DocumentConformiteDao daoVide() {
        return new InMemoryDocumentConformiteDao();
    }

    @Override
    protected EmployeDao employeDaoVide() {
        return new InMemoryEmployeDao();
    }
}
