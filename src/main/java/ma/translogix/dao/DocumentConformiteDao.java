package ma.translogix.dao;

import java.util.List;
import ma.translogix.model.DocumentConformite;

public interface DocumentConformiteDao extends CrudDao<DocumentConformite, Long> {
    List<DocumentConformite> findByEmploye(Long employeId);
}
