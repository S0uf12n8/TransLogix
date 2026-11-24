package ma.translogix.model;

import java.time.LocalDate;

/**
 * One leave request of an employee ([M3], iteration 3). employeId is an Employe id, not a Utilisateur id.
 * nbJours is computed by CongeService from the period (working days), never typed by the user.
 */
public class DemandeConge {
    private Long id;
    private Long employeId;
    private TypeConge type;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private long nbJours;
    private StatutConge statut = StatutConge.EN_ATTENTE;
    private String motif;
    private LocalDate dateDemande;

    public DemandeConge() {
    }

    public DemandeConge(Long id, Long employeId, TypeConge type, LocalDate dateDebut, LocalDate dateFin,
                        long nbJours, StatutConge statut, String motif, LocalDate dateDemande) {
        this.id = id;
        this.employeId = employeId;
        this.type = type;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nbJours = nbJours;
        this.statut = statut;
        this.motif = motif;
        this.dateDemande = dateDemande;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeId() { return employeId; }
    public void setEmployeId(Long employeId) { this.employeId = employeId; }

    public TypeConge getType() { return type; }
    public void setType(TypeConge type) { this.type = type; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public long getNbJours() { return nbJours; }
    public void setNbJours(long nbJours) { this.nbJours = nbJours; }

    public StatutConge getStatut() { return statut; }
    public void setStatut(StatutConge statut) { this.statut = statut; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public LocalDate getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDate dateDemande) { this.dateDemande = dateDemande; }
}
