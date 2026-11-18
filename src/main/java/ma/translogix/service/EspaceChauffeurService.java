package ma.translogix.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import ma.translogix.dao.EmployeDao;
import ma.translogix.model.Employe;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutMission;
import ma.translogix.util.ValidationException;

/**
 * The driver's own space ([O3]). The logged-in account is linked to one employee (Employe.utilisateurId);
 * the driver sees only HIS missions and can only start or deliver them. Every lifecycle rule stays in
 * MissionService. Missions carry no price, so nothing here can show one (ENF-03).
 */
public class EspaceChauffeurService {

    /** The only moves a driver may make: PLANIFIEE -> EN_COURS and EN_COURS -> LIVREE. */
    private static final Set<StatutMission> STATUTS_AUTORISES = Set.of(StatutMission.EN_COURS, StatutMission.LIVREE);

    /** On the road first, then the next departures, then the history (most recent first). */
    private static final Comparator<Mission> ORDRE = Comparator
            .comparingInt((Mission m) -> switch (m.getStatut()) {
                case EN_COURS -> 0;
                case PLANIFIEE -> 1;
                default -> 2;
            })
            .thenComparing((a, b) -> a.getStatut() == StatutMission.PLANIFIEE
                    ? a.getDateDebut().compareTo(b.getDateDebut())
                    : b.getDateDebut().compareTo(a.getDateDebut()));

    private final MissionService missions;
    private final EmployeDao employes;
    private final Supplier<Long> utilisateurConnecte;

    /** @param utilisateurConnecte id of the logged-in account (Session in the app, a fixed value in tests) */
    public EspaceChauffeurService(MissionService missions, EmployeDao employes, Supplier<Long> utilisateurConnecte) {
        this.missions = missions;
        this.employes = employes;
        this.utilisateurConnecte = utilisateurConnecte;
    }

    /** The employee linked to the logged-in account. */
    public Employe chauffeur() {
        return employes.findByUtilisateurId(utilisateurConnecte.get())
                .orElseThrow(() -> new ValidationException(
                        "Ce compte n'est lié à aucun chauffeur. Demandez au service RH de lier votre fiche."));
    }

    public List<Mission> mesMissions() {
        Long moi = chauffeur().getId();
        return missions.lister().stream().filter(m -> moi.equals(m.getChauffeurId())).sorted(ORDRE).toList();
    }

    public void demarrer(Long missionId) {
        changerStatut(missionId, StatutMission.EN_COURS);
    }

    public void livrer(Long missionId) {
        changerStatut(missionId, StatutMission.LIVREE);
    }

    /** Refuses another driver's mission and any status other than EN_COURS / LIVREE, then asks MissionService. */
    public void changerStatut(Long missionId, StatutMission nouveau) {
        Mission mission = missions.trouver(missionId);
        if (!chauffeur().getId().equals(mission.getChauffeurId())) {
            throw new ValidationException("La mission " + mission.getReference()
                    + " est affectée à un autre chauffeur.");
        }
        if (!STATUTS_AUTORISES.contains(nouveau)) {
            throw new ValidationException("Un chauffeur peut seulement démarrer ou livrer sa mission.");
        }
        missions.changerStatut(missionId, nouveau);
    }
}
