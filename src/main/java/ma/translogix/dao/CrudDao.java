package ma.translogix.dao;

import java.util.List;
import java.util.Optional;

/** Shared contract for every DAO. Each entity has an in-memory and a JDBC implementation. */
public interface CrudDao<T, ID> {
    List<T> findAll();

    Optional<T> findById(ID id);

    T save(T entity);

    void update(T entity);

    void delete(ID id);
}
