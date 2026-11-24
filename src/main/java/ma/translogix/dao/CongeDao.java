package ma.translogix.dao;

import java.util.List;
import ma.translogix.model.DemandeConge;
import ma.translogix.model.StatutConge;

public interface CongeDao extends CrudDao<DemandeConge, Long> {
    List<DemandeConge> findByEmploye(Long employeId);

    List<DemandeConge> findByStatut(StatutConge statut);
}
