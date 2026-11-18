package ma.translogix.dao;

import java.util.List;
import ma.translogix.model.Paiement;

/** Payments received. Append-only like the audit log: a recorded payment is never edited or deleted. */
public interface PaiementDao {
    Paiement save(Paiement paiement);

    List<Paiement> findAll();

    List<Paiement> findByFacture(Long factureId);
}
