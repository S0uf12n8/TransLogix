
package ma.translogix.model.comptabilite;

public class Employe {

    private Long id;
    private String matricule;
    private String nom;
    private String poste;
    private double salaireBase;

    public Employe() {
    }

    public Employe(Long id, String matricule, String nom,
                   String poste, double salaireBase) {
        this.id = id;
        this.matricule = matricule;
        this.nom = nom;
        this.poste = poste;
        this.salaireBase = salaireBase;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPoste() { return poste; }
    public void setPoste(String poste) { this.poste = poste; }

    public double getSalaireBase() { return salaireBase; }
    public void setSalaireBase(double salaireBase) { this.salaireBase = salaireBase; }
}
