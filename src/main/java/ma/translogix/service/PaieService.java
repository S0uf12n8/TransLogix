package ma.translogix.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import ma.translogix.dao.PaieDao;
import ma.translogix.model.Employe;
import ma.translogix.model.ModePaiementSalaire;
import ma.translogix.model.Paie;
import ma.translogix.model.ResultatAudit;
import ma.translogix.model.StatutPaie;
import ma.translogix.util.Recherche;
import ma.translogix.util.ValidationException;

/**
 * Payroll rules of the "Paiement des employés" page. Salaries come from the accountant's input;
 * the only computed figure is the net (base + bonuses - deductions). Important operations are audited.
 */
public class PaieService {

    /** Search criteria of the payroll page; null or blank means "no filter". */
    public record CriterePaie(String texte, YearMonth periode, StatutPaie statut, Long employeId) {
        public static final CriterePaie AUCUN = new CriterePaie(null, null, null, null);
    }

    /** Totals of the currently displayed records, never invented figures. */
    public record SynthesePaie(BigDecimal totalNet, BigDecimal totalPaye, BigDecimal totalImpaye,
                               int nombreFiches, int nombrePayees, int nombreEmployes) {
    }

    private static final DateTimeFormatter NUMERO_MOIS = DateTimeFormatter.ofPattern("uuuuMM");
    private static final Comparator<Paie> RECENTES_D_ABORD =
            Comparator.comparing(Paie::getPeriode, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(p -> p.getNumero() == null ? "" : p.getNumero(), Comparator.reverseOrder());

    private final PaieDao dao;
    private final EmployeService employes;
    private final AuditService audit;
    private final Clock horloge;

    public PaieService(PaieDao dao, EmployeService employes, AuditService audit, Clock horloge) {
        this.dao = dao;
        this.employes = employes;
        this.audit = audit;
        this.horloge = horloge;
    }

    /** Active employees, the only people a new payroll record can be created for. */
    public List<Employe> employesActifs() {
        return employes.lister().stream().filter(Employe::isActif)
                .sorted(Comparator.comparing(Employe::getNom, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Employe::getPrenom, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<Paie> lister() {
        return dao.findAll().stream().sorted(RECENTES_D_ABORD).toList();
    }

    public Paie trouver(Long id) {
        return dao.findById(id).orElseThrow(() -> new ValidationException("Fiche de paie introuvable."));
    }

    /** All records matching every criterion, newest period first. */
    public List<Paie> rechercher(CriterePaie critere) {
        CriterePaie c = critere == null ? CriterePaie.AUCUN : critere;
        Map<Long, Employe> parId = employes.lister().stream()
                .collect(Collectors.toMap(Employe::getId, Function.identity(), (a, b) -> a));
        return dao.findAll().stream()
                .filter(p -> c.periode() == null || c.periode().equals(p.getPeriode()))
                .filter(p -> c.statut() == null || c.statut() == p.getStatut())
                .filter(p -> c.employeId() == null || c.employeId().equals(p.getEmployeId()))
                .filter(p -> correspond(p, parId.get(p.getEmployeId()), c.texte()))
                .sorted(RECENTES_D_ABORD)
                .toList();
    }

    private static boolean correspond(Paie p, Employe employe, String texte) {
        if (texte == null || texte.isBlank()) {
            return true;
        }
        String periode = p.getPeriode() == null ? "" : p.getPeriode().toString();
        String statut = p.getStatut() == null ? "" : p.getStatut().getLibelle();
        return Recherche.correspond(texte, p.getNumero(), periode, statut, p.getReference(),
                employe == null ? null : employe.getNomComplet(), employe == null ? null : employe.getMatricule());
    }

    /** Totals of the supplied list (the same list the page displays). */
    public SynthesePaie synthese(List<Paie> fiches) {
        List<Paie> retenues = fiches == null ? List.of() : fiches;
        BigDecimal totalNet = somme(retenues, Paie::getNet);
        BigDecimal totalPaye = somme(retenues.stream().filter(p -> p.getStatut() == StatutPaie.PAYEE).toList(),
                Paie::getNet);
        BigDecimal totalImpaye = somme(retenues.stream().filter(p -> p.getStatut() != StatutPaie.PAYEE).toList(),
                Paie::getNet);
        long payees = retenues.stream().filter(p -> p.getStatut() == StatutPaie.PAYEE).count();
        long salaries = retenues.stream().map(Paie::getEmployeId).filter(java.util.Objects::nonNull).distinct().count();
        return new SynthesePaie(arrondir(totalNet), arrondir(totalPaye), arrondir(totalImpaye),
                retenues.size(), (int) payees, (int) salaries);
    }

    /** Creates a record in "En attente". One record per employee and per month. */
    public Paie creer(Long employeId, YearMonth periode, BigDecimal salaireBase, BigDecimal primes,
                      BigDecimal deductions) {
        String action = "Création d'une fiche de paie";
        try {
            Employe employe = employes.trouver(employeId);
            if (!employe.isActif()) {
                throw new ValidationException("Impossible de créer une fiche pour un employé désactivé.");
            }
            if (periode == null) {
                throw new ValidationException("Choisissez le mois de la paie.");
            }
            YearMonth moisCourant = YearMonth.now(horloge);
            if (periode.isAfter(moisCourant)) {
                throw new ValidationException("Le mois de la paie ne peut pas être dans le futur.");
            }
            dao.findByEmployeEtPeriode(employeId, periode).ifPresent(p -> {
                throw new ValidationException("Une fiche existe déjà pour " + employe.getNomComplet()
                        + " sur " + periode + " (" + p.getNumero() + ").");
            });
            BigDecimal base = positif(salaireBase, "Le salaire de base");
            BigDecimal supplement = positifOuZero(primes, "Les primes");
            BigDecimal retenue = positifOuZero(deductions, "Les retenues");
            BigDecimal net = base.add(supplement).subtract(retenue);
            if (net.signum() <= 0) {
                throw new ValidationException("Le salaire net doit rester supérieur à 0 après retenues.");
            }
            Paie paie = new Paie(null, prochainNumero(periode), employeId, periode, arrondir(base),
                    arrondir(supplement), arrondir(retenue), arrondir(net), null, null, null,
                    StatutPaie.EN_ATTENTE);
            Paie enregistree = dao.save(paie);
            auditer(action + " " + enregistree.getNumero(), true);
            return enregistree;
        } catch (ValidationException e) {
            auditer(action, false);
            throw e;
        }
    }

    /** Validates a pending record. A paid record can never go back. */
    public void valider(Long id) {
        String action = "Validation d'une fiche de paie";
        try {
            Paie paie = trouver(id);
            if (paie.getStatut() != StatutPaie.EN_ATTENTE) {
                throw new ValidationException("Seule une fiche en attente peut être validée (" + paie.getNumero()
                        + " est « " + paie.getStatut().getLibelle() + " »).");
            }
            paie.setStatut(StatutPaie.VALIDEE);
            dao.update(paie);
            auditer(action + " " + paie.getNumero(), true);
        } catch (ValidationException e) {
            auditer(action, false);
            throw e;
        }
    }

    /** Records the salary payment. The record must have been validated first. */
    public void enregistrerPaiement(Long id, ModePaiementSalaire mode, String reference, LocalDate date) {
        String action = "Enregistrement du paiement d'une fiche de paie";
        try {
            Paie paie = trouver(id);
            if (paie.getStatut() != StatutPaie.VALIDEE) {
                throw new ValidationException("Validez d'abord la fiche " + paie.getNumero() + ".");
            }
            if (mode == null) {
                throw new ValidationException("Choisissez le mode de paiement du salaire.");
            }
            if (reference == null || reference.isBlank()) {
                throw new ValidationException("La référence du paiement est obligatoire.");
            }
            String referenceNormalisee = reference.trim();
            for (Paie autre : dao.findAll()) {
                if (!autre.getId().equals(id) && referenceNormalisee.equalsIgnoreCase(autre.getReference())) {
                    throw new ValidationException("Cette référence est déjà utilisée par la fiche "
                            + autre.getNumero() + ".");
                }
            }
            if (date == null) {
                throw new ValidationException("La date de paiement est obligatoire.");
            }
            if (date.isAfter(LocalDate.now(horloge))) {
                throw new ValidationException("La date de paiement ne peut pas être dans le futur.");
            }
            if (paie.getPeriode() != null && date.isBefore(paie.getPeriode().atDay(1))) {
                throw new ValidationException("Le paiement ne peut pas précéder le mois de la paie.");
            }
            paie.setStatut(StatutPaie.PAYEE);
            paie.setModePaiement(mode);
            paie.setReference(referenceNormalisee);
            paie.setDatePaiement(date);
            dao.update(paie);
            auditer(action + " " + paie.getNumero(), true);
        } catch (ValidationException e) {
            auditer(action, false);
            throw e;
        }
    }

    private String prochainNumero(YearMonth periode) {
        String prefixe = "PAI-" + periode.format(NUMERO_MOIS) + "-";
        int max = dao.findAll().stream()
                .map(Paie::getNumero)
                .filter(n -> n != null && n.startsWith(prefixe))
                .mapToInt(n -> numero(n.substring(prefixe.length())))
                .max()
                .orElse(0);
        return prefixe + String.format(Locale.ROOT, "%03d", max + 1);
    }

    private void auditer(String action, boolean reussi) {
        audit.enregistrer("Paie", action, reussi ? ResultatAudit.REUSSI : ResultatAudit.REFUSE);
    }

    private static BigDecimal positif(BigDecimal valeur, String libelle) {
        BigDecimal montant = arrondir(valeur, libelle);
        if (montant.signum() <= 0) {
            throw new ValidationException(libelle + " doit être supérieur à 0.");
        }
        return montant;
    }

    private static BigDecimal positifOuZero(BigDecimal valeur, String libelle) {
        BigDecimal montant = valeur == null ? BigDecimal.ZERO.setScale(2) : arrondir(valeur, libelle);
        if (montant.signum() < 0) {
            throw new ValidationException(libelle + " ne peut pas être négatif.");
        }
        return montant;
    }

    private static BigDecimal arrondir(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO.setScale(2) : valeur.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal arrondir(BigDecimal valeur, String libelle) {
        if (valeur == null) {
            throw new ValidationException(libelle + " est obligatoire.");
        }
        return valeur.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal somme(List<Paie> fiches, Function<Paie, BigDecimal> champ) {
        return fiches.stream().map(champ).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static int numero(String texte) {
        try {
            return Integer.parseInt(texte);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
