package ma.translogix.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import ma.translogix.dao.DocumentConformiteDao;
import ma.translogix.dao.EmployeDao;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.Employe;
import ma.translogix.model.StatutConformite;
import ma.translogix.model.TypeDocument;
import ma.translogix.util.ValidationException;

/**
 * Licences, FIMC cards and medical visits ([M4]). The status is computed from today (the injected Clock),
 * never stored: expired before today -> EXPIRE; expires within 30 days (today included) -> A_RENOUVELER;
 * later -> VALIDE.
 */
public class ConformiteService {

    /** A document expiring within this many days must be renewed. */
    public static final int SEUIL_RENOUVELLEMENT_JOURS = 30;

    private final DocumentConformiteDao dao;
    private final EmployeDao employes;
    private final Clock clock;

    public ConformiteService(DocumentConformiteDao dao, EmployeDao employes, Clock clock) {
        this.dao = dao;
        this.employes = employes;
        this.clock = clock;
    }

    public StatutConformite statut(DocumentConformite document) {
        return statut(document.getDateExpiration());
    }

    public StatutConformite statut(LocalDate dateExpiration) {
        LocalDate aujourdhui = LocalDate.now(clock);
        if (dateExpiration.isBefore(aujourdhui)) {
            return StatutConformite.EXPIRE;
        }
        if (!dateExpiration.isAfter(aujourdhui.plusDays(SEUIL_RENOUVELLEMENT_JOURS))) {
            return StatutConformite.A_RENOUVELER;
        }
        return StatutConformite.VALIDE;
    }

    /** Days left before expiry (0 = expires today, negative = already expired). */
    public long joursRestants(DocumentConformite document) {
        return ChronoUnit.DAYS.between(LocalDate.now(clock), document.getDateExpiration());
    }

    public List<DocumentConformite> documentsDe(Long employeId) {
        return dao.findByEmploye(employeId);
    }

    public Optional<DocumentConformite> document(Long employeId, TypeDocument type) {
        return dao.findByEmploye(employeId).stream().filter(d -> d.getType() == type).findFirst();
    }

    /** Worst status among the employee's documents; empty when the employee has none. */
    public Optional<StatutConformite> statutGlobal(Long employeId) {
        return dao.findByEmploye(employeId).stream().map(this::statut).min(Comparator.naturalOrder());
    }

    /**
     * Records a renewed document ("Renouveler" / "Mettre à jour document"): updates the employee's document
     * of that type, or creates it when the employee does not have one yet.
     */
    public DocumentConformite renouveler(Long employeId, TypeDocument type, String numero, String categorie,
                                         LocalDate dateExpiration) {
        if (employeId == null) {
            throw new ValidationException("Choisissez un employé.");
        }
        Employe employe = employes.findById(employeId)
                .orElseThrow(() -> new ValidationException("Employé introuvable."));
        if (!employe.isActif()) {
            throw new ValidationException(employe.getNomComplet() + " est désactivé : document non modifiable.");
        }
        if (type == null) {
            throw new ValidationException("Choisissez le document renouvelé.");
        }
        if (dateExpiration == null) {
            throw new ValidationException("La date d'expiration est obligatoire.");
        }
        if (type == TypeDocument.PERMIS && vide(numero)) {
            throw new ValidationException("Le numéro de permis est obligatoire.");
        }
        String numeroPropre = vide(numero) ? null : numero.trim();
        String categoriePropre = type != TypeDocument.PERMIS || vide(categorie) ? null : categorie.trim();

        Optional<DocumentConformite> existant = document(employeId, type);
        if (existant.isPresent()) {
            DocumentConformite d = existant.get();
            d.setNumero(numeroPropre);
            d.setCategorie(categoriePropre);
            d.setDateExpiration(dateExpiration);
            dao.update(d);
            return d;
        }
        return dao.save(new DocumentConformite(null, employeId, type, numeroPropre, categoriePropre, dateExpiration));
    }

    /** Documents of active employees that are EXPIRE or A_RENOUVELER, most urgent (earliest expiry) first. */
    public List<DocumentConformite> alertes() {
        return dao.findAll().stream()
                .filter(d -> employes.findById(d.getEmployeId()).map(Employe::isActif).orElse(false))
                .filter(d -> statut(d) != StatutConformite.VALIDE)
                .sorted(Comparator.comparing(DocumentConformite::getDateExpiration))
                .toList();
    }

    private static boolean vide(String texte) {
        return texte == null || texte.isBlank();
    }
}
