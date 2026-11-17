
package ma.translogix.service.comptabilite;

import ma.translogix.model.comptabilite.BulletinPaie;
import java.util.List;

public class PaieService {

    public List<BulletinPaie> obtenirTousLesBulletins() {
        throw new UnsupportedOperationException("API not connected yet.");
    }

    public BulletinPaie creerBulletin(BulletinPaie bulletin) {
        throw new UnsupportedOperationException("API not connected yet.");
    }

    public double calculerTotalSalaires(List<BulletinPaie> bulletins) {

        return bulletins.stream()
            .mapToDouble(BulletinPaie::getNetAPayer)
            .sum();
    }
}
