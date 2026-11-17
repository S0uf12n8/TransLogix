
package ma.translogix.dto.comptabilite;

public class FactureDTO {

    private Long id;
    private String numero;
    private String client;
    private String dateEmission;
    private double montantHT;
    private double tva;
    private double montantTTC;
    private String statut;

    public FactureDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }

    public String getDateEmission() { return dateEmission; }
    public void setDateEmission(String dateEmission) { this.dateEmission = dateEmission; }

    public double getMontantHT() { return montantHT; }
    public void setMontantHT(double montantHT) { this.montantHT = montantHT; }

    public double getTva() { return tva; }
    public void setTva(double tva) { this.tva = tva; }

    public double getMontantTTC() { return montantTTC; }
    public void setMontantTTC(double montantTTC) { this.montantTTC = montantTTC; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
