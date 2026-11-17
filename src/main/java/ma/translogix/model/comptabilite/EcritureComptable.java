
package ma.translogix.model.comptabilite;

public class EcritureComptable {

    private Long id;
    private String date;
    private String journal;
    private String reference;
    private String libelle;

    public EcritureComptable() {
    }

    public EcritureComptable(Long id, String date, String journal,
                             String reference, String libelle) {
        this.id = id;
        this.date = date;
        this.journal = journal;
        this.reference = reference;
        this.libelle = libelle;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getJournal() { return journal; }
    public void setJournal(String journal) { this.journal = journal; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}
