package ma.translogix.model;

/** How a client paid an invoice (options of the "Enregistrer règlement" dialog in contable.html). */
public enum ModePaiement {
    VIREMENT("Virement bancaire"),
    CHEQUE("Chèque bancaire"),
    EFFET_DE_COMMERCE("Effet de commerce");

    private final String libelle;

    ModePaiement(String libelle) {
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
