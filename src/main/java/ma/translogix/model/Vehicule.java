package ma.translogix.model;

import java.math.BigDecimal;

/** A truck of the fleet. The matricule is the plate (12345-A-6), stored upper-case and unique. */
public class Vehicule {
    private Long id;
    private String matricule;
    private String marque;
    private String modele;
    private BigDecimal capaciteTonnes;
    private StatutVehicule statut = StatutVehicule.EN_SERVICE;

    public Vehicule() {
    }

    public Vehicule(Long id, String matricule, String marque, String modele, BigDecimal capaciteTonnes,
                    StatutVehicule statut) {
        this.id = id;
        this.matricule = matricule;
        this.marque = marque;
        this.modele = modele;
        this.capaciteTonnes = capaciteTonnes;
        this.statut = statut;
    }

    /** "Volvo FH" (missing parts left out). */
    public String getDesignation() {
        String m = marque == null ? "" : marque;
        String d = modele == null ? "" : modele;
        return (m + " " + d).trim();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }

    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }

    public BigDecimal getCapaciteTonnes() { return capaciteTonnes; }
    public void setCapaciteTonnes(BigDecimal capaciteTonnes) { this.capaciteTonnes = capaciteTonnes; }

    public StatutVehicule getStatut() { return statut; }
    public void setStatut(StatutVehicule statut) { this.statut = statut; }
}
