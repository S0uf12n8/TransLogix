
package ma.translogix.model.comptabilite;

public class LigneEcriture {

    private Long id;
    private Long ecritureId;
    private String numeroCompte;
    private String libelle;
    private double debit;
    private double credit;

    public LigneEcriture() {
    }

    public LigneEcriture(Long id, Long ecritureId, String numeroCompte,
                         String libelle, double debit, double credit) {
        this.id = id;
        this.ecritureId = ecritureId;
        this.numeroCompte = numeroCompte;
        this.libelle = libelle;
        this.debit = debit;
        this.credit = credit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEcritureId() { return ecritureId; }
    public void setEcritureId(Long ecritureId) { this.ecritureId = ecritureId; }

    public String getNumeroCompte() { return numeroCompte; }
    public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public double getDebit() { return debit; }
    public void setDebit(double debit) { this.debit = debit; }

    public double getCredit() { return credit; }
    public void setCredit(double credit) { this.credit = credit; }
}
