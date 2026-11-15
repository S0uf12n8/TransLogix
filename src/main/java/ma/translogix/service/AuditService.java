package ma.translogix.service;

import java.time.Clock;
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
        if (estVide(module) || estVide(action)) {
            throw new ValidationException("Le module et l'action de l'audit sont obligatoires.");
        }
        if (resultat == null) {
            throw new ValidationException("Le résultat de l'audit est obligatoire.");
        }
        String qui = acteur.get();
        EvenementAudit evenement = new EvenementAudit(null, LocalDateTime.now(horloge),
                estVide(qui) ? "Inconnu" : qui, module.trim(), action.trim(), resultat);
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

    private static boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
