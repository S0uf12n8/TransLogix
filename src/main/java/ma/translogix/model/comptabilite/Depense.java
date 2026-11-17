package ma.translogix.model.comptabilite;

public class Depense {

    private Long id;
    private String date;
    private String categorie;
    private String description;
    private double montant;

    public Depense() {
    }

    public Depense(Long id, String date, String categorie,
                   String description, double montant) {
        this.id = id;
        this.date = date;
        this.categorie = categorie;
        this.description = description;
        this.montant = montant;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }
}
