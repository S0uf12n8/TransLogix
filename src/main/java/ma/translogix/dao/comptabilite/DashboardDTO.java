
package ma.translogix.dto.comptabilite;

public class DashboardDTO {

    private double chiffreAffairesHT;
    private double encaissements;
    private double creances;
    private double charges;
    private double tresorerie;
    private double resultatNet;

    public DashboardDTO() {
    }

    public double getChiffreAffairesHT() {
        return chiffreAffairesHT;
    }

    public void setChiffreAffairesHT(double chiffreAffairesHT) {
        this.chiffreAffairesHT = chiffreAffairesHT;
    }

    public double getEncaissements() {
        return encaissements;
    }

    public void setEncaissements(double encaissements) {
        this.encaissements = encaissements;
    }

    public double getCreances() {
        return creances;
    }

    public void setCreances(double creances) {
        this.creances = creances;
    }

    public double getCharges() {
        return charges;
    }

    public void setCharges(double charges) {
        this.charges = charges;
    }

    public double getTresorerie() {
        return tresorerie;
    }

    public void setTresorerie(double tresorerie) {
        this.tresorerie = tresorerie;
    }

    public double getResultatNet() {
        return resultatNet;
    }

    public void setResultatNet(double resultatNet) {
        this.resultatNet = resultatNet;
    }
}
