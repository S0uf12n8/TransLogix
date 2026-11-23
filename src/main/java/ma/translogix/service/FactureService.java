package ma.translogix.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ma.translogix.dao.ClientDao;
import ma.translogix.dao.FactureDao;
import ma.translogix.dao.PaiementDao;
import ma.translogix.model.Client;
import ma.translogix.model.Facture;
import ma.translogix.model.Mission;
import ma.translogix.model.ModePaiement;
import ma.translogix.model.Paiement;
import ma.translogix.model.StatutFacture;
import ma.translogix.model.StatutMission;
import ma.translogix.util.ValidationException;

/**
 * Business rules for invoices and payments. Issue [C3]. No JavaFX and no SQL here.
 * <ul>
 *   <li>Only a LIVREE mission is invoiced, once (cancelled invoice included); the client comes from the mission,
 *       and the mission then moves to FACTUREE through MissionService (promise 2).</li>
 *   <li>VAT 14 %, HALF_UP, 2 decimals; TTC = HT + VAT.</li>
 *   <li>Due date = emission date + payment delay (the client's delay unless another condition is chosen).</li>
 *   <li>"En retard" is computed (unpaid and past due), never stored.</li>
 *   <li>A payment sets PAYEE; a cancelled or already paid invoice cannot be paid.</li>
 * </ul>
 */
public class FactureService {
    /** Moroccan VAT rate on road transport, in percent. */
    public static final BigDecimal TAUX_TVA = new BigDecimal("14.00");

    private static final BigDecimal CENT = new BigDecimal("100");
    private static final Pattern NUMERO = Pattern.compile("FACT-(\\d{4})-(\\d{3,})");
    private static final Comparator<Facture> RECENTES_D_ABORD =
            Comparator.comparing(Facture::getDateEmission).thenComparing(Facture::getId).reversed();

    /** Figures of the comptable dashboard, all computed from the stored invoices and payments. */
    public record Synthese(BigDecimal chiffreAffairesMoisHT, int facturesDuMois, BigDecimal factureMoisTtc,
                           BigDecimal encaissementsMois, BigDecimal creancesEnRetard, int facturesEnRetard) {
    }

    private final FactureDao dao;
    private final ClientDao clientDao;
    private final PaiementDao paiementDao;
    private final MissionService missions;
    private final Clock horloge;

    public FactureService(FactureDao dao, ClientDao clientDao, PaiementDao paiementDao, MissionService missions,
                          Clock horloge) {
        this.dao = dao;
        this.clientDao = clientDao;
        this.paiementDao = paiementDao;
        this.missions = missions;
        this.horloge = horloge;
    }

    /** VAT of an amount excluding tax, at 14 %, rounded HALF_UP to 2 decimals. */
    public static BigDecimal calculerTva(BigDecimal montantHT) {
        return arrondir(montantHT).multiply(TAUX_TVA).divide(CENT, 2, RoundingMode.HALF_UP);
    }

    /** HT + VAT, 2 decimals. */
    public static BigDecimal calculerTtc(BigDecimal montantHT) {
        return arrondir(montantHT).add(calculerTva(montantHT));
    }

    /** All invoices, newest first. */
    public List<Facture> lister() {
        return dao.findAll().stream().sorted(RECENTES_D_ABORD).toList();
    }

    public List<Facture> dernieres(int n) {
        return lister().stream().limit(n).toList();
    }

    public Facture trouver(Long id) {
        return dao.findById(id).orElseThrow(() -> new ValidationException("Facture introuvable."));
    }

    /** Delivered missions without an invoice yet: what the "Créer une facture" dialog offers. */
    public List<Mission> missionsAFacturer() {
        return missions.lister().stream()
                .filter(m -> m.getStatut() == StatutMission.LIVREE)
                .filter(m -> dao.findByMission(m.getId()).isEmpty())
                .toList();
    }

    /** Invoices a delivered mission, dated today. delaiJours null means "the client's payment delay". */
    public Facture creer(Long missionId, BigDecimal montantHT, Integer delaiJours) {
        return creer(missionId, montantHT, delaiJours, aujourdhui());
    }

    /** Same, with an explicit emission date (demo seed data). */
    public Facture creer(Long missionId, BigDecimal montantHT, Integer delaiJours, LocalDate dateEmission) {
        if (missionId == null) {
            throw new ValidationException("Choisissez une mission livrée.");
        }
        Mission mission = missions.trouver(missionId);
        dao.findByMission(missionId).ifPresent(f -> {
            throw new ValidationException("La mission " + mission.getReference() + " est déjà facturée ("
                    + f.getNumero() + ").");
        });
        if (mission.getStatut() != StatutMission.LIVREE) {
            throw new ValidationException("Seule une mission livrée peut être facturée (" + mission.getReference()
                    + " est « " + mission.getStatut().getLibelle() + " »).");
        }
        Client client = clientDao.findById(mission.getClientId())
                .orElseThrow(() -> new ValidationException("Client introuvable."));
        if (montantHT == null || montantHT.signum() <= 0) {
            throw new ValidationException("Le montant HT doit être supérieur à 0.");
        }
        int delai = delaiJours == null ? client.getDelaiPaiementJours() : delaiJours;
        if (delai < 0) {
            throw new ValidationException("Le délai de paiement ne peut pas être négatif.");
        }
        Facture facture = new Facture();
        facture.setNumero(prochainNumero(dateEmission.getYear()));
        facture.setClientId(client.getId());
        facture.setMissionId(mission.getId());
        facture.setReferenceTrajet(mission.getReference());
        facture.setMontantHT(arrondir(montantHT));
        facture.setTauxTva(TAUX_TVA);
        facture.setMontantTva(calculerTva(montantHT));
        facture.setMontantTtc(calculerTtc(montantHT));
        facture.setDateEmission(dateEmission);
        facture.setDateEcheance(dateEmission.plusDays(delai));
        facture.setStatut(StatutFacture.EN_ATTENTE);
        // JDBC ([C5]): the insert and the mission update must run in ONE transaction.
        Facture enregistree = dao.save(facture);
        missions.marquerFacturee(mission.getId());
        return enregistree;
    }

    /** Records the client's payment; the invoice becomes PAYEE. */
    public Paiement enregistrerPaiement(Long factureId, ModePaiement mode, String reference, LocalDate date) {
        Facture facture = trouver(factureId);
        if (facture.getStatut() == StatutFacture.ANNULEE) {
            throw new ValidationException("Une facture annulée ne peut pas être réglée.");
        }
        if (facture.getStatut() == StatutFacture.PAYEE) {
            throw new ValidationException("La facture " + facture.getNumero() + " est déjà réglée.");
        }
        if (mode == null) {
            throw new ValidationException("Choisissez le mode de paiement.");
        }
        if (reference == null || reference.isBlank()) {
            throw new ValidationException("Le numéro de pièce est obligatoire.");
        }
        if (date == null) {
            throw new ValidationException("La date d'encaissement est obligatoire.");
        }
        if (date.isAfter(aujourdhui())) {
            throw new ValidationException("La date d'encaissement ne peut pas être dans le futur.");
        }
        if (date.isBefore(facture.getDateEmission())) {
            throw new ValidationException("La date d'encaissement est antérieure à la date d'émission.");
        }
        Paiement paiement = paiementDao.save(new Paiement(null, factureId, mode, reference.trim(), date));
        facture.setStatut(StatutFacture.PAYEE);
        dao.update(facture);
        return paiement;
    }

    /** Cancels an unpaid invoice (kept for history, never deleted). */
    public void annuler(Long factureId) {
        Facture facture = trouver(factureId);
        if (facture.getStatut() == StatutFacture.PAYEE) {
            throw new ValidationException("Une facture réglée ne peut pas être annulée.");
        }
        facture.setStatut(StatutFacture.ANNULEE);
        dao.update(facture);
    }

    /** Payments recorded for one invoice, oldest first. Read-only: a payment is never edited or deleted. */
    public List<Paiement> paiementsDe(Long factureId) {
        return paiementDao.findByFacture(factureId);
    }

    /** Unpaid and past its due date. Computed every time, never stored. */
    public boolean estEnRetard(Facture facture) {
        return facture.getStatut() == StatutFacture.EN_ATTENTE && facture.getDateEcheance().isBefore(aujourdhui());
    }

    /** Status shown to the user: "En retard" for an overdue EN_ATTENTE invoice, otherwise the stored status. */
    public String libelleStatut(Facture facture) {
        return estEnRetard(facture) ? "En retard" : facture.getStatut().getLibelle();
    }

    public Synthese synthese() {
        YearMonth mois = YearMonth.from(aujourdhui());
        List<Facture> toutes = dao.findAll();
        List<Facture> duMois = toutes.stream()
                .filter(f -> f.getStatut() != StatutFacture.ANNULEE)
                .filter(f -> YearMonth.from(f.getDateEmission()).equals(mois))
                .toList();
        List<Facture> enRetard = toutes.stream().filter(this::estEnRetard).toList();
        BigDecimal encaisse = paiementDao.findAll().stream()
                .filter(p -> YearMonth.from(p.getDate()).equals(mois))
                .map(p -> trouver(p.getFactureId()).getMontantTtc())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Synthese(
                somme(duMois, Facture::getMontantHT), duMois.size(), somme(duMois, Facture::getMontantTtc),
                encaisse, somme(enRetard, Facture::getMontantTtc), enRetard.size());
    }

    /** FACT-YYYY-NNN: highest number of that year + 1. */
    private String prochainNumero(int annee) {
        int max = 0;
        for (Facture f : dao.findAll()) {
            Matcher m = NUMERO.matcher(f.getNumero());
            if (m.matches() && Integer.parseInt(m.group(1)) == annee) {
                max = Math.max(max, Integer.parseInt(m.group(2)));
            }
        }
        return String.format("FACT-%d-%03d", annee, max + 1);
    }

    private LocalDate aujourdhui() {
        return LocalDate.now(horloge);
    }

    private static BigDecimal arrondir(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal somme(List<Facture> factures, Function<Facture, BigDecimal> champ) {
        return factures.stream().map(champ).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
