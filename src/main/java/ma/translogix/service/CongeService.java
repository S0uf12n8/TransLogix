package ma.translogix.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import ma.translogix.dao.CongeDao;
import ma.translogix.model.DemandeConge;
import ma.translogix.model.Employe;
import ma.translogix.model.StatutConge;
import ma.translogix.model.TypeConge;
import ma.translogix.util.ValidationException;

/**
 * Leave-request rules ([M3], iteration 3). No JavaFX and no SQL here.
 * A request starts EN_ATTENTE; only the RH can then approve or refuse it. Working days are counted
 * from the period, and an approved leave cannot overlap another approved leave of the same employee.
 */
public class CongeService {

    private final CongeDao dao;
    private final EmployeService employes;
    private final Clock clock;

    public CongeService(CongeDao dao, EmployeService employes, Clock clock) {
        this.dao = dao;
        this.employes = employes;
        this.clock = clock;
    }

    /** Real counters of the leave page right-hand card. */
    public record Compteurs(long enAttente, long approuves, long refuses) {
    }

    /** Every request, in creation order, whatever its status. */
    public List<DemandeConge> lister() {
        return dao.findAll();
    }

    public List<DemandeConge> listerParStatut(StatutConge statut) {
        return dao.findByStatut(statut);
    }

    public List<DemandeConge> listerParEmploye(Long employeId) {
        return dao.findByEmploye(employeId);
    }

    /**
     * Records a request for an active employee. The number of working days is computed here, never supplied
     * by the form, and the request is dated with the injected clock.
     */
    public DemandeConge creer(Long employeId, TypeConge type, LocalDate dateDebut, LocalDate dateFin, String motif) {
        Employe employe = employes.trouver(employeId);
        if (!employe.isActif()) {
            throw new ValidationException("Impossible de demander un congé pour un employé désactivé.");
        }
        if (type == null) {
            throw new ValidationException("Choisissez un type de congé.");
        }
        if (dateDebut == null || dateFin == null) {
            throw new ValidationException("Les dates de début et de fin sont obligatoires.");
        }
        if (dateFin.isBefore(dateDebut)) {
            throw new ValidationException("La date de fin doit être postérieure ou égale à la date de début.");
        }
        long jours = compterJoursOuvres(dateDebut, dateFin);
        if (jours == 0) {
            throw new ValidationException("La période ne contient aucun jour ouvré.");
        }
        boolean chevauche = dao.findByEmploye(employeId).stream()
                .filter(d -> d.getStatut() == StatutConge.APPROUVE)
                .anyMatch(d -> !dateFin.isBefore(d.getDateDebut()) && !dateDebut.isAfter(d.getDateFin()));
        if (chevauche) {
            throw new ValidationException("Cet employé est déjà en congé approuvé sur cette période.");
        }
        DemandeConge demande = new DemandeConge(null, employeId, type, dateDebut, dateFin, jours,
                StatutConge.EN_ATTENTE, vide(motif) ? null : motif.trim(), LocalDate.now(clock));
        return dao.save(demande);
    }

    public void approuver(Long id) {
        changerStatut(id, StatutConge.APPROUVE);
    }

    public void refuser(Long id) {
        changerStatut(id, StatutConge.REFUSE);
    }

    /** Number of pending requests, for the RH dashboard KPI. */
    public int aValider() {
        return dao.findByStatut(StatutConge.EN_ATTENTE).size();
    }

    public Compteurs compteurs() {
        return new Compteurs(dao.findByStatut(StatutConge.EN_ATTENTE).size(),
                dao.findByStatut(StatutConge.APPROUVE).size(),
                dao.findByStatut(StatutConge.REFUSE).size());
    }

    private void changerStatut(Long id, StatutConge nouveau) {
        DemandeConge demande = trouver(id);
        if (demande.getStatut() != StatutConge.EN_ATTENTE) {
            throw new ValidationException("Seules les demandes en attente peuvent être traitées.");
        }
        demande.setStatut(nouveau);
        dao.update(demande);
    }

    private DemandeConge trouver(Long id) {
        if (id == null) {
            throw new ValidationException("Demande de congé introuvable.");
        }
        return dao.findById(id).orElseThrow(() -> new ValidationException("Demande de congé introuvable."));
    }

    /** Monday to Friday, endpoints included. */
    public static long compterJoursOuvres(LocalDate debut, LocalDate fin) {
        return debut.datesUntil(fin.plusDays(1))
                .filter(jour -> jour.getDayOfWeek().getValue() < 6)
                .count();
    }

    private static boolean vide(String texte) {
        return texte == null || texte.isBlank();
    }
}
