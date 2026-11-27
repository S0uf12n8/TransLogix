package ma.translogix.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One expense reported by a driver for one mission. Amounts are entered from real receipts:
 * the application never invents an expense. Validation belongs to DepenseService.
 */
public class Depense {
    private Long id;
    private Long missionId;
    private Long chauffeurId;
    private TypeDepense type;
    private String station;
    private BigDecimal montant;
    /** Litres, only meaningful for fuel; null otherwise. */
    private BigDecimal litres;
    private LocalDate date;
    private StatutDepense statut = StatutDepense.EN_ATTENTE;
    /** Required when the accountant refuses the expense. */
    private String motifRefus;

    public Depense() {
    }

    public Depense(Long id, Long missionId, Long chauffeurId, TypeDepense type, String station,
                   BigDecimal montant, BigDecimal litres, LocalDate date, StatutDepense statut, String motifRefus) {
        this.id = id;
        this.missionId = missionId;
        this.chauffeurId = chauffeurId;
        this.type = type;
        this.station = station;
        this.montant = montant;
        this.litres = litres;
        this.date = date;
        this.statut = statut;
        this.motifRefus = motifRefus;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMissionId() { return missionId; }
    public void setMissionId(Long missionId) { this.missionId = missionId; }

    public Long getChauffeurId() { return chauffeurId; }
    public void setChauffeurId(Long chauffeurId) { this.chauffeurId = chauffeurId; }

    public TypeDepense getType() { return type; }
    public void setType(TypeDepense type) { this.type = type; }

    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public BigDecimal getLitres() { return litres; }
    public void setLitres(BigDecimal litres) { this.litres = litres; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public StatutDepense getStatut() { return statut; }
    public void setStatut(StatutDepense statut) { this.statut = statut; }

    public String getMotifRefus() { return motifRefus; }
    public void setMotifRefus(String motifRefus) { this.motifRefus = motifRefus; }
}
