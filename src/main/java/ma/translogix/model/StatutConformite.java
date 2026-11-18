package ma.translogix.model;

/**
 * Status of a compliance document, computed by ConformiteService from today's date. Never stored.
 * Declared from the most to the least urgent, so the enum order gives the worst status.
 */
public enum StatutConformite {
    EXPIRE("Expiré"),
    A_RENOUVELER("À renouveler"),
    VALIDE("Conforme");

    private final String libelle;

    StatutConformite(String libelle) {
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
