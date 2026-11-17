package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ma.translogix.dao.DocumentConformiteDao;
import ma.translogix.model.DocumentConformite;

/** Keeps compliance documents in a map. Used by tests and by the UI until the JDBC DAO of [M6] exists. */
public class InMemoryDocumentConformiteDao implements DocumentConformiteDao {
    private final Map<Long, DocumentConformite> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public List<DocumentConformite> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public Optional<DocumentConformite> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public List<DocumentConformite> findByEmploye(Long employeId) {
        return donnees.values().stream().filter(d -> d.getEmployeId().equals(employeId)).toList();
    }

    @Override
    public DocumentConformite save(DocumentConformite document) {
        document.setId(prochainId++);
        donnees.put(document.getId(), document);
        return document;
    }

    @Override
    public void update(DocumentConformite document) {
        if (!donnees.containsKey(document.getId())) {
            throw new IllegalArgumentException("Document inconnu : " + document.getId());
        }
        donnees.put(document.getId(), document);
    }

    @Override
    public void delete(Long id) {
        donnees.remove(id);
    }
}
