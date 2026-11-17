
package ma.translogix.model.comptabilite;

public class Facture {

    private Long id;
    private String numero;
    private Long clientId;
    private String dateEmission;
    private String dateEcheance;
    private double montantHT;
    private double tauxTVA;
    private String statut;

    public Facture() {
    }

    public Facture(Long id, String numero, Long clientId,
                   String dateEmission, String dateEcheance,
                   double montantHT, double tauxTVA, String statut) {
        this.id = id;
        this.numero = numero;
        this.clientId = clientId;
        this.dateEmission = dateEmission;
        this.dateEcheance = dateEcheance;
        this.montantHT = montantHT;
        this.tauxTVA = tauxTVA;
        this.statut = statut;
    }

    public double calculerTVA() {
        return montantHT * tauxTVA / 100;
    }

    public double calculerTTC() {
        return montantHT + calculerTVA();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getDateEmission() { return dateEmission; }
    public void setDateEmission(String dateEmission) { this.dateEmission = dateEmission; }

    public String getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(String dateEcheance) { this.dateEcheance = dateEcheance; }

    public double getMontantHT() { return montantHT; }
    public void setMontantHT(double montantHT) { this.montantHT = montantHT; }

    public double getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(double tauxTVA) { this.tauxTVA = tauxTVA; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
