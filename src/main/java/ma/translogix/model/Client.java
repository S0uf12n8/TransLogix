package ma.translogix.model;

/** A customer company that orders transports and receives invoices. Issue [C2]. */
public class Client {
    /** Default payment delay in days ("Paiement à 30 jours"). */
    public static final int DELAI_PAR_DEFAUT = 30;

    private Long id;
    private String nom;
    /** Moroccan company identifier (15 digits). null when unknown. */
    private String ice;
    private String telephone;
    private String email;
    private String adresse;
    private int delaiPaiementJours = DELAI_PAR_DEFAUT;

    public Client() {
    }

    public Client(Long id, String nom, String ice, String telephone, String email,
                  String adresse, int delaiPaiementJours) {
        this.id = id;
        this.nom = nom;
        this.ice = ice;
        this.telephone = telephone;
        this.email = email;
        this.adresse = adresse;
        this.delaiPaiementJours = delaiPaiementJours;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public int getDelaiPaiementJours() { return delaiPaiementJours; }
    public void setDelaiPaiementJours(int delaiPaiementJours) { this.delaiPaiementJours = delaiPaiementJours; }

    /** Shown in the client ComboBox of the invoice dialog. */
    @Override
    public String toString() {
        return nom;
    }
}
