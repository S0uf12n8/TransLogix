
package ma.translogix.service.comptabilite;

public class TVAService {

    public double calculerTVACollectee(double montantHT, double taux) {

        if (montantHT < 0 || taux < 0) {
            throw new IllegalArgumentException("Valeurs négatives interdites.");
        }

        return montantHT * taux / 100;
    }

    public double calculerTVADeductible(double montantHT, double taux) {

        if (montantHT < 0 || taux < 0) {
            throw new IllegalArgumentException("Valeurs négatives interdites.");
        }

        return montantHT * taux / 100;
    }

    public double calculerSoldeTVA(double tvaCollectee,
                                   double tvaDeductible) {
        return tvaCollectee - tvaDeductible;
    }
}
