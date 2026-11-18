package ma.translogix.model;

import java.time.LocalDate;

/** Payment received for one invoice (full payment: partial payments are out of Iteration 2). */
public class Paiement {
    private Long id;
    private Long factureId;
    private ModePaiement mode;
    /** Transfer or cheque number, e.g. VIR-882910. */
    private String reference;
    private LocalDate date;

    public Paiement() {
    }

    public Paiement(Long id, Long factureId, ModePaiement mode, String reference, LocalDate date) {
        this.id = id;
        this.factureId = factureId;
        this.mode = mode;
        this.reference = reference;
        this.date = date;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFactureId() { return factureId; }
    public void setFactureId(Long factureId) { this.factureId = factureId; }

    public ModePaiement getMode() { return mode; }
    public void setMode(ModePaiement mode) { this.mode = mode; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
