
package ma.translogix.model.comptabilite;

public class DetteFournisseur {

    private Long id;
    private Long fournisseurId;
    private String reference;
    private String dateEcheance;
    private double montant;
    private String statut;

    public DetteFournisseur() {
    }

    public DetteFournisseur(Long id, Long fournisseurId,
                            String reference, String dateEcheance,
                            double montant, String statut) {
        this.id = id;
        this.fournisseurId = fournisseurId;
        this.reference = reference;
        this.dateEcheance = dateEcheance;
        this.montant = montant;
        this.statut = statut;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(String dateEcheance) { this.dateEcheance = dateEcheance; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
