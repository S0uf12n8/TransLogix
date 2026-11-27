package ma.translogix.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import ma.translogix.dao.IncidentDao;
import ma.translogix.model.GraviteIncident;
import ma.translogix.model.Incident;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutIncident;
import ma.translogix.model.StatutMission;
import ma.translogix.model.TypeIncident;
import ma.translogix.util.ValidationException;

/**
 * Road incidents of the Dispatcher module. No JavaFX and no SQL here: every write goes through the DAO.
 * An incident can only be declared while its mission is still planned or in progress.
 */
public class IncidentService {

    /** First reference of a new database. */
    static final int PREMIER_NUMERO = 5001;

    private static final Set<StatutMission> MISSIONS_DECLARABLES =
            EnumSet.of(StatutMission.PLANIFIEE, StatutMission.EN_COURS);

    private final IncidentDao dao;
    private final MissionService missions;
    private final Clock horloge;

    public IncidentService(IncidentDao dao, MissionService missions, Clock horloge) {
        this.dao = dao;
        this.missions = missions;
        this.horloge = horloge;
    }

    /** All incidents, newest first. */
    public List<Incident> lister() {
        return dao.findAll().stream()
                .sorted(Comparator.comparing(Incident::getDateSignalement).thenComparing(Incident::getId).reversed())
                .toList();
    }

    /** Missions an incident can be declared for; the dialog does not hardcode this rule. */
    public List<Mission> missionsEligibles() {
        return missions.lister().stream().filter(m -> MISSIONS_DECLARABLES.contains(m.getStatut())).toList();
    }

    /** Declares and saves one incident through the DAO. */
    public Incident declarer(Long missionId, TypeIncident type, GraviteIncident gravite, String description,
                             String localisation, LocalDateTime dateSignalement) {
        Mission mission = missions.trouver(missionId);
        if (!MISSIONS_DECLARABLES.contains(mission.getStatut())) {
            throw new ValidationException("Un incident ne peut être déclaré que pour une mission planifiée ou en cours ("
                    + mission.getReference() + " est « " + mission.getStatut().getLibelle() + " »).");
        }
        if (type == null) {
            throw new ValidationException("Choisissez le type d'incident.");
        }
        if (gravite == null) {
            throw new ValidationException("Choisissez la gravité de l'incident.");
        }
        String detail = obligatoire(description, "La description est obligatoire.");
        String lieu = obligatoire(localisation, "La localisation est obligatoire.");
        if (dateSignalement == null) {
            throw new ValidationException("La date et l'heure de signalement sont obligatoires.");
        }
        if (dateSignalement.isAfter(LocalDateTime.now(horloge))) {
            throw new ValidationException("Le signalement ne peut pas être dans le futur.");
        }

        Incident incident = new Incident(null, prochaineReference(), mission.getId(), type, gravite, detail, lieu,
                dateSignalement, StatutIncident.OUVERT);
        return dao.save(incident);
    }

    private String prochaineReference() {
        int dernier = dao.findAll().stream()
                .map(Incident::getReference)
                .filter(r -> r != null && r.matches("INC-\\d+"))
                .mapToInt(r -> Integer.parseInt(r.substring(4)))
                .max()
                .orElse(PREMIER_NUMERO - 1);
        return "INC-" + (dernier + 1);
    }

    private static String obligatoire(String texte, String message) {
        if (texte == null || texte.isBlank()) {
            throw new ValidationException(message);
        }
        return texte.trim();
    }
}
