
package ma.translogix.dto.comptabilite;

public class PaiementDTO {

    private Long id;
    private String reference;
    private String date;
    private double montant;
    private String mode;
    private String statut;

    public PaiementDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
