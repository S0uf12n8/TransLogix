
package ma.translogix.model.comptabilite;

public class Transaction {

    private Long id;
    private String date;
    private String libelle;
    private double entree;
    private double sortie;
    private double solde;

    public Transaction() {
    }

    public Transaction(Long id, String date, String libelle,
                       double entree, double sortie, double solde) {
        this.id = id;
        this.date = date;
        this.libelle = libelle;
        this.entree = entree;
        this.sortie = sortie;
        this.solde = solde;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public double getEntree() { return entree; }
    public void setEntree(double entree) { this.entree = entree; }

    public double getSortie() { return sortie; }
    public void setSortie(double sortie) { this.sortie = sortie; }

    public double getSolde() { return solde; }
    public void setSolde(double solde) { this.solde = solde; }
}
