package ma.translogix.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import ma.translogix.dao.AuditDao;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.ResultatAudit;
import ma.translogix.util.Recherche;
import ma.translogix.util.ValidationException;

/** Records and reads the audit log. No JavaFX and no SQL here. */
public class AuditService {
    /** Display format of the timestamp, also used by the search. */
    public static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");

    private static final Comparator<EvenementAudit> RECENT_D_ABORD =
            Comparator.comparing(EvenementAudit::getHorodatage)
                    .thenComparing(EvenementAudit::getId)
                    .reversed();

    private final AuditDao dao;
    private final Clock horloge;
    private final Supplier<String> acteur;

    /**
     * @param acteur who is acting right now (e.g. the logged-in user); called at each record
     */
    public AuditService(AuditDao dao, Clock horloge, Supplier<String> acteur) {
        this.dao = dao;
        this.horloge = horloge;
        this.acteur = acteur;
    }

    public EvenementAudit enregistrer(String module, String action, ResultatAudit resultat) {
        return enregistrer(acteur.get(), module, action, resultat);
    }

    /**
     * Same, with an explicit actor: for events that happen before anyone is logged in (a login attempt is done
     * by the email typed on the login screen, not by "Système").
     */
    public EvenementAudit enregistrer(String qui, String module, String action, ResultatAudit resultat) {
        if (estVide(module) || estVide(action)) {
            throw new ValidationException("Le module et l'action de l'audit sont obligatoires.");
        }
        if (resultat == null) {
            throw new ValidationException("Le résultat de l'audit est obligatoire.");
        }
        EvenementAudit evenement = new EvenementAudit(null, LocalDateTime.now(horloge),
                estVide(qui) ? "Inconnu" : couper(qui.trim(), 150), couper(module.trim(), 60),
                couper(action.trim(), 500), resultat);
        return dao.save(evenement);
    }

    /** Every event, most recent first. */
    public List<EvenementAudit> lister() {
        return dao.findAll().stream().sorted(RECENT_D_ABORD).toList();
    }

    /** Events whose date, user, module, action or result contain the query (case and accents ignored). */
    public List<EvenementAudit> rechercher(String requete) {
        return lister().stream().filter(e -> correspond(e, requete)).toList();
    }

    public static boolean correspond(EvenementAudit e, String requete) {
        return Recherche.correspond(requete, e.getHorodatage().format(FORMAT_DATE),
                e.getUtilisateur(), e.getModule(), e.getAction(), e.getResultat().getLibelle());
    }

    /**
     * Search criteria of the audit screen, all combined (AND). null or blank = criterion not used.
     * @param texte  free text, as in rechercher(String)
     * @param du     first day included
     * @param au     last day included
     */
    public record CritereAudit(String texte, String module, ResultatAudit resultat, LocalDate du, LocalDate au) {
        public static final CritereAudit AUCUN = new CritereAudit(null, null, null, null, null);
    }

    /**
     * Events matching every criterion, most recent first.
     * @throws ValidationException if the period ends before it starts
     */
    public List<EvenementAudit> filtrer(CritereAudit critere) {
        verifierPeriode(critere);
        return lister().stream().filter(e -> correspond(e, critere)).toList();
    }

    /** Same rule as filtrer, for one event (the screen filters its own list with it). */
    public static boolean correspond(EvenementAudit e, CritereAudit critere) {
        CritereAudit c = critere == null ? CritereAudit.AUCUN : critere;
        LocalDate jour = e.getHorodatage().toLocalDate();
        return (estVide(c.module()) || e.getModule().equals(c.module()))
                && (c.resultat() == null || e.getResultat() == c.resultat())
                && (c.du() == null || !jour.isBefore(c.du()))
                && (c.au() == null || !jour.isAfter(c.au()))
                && correspond(e, c.texte());
    }

    /** @throws ValidationException if the period ends before it starts */
    public static void verifierPeriode(CritereAudit c) {
        if (c != null && c.du() != null && c.au() != null && c.au().isBefore(c.du())) {
            throw new ValidationException("La date de fin doit être après la date de début.");
        }
    }

    /** Module names present in the log, alphabetical: the choices of the module filter. */
    public List<String> modules() {
        return dao.findAll().stream().map(EvenementAudit::getModule).distinct().sorted().toList();
    }

    private static boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }

    /** Sizes of the audit_evenements columns: a long typed email never makes the audit insert fail. */
    private static String couper(String texte, int max) {
        return texte.length() <= max ? texte : texte.substring(0, max - 1) + "…";
    }
}
