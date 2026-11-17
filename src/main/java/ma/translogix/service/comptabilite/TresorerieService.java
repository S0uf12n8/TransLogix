
package ma.translogix.service.comptabilite;

import ma.translogix.model.comptabilite.Transaction;
import java.util.List;

public class TresorerieService {

    public List<Transaction> obtenirTransactions() {
        throw new UnsupportedOperationException("API not connected yet.");
    }

    public double calculerSolde(double soldeInitial,
                                List<Transaction> transactions) {

        double solde = soldeInitial;

        for (Transaction transaction : transactions) {
            solde += transaction.getEntree();
            solde -= transaction.getSortie();
        }

        return solde;
    }
}
