package ma.translogix.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import ma.translogix.model.Client;
import ma.translogix.model.Facture;
import ma.translogix.model.ModePaiement;
import ma.translogix.model.Paiement;
import ma.translogix.model.StatutFacture;
import ma.translogix.util.ValidationException;

/**
 * Figures of the "Trésorerie &amp; bilan" page. Invoiced revenue and cash received are two different things:
 * an invoice is counted once when it is issued (TTC), and a payment is counted once when it is cashed in.
 * Adding both would double-count the same business. No SQL, no JavaFX here.
 */
public class TresorerieService {

    /** One filter combination of the page. null / blank means "no filter". */
    public record CritereTresorerie(LocalDate du, LocalDate au, Long clientId, ModePaiement mode) {
        public static final CritereTresorerie TOUT = new CritereTresorerie(null, null, null, null);
    }

    /** A cash inflow: one recorded client payment, valued at the paid invoice's TTC amount. */
    public record Encaissement(LocalDate date, String numeroFacture, Long clientId, String client,
                               ModePaiement mode, String reference, BigDecimal montantTtc) {
    }

    /** One line of the invoice-status summary (counts and TTC amounts). */
    public record LigneStatut(String statut, int nombre, BigDecimal montantTtc) {
    }

    /** One month of the comparison chart. The two series must never be added together. */
    public record MoisTresorerie(YearMonth mois, BigDecimal factureTtc, BigDecimal encaisse) {
    }

    /** Headline figures of the current selection. */
    public record Synthese(BigDecimal factureHt, BigDecimal tva, BigDecimal factureTtc,
                           BigDecimal encaisse, BigDecimal creancesTtc, BigDecimal tauxEncaissement,
                           int nombreFactures, int nombreEncaissements) {
    }

    /** Everything the page needs, computed from one read of each list. */
    public record Tresorerie(Synthese synthese, List<LigneStatut> statuts, List<MoisTresorerie> mois,
                             List<Encaissement> encaissements) {
    }

    private static final List<String> ORDRE_STATUTS = List.of(
            StatutFacture.PAYEE.getLibelle(), "En attente", "En retard", StatutFacture.ANNULEE.getLibelle());
    private static final int NOMBRE_MOIS_MAX = 24;

    private final FactureService factures;
    private final ClientService clients;
    private final Clock horloge;

    public TresorerieService(FactureService factures, ClientService clients, Clock horloge) {
        this.factures = factures;
        this.clients = clients;
        this.horloge = horloge;
    }

    /** Clients offered by the page's client filter, by name. */
    public List<Client> clients() {
        return clients.lister();
    }

    /** Computes all page figures. The period applies to invoice issue dates and cash-in dates. */
    public Tresorerie generer(CritereTresorerie critere) {
        CritereTresorerie c = critere == null ? CritereTresorerie.TOUT : critere;
        verifierPeriode(c);

        List<Facture> toutes = factures.lister();
        Map<Long, String> nomsClients = new LinkedHashMap<>();
        for (Client client : clients.lister()) {
            nomsClients.put(client.getId(), client.getNom());
        }
        Map<Long, Facture> facturesParId = new LinkedHashMap<>();
        for (Facture facture : toutes) {
            facturesParId.put(facture.getId(), facture);
        }

        // Revenue and status summary: invoices issued in the period, cancelled ones excluded from money.
        List<Facture> emises = toutes.stream()
                .filter(f -> f.getDateEmission() != null && dansLaPeriode(f.getDateEmission(), c))
                .filter(f -> c.clientId() == null || c.clientId().equals(f.getClientId()))
                .toList();
        List<Facture> comptables = emises.stream()
                .filter(f -> f.getStatut() != StatutFacture.ANNULEE)
                .toList();
        BigDecimal factureHt = somme(comptables, Facture::getMontantHT);
        BigDecimal tva = somme(comptables, Facture::getMontantTva);
        BigDecimal factureTtc = somme(comptables, Facture::getMontantTtc);
        BigDecimal creances = somme(comptables.stream()
                .filter(f -> f.getStatut() == StatutFacture.EN_ATTENTE).toList(), Facture::getMontantTtc);

        // Cash: one row per real payment. The invoice gives the client and the amount (never added to revenue).
        List<Encaissement> encaissements = new ArrayList<>();
        for (Paiement paiement : factures.paiements()) {
            Facture facture = paiement.getFactureId() == null ? null : facturesParId.get(paiement.getFactureId());
            if (facture == null || paiement.getDate() == null
                    || !dansLaPeriode(paiement.getDate(), c)
                    || !dansLaPeriode(facture.getDateEmission(), c)
                    || (c.clientId() != null && !c.clientId().equals(facture.getClientId()))
                    || (c.mode() != null && c.mode() != paiement.getMode())) {
                continue;
            }
            Long clientId = facture.getClientId();
            encaissements.add(new Encaissement(paiement.getDate(), facture.getNumero(), clientId,
                    nomsClients.getOrDefault(clientId, "Client inconnu"), paiement.getMode(),
                    paiement.getReference(), montant(facture.getMontantTtc())));
        }
        BigDecimal encaisse = encaissements.stream().map(Encaissement::montantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taux = factureTtc.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                : encaisse.multiply(BigDecimal.valueOf(100)).divide(factureTtc, 2, RoundingMode.HALF_UP);

        Synthese synthese = new Synthese(arrondir(factureHt), arrondir(tva), arrondir(factureTtc),
                arrondir(encaisse), arrondir(creances), taux, comptables.size(), encaissements.size());

        return new Tresorerie(synthese, resumesStatuts(emises), courbeMensuelle(emises, encaissements, c),
                List.copyOf(encaissements));
    }

    /** Status counts use every issued invoice, including cancelled ones (which have no amount in the KPIs). */
    private List<LigneStatut> resumesStatuts(List<Facture> emises) {
        Map<String, Integer> comptes = new LinkedHashMap<>();
        Map<String, BigDecimal> montants = new LinkedHashMap<>();
        for (String statut : ORDRE_STATUTS) {
            comptes.put(statut, 0);
            montants.put(statut, BigDecimal.ZERO.setScale(2));
        }
        for (Facture facture : emises) {
            String libelle = factures.libelleStatut(facture);
            comptes.merge(libelle, 1, Integer::sum);
            montants.merge(libelle, montant(facture.getMontantTtc()), BigDecimal::add);
        }
        List<LigneStatut> lignes = new ArrayList<>();
        for (String statut : ORDRE_STATUTS) {
            lignes.add(new LigneStatut(statut, comptes.get(statut), arrondir(montants.get(statut))));
        }
        return List.copyOf(lignes);
    }

    /** Months of the selected period (at most 24), with both real series, even when a month has no data. */
    private List<MoisTresorerie> courbeMensuelle(List<Facture> emises, List<Encaissement> encaissements,
                                                  CritereTresorerie c) {
        List<YearMonth> mois = moisDeLaPeriode(c);
        Map<YearMonth, BigDecimal> facturesParMois = new TreeMap<>();
        Map<YearMonth, BigDecimal> caissesParMois = new TreeMap<>();
        for (YearMonth moisCourant : mois) {
            facturesParMois.put(moisCourant, BigDecimal.ZERO.setScale(2));
            caissesParMois.put(moisCourant, BigDecimal.ZERO.setScale(2));
        }
        for (Facture facture : emises) {
            if (facture.getStatut() != StatutFacture.ANNULEE && facture.getDateEmission() != null) {
                YearMonth cle = YearMonth.from(facture.getDateEmission());
                if (facturesParMois.containsKey(cle)) {
                    facturesParMois.merge(cle, montant(facture.getMontantTtc()), BigDecimal::add);
                }
            }
        }
        for (Encaissement encaissement : encaissements) {
            YearMonth cle = YearMonth.from(encaissement.date());
            if (caissesParMois.containsKey(cle)) {
                caissesParMois.merge(cle, encaissement.montantTtc(), BigDecimal::add);
            }
        }
        List<MoisTresorerie> resultat = new ArrayList<>();
        for (YearMonth moisCourant : mois) {
            resultat.add(new MoisTresorerie(moisCourant, arrondir(facturesParMois.get(moisCourant)),
                    arrondir(caissesParMois.get(moisCourant))));
        }
        return List.copyOf(resultat);
    }

    private List<YearMonth> moisDeLaPeriode(CritereTresorerie c) {
        YearMonth fin;
        YearMonth debut;
        if (c.au() != null) {
            fin = YearMonth.from(c.au());
        } else if (c.du() != null) {
            fin = YearMonth.from(c.du());
        } else {
            fin = YearMonth.now(horloge);
        }
        if (c.du() != null) {
            debut = YearMonth.from(c.du());
        } else {
            debut = fin.minusMonths(NOMBRE_MOIS_MAX - 1L);
        }
        if (debut.until(fin, java.time.temporal.ChronoUnit.MONTHS) >= NOMBRE_MOIS_MAX) {
            debut = fin.minusMonths(NOMBRE_MOIS_MAX - 1L);
        }
        List<YearMonth> mois = new ArrayList<>();
        for (YearMonth courant = debut; !courant.isAfter(fin); courant = courant.plusMonths(1)) {
            mois.add(courant);
        }
        return mois;
    }

    /** True when the date is inside the selected period (bounds included). */
    public static boolean dansLaPeriode(LocalDate date, CritereTresorerie c) {
        if (date == null) {
            return false;
        }
        return (c.du() == null || !date.isBefore(c.du())) && (c.au() == null || !date.isAfter(c.au()));
    }

    private static void verifierPeriode(CritereTresorerie c) {
        if (c.du() != null && c.au() != null && c.au().isBefore(c.du())) {
            throw new ValidationException("La date de fin doit être après la date de début.");
        }
    }

    private static BigDecimal somme(List<Facture> factures, Function<Facture, BigDecimal> champ) {
        return factures.stream().map(champ).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal montant(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO.setScale(2) : valeur;
    }

    private static BigDecimal arrondir(BigDecimal valeur) {
        return valeur.setScale(2, RoundingMode.HALF_UP);
    }

    /** Sorted by date then reference, most recent first: used by the controller's payment history. */
    public static final Comparator<Encaissement> RECENTS_D_ABORD =
            Comparator.comparing(Encaissement::date, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(e -> e.reference() == null ? "" : e.reference(),
                            Comparator.reverseOrder());
}
