package ma.translogix.model;

/** Lifecycle of a payroll record: created, validated by the accountant, then paid. */
public enum StatutPaie {
    EN_ATTENTE("En attente"),
    VALIDEE("Validée"),
    PAYEE("Payée");

    private final String libelle;

    StatutPaie(String libelle) {
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
