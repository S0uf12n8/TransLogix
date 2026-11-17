
package ma.translogix.model.comptabilite;

public class Paiement {

    private Long id;
    private Long factureId;
    private String datePaiement;
    private double montant;
    private String modePaiement;

    public Paiement() {
    }

    public Paiement(Long id, Long factureId, String datePaiement,
                    double montant, String modePaiement) {
        this.id = id;
        this.factureId = factureId;
        this.datePaiement = datePaiement;
        this.montant = montant;
        this.modePaiement = modePaiement;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFactureId() { return factureId; }
    public void setFactureId(Long factureId) { this.factureId = factureId; }

    public String getDatePaiement() { return datePaiement; }
    public void setDatePaiement(String datePaiement) { this.datePaiement = datePaiement; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }
}
