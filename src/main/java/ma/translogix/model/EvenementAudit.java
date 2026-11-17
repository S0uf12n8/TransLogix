package ma.translogix.model;

import java.time.LocalDateTime;

/** One line of the audit log. Immutable once recorded, except for the id set by the DAO. */
public class EvenementAudit {
    private Long id;
    private final LocalDateTime horodatage;
    private final String utilisateur;
    private final String module;
    private final String action;
    private final ResultatAudit resultat;

    public EvenementAudit(Long id, LocalDateTime horodatage, String utilisateur,
                          String module, String action, ResultatAudit resultat) {
        this.id = id;
        this.horodatage = horodatage;
        this.utilisateur = utilisateur;
        this.module = module;
        this.action = action;
        this.resultat = resultat;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getHorodatage() { return horodatage; }
    public String getUtilisateur() { return utilisateur; }
    public String getModule() { return module; }
    public String getAction() { return action; }
    public ResultatAudit getResultat() { return resultat; }
}
