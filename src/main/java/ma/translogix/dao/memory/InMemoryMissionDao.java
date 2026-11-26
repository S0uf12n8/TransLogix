package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import ma.translogix.dao.CritereMission;
import ma.translogix.dao.MissionDao;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;
import ma.translogix.util.Recherche;

/** Keeps missions in a map. Used by tests and by the UI until the JDBC DAO of [H6] exists. */
public class InMemoryMissionDao implements MissionDao {
    private final Map<Long, Mission> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public List<Mission> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public Optional<Mission> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public List<Mission> findByChauffeur(Long chauffeurId) {
        return donnees.values().stream().filter(m -> Objects.equals(m.getChauffeurId(), chauffeurId)).toList();
    }

    @Override
    public List<Mission> findByVehicule(Long vehiculeId) {
        return donnees.values().stream().filter(m -> Objects.equals(m.getVehiculeId(), vehiculeId)).toList();
    }

    @Override
    public List<Mission> search(CritereMission c) {
        CritereMission critere = c == null ? CritereMission.TOUT : c;
        List<Long> idsChauffeurs = critere.idsChauffeursMotes() == null
                ? List.of() : critere.idsChauffeursMotes();
        boolean sansChauffeur = critere.chauffeur() == null || critere.chauffeur().isBlank();
        return donnees.values().stream()
                .filter(m -> Recherche.correspond(critere.reference(), m.getReference()))
                .filter(m -> sansChauffeur || idsChauffeurs.contains(m.getChauffeurId()))
                .filter(m -> Recherche.correspond(critere.ville(), m.getVilleDepart(), m.getVilleArrivee()))
                .toList();
    }

    @Override
    public Map<StatutMission, Long> compterParStatut() {
        Map<StatutMission, Long> comptages = new EnumMap<>(StatutMission.class);
        for (StatutMission statut : StatutMission.values()) {
            comptages.put(statut, 0L);
        }
        for (Mission mission : donnees.values()) {
            if (mission.getStatut() != null) {
                comptages.merge(mission.getStatut(), 1L, Long::sum);
            }
        }
        return comptages;
    }

    @Override
    public Set<Long> findVehiculesEnMission() {
        return donnees.values().stream()
                .filter(m -> m.getStatut() == StatutMission.EN_COURS && m.getVehiculeId() != null)
                .map(Mission::getVehiculeId)
                .collect(Collectors.toSet());
    }

    @Override
    public Mission save(Mission mission) {
        mission.setId(prochainId++);
        donnees.put(mission.getId(), mission);
        return mission;
    }

    @Override
    public void update(Mission mission) {
        if (!donnees.containsKey(mission.getId())) {
            throw new IllegalArgumentException("Mission inconnue : " + mission.getId());
        }
        donnees.put(mission.getId(), mission);
    }

    @Override
    public void delete(Long id) {
        donnees.remove(id);
    }
}
