package ma.translogix.dao;

import java.util.List;
import java.util.Optional;
import ma.translogix.model.Depense;

/**
 * Driver expenses. A submitted expense is kept even when refused: the history must stay auditable.
 * There is deliberately no delete method.
 */
public interface DepenseDao {
    Depense save(Depense depense);

    /** @throws IllegalArgumentException when the id is unknown (same behaviour as the in-memory DAO) */
    void update(Depense depense);

    List<Depense> findAll();

    Optional<Depense> findById(Long id);
}
