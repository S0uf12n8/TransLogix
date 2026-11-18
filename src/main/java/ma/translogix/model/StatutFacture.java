package ma.translogix.model;

/**
 * Stored status of an invoice. "En retard" is NOT a status: it is computed
 * (FactureService.estEnRetard) from the due date, so it can never be out of date.
 */
public enum StatutFacture {
    EN_ATTENTE("En attente"),
    PAYEE("Payée"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutFacture(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
