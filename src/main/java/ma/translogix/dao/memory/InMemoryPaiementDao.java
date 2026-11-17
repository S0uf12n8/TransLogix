package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.List;
import ma.translogix.dao.PaiementDao;
import ma.translogix.model.Paiement;

/** Keeps payments in a list. Used by tests and by the UI until the JDBC DAO exists ([C5]). */
public class InMemoryPaiementDao implements PaiementDao {
    private final List<Paiement> donnees = new ArrayList<>();
    private long prochainId = 1;

    @Override
    public synchronized Paiement save(Paiement paiement) {
        paiement.setId(prochainId++);
        donnees.add(paiement);
        return paiement;
    }

    @Override
    public synchronized List<Paiement> findAll() {
        return new ArrayList<>(donnees);
    }

    @Override
    public synchronized List<Paiement> findByFacture(Long factureId) {
        return donnees.stream().filter(p -> factureId.equals(p.getFactureId())).toList();
    }
}
