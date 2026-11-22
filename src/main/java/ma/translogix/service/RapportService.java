package ma.translogix.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import ma.translogix.model.Client;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.Employe;
import ma.translogix.model.Facture;
import ma.translogix.model.Mission;
import ma.translogix.model.StatutConformite;
import ma.translogix.model.StatutFacture;
import ma.translogix.model.StatutMission;
import ma.translogix.model.Utilisateur;
import ma.translogix.model.Vehicule;
import ma.translogix.util.Formats;
import ma.translogix.util.ValidationException;

/**
 * Builds the dynamic reports of the "Rapports & analytics" page from the real services, filtered by
 * period, client and status (FiltreRapport). No SQL here: the data follows the DAOs plugged in AppContext.
 * Names (client, driver, vehicle) are resolved from one list read per report, never one lookup per row (no N+1).
 * Amounts are written "1234,50" (no thousands separator) so Excel reads them as numbers.
 */
public class RapportService {

    static final String EN_RETARD = "En retard";
    private static final String ACTIF = "Actif";
    private static final String INACTIF = "Inactif";
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FICHIER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final UtilisateurService utilisateurs;
    private final FactureService factures;
    private final MissionService missions;
    private final ClientService clients;
    private final VehiculeService vehicules;
    private final EmployeService employes;
    private final ConformiteService conformite;
    private final Clock horloge;

    public RapportService(UtilisateurService utilisateurs, FactureService factures, MissionService missions,
                          ClientService clients, VehiculeService vehicules, EmployeService employes,
                          ConformiteService conformite, Clock horloge) {
        this.utilisateurs = utilisateurs;
        this.factures = factures;
        this.missions = missions;
        this.clients = clients;
        this.vehicules = vehicules;
        this.employes = employes;
        this.conformite = conformite;
        this.horloge = horloge;
    }

    /** Clients offered in the client filter, by name. */
    public List<Client> clients() {
        return clients.lister();
    }

    /** Status labels offered in the status filter of this report. */
    public List<String> statuts(TypeRapport type) {
        return switch (type) {
            case FACTURATION -> List.of(StatutFacture.EN_ATTENTE.getLibelle(), EN_RETARD,
                    StatutFacture.PAYEE.getLibelle(), StatutFacture.ANNULEE.getLibelle());
            case MISSIONS -> Arrays.stream(StatutMission.values()).map(StatutMission::getLibelle).toList();
            case CONFORMITE -> List.of(StatutConformite.EXPIRE.getLibelle(), StatutConformite.A_RENOUVELER.getLibelle());
            case COMPTES -> List.of(ACTIF, INACTIF);
        };
    }

    /**
     * The report, filtered. Criteria a report does not use (TypeRapport.utiliseDates / utiliseClient) are ignored.
     * @throws ValidationException if the period ends before it starts
     */
    public Rapport generer(TypeRapport type, FiltreRapport filtre) {
        FiltreRapport f = filtre == null ? FiltreRapport.TOUT : filtre;
        if (f.du() != null && f.au() != null && f.au().isBefore(f.du())) {
            throw new ValidationException("La date de fin doit être après la date de début.");
        }
        f = new FiltreRapport(type.utiliseDates() ? f.du() : null, type.utiliseDates() ? f.au() : null,
                type.utiliseClient() ? f.clientId() : null, f.statut());
        return switch (type) {
            case FACTURATION -> facturation(f);
            case MISSIONS -> missions(f);
            case CONFORMITE -> conformite(f);
            case COMPTES -> comptes(f);
        };
    }

    // ------------------------------------------------------------------ invoices

    /** Invoices issued in the period. Totals leave out cancelled invoices. */
    private Rapport facturation(FiltreRapport f) {
        Map<Long, String> nomsClients = parId(clients.lister(), Client::getId, Client::getNom);
        Map<Long, String> refsMissions = parId(missions.lister(), Mission::getId, Mission::getReference);
        List<Facture> retenues = factures.lister().stream()
                .filter(x -> f.dansLaPeriode(x.getDateEmission()))
                .filter(x -> f.pourLeClient(x.getClientId()))
                .filter(x -> f.avecLeStatut(factures.libelleStatut(x)))
                .sorted(Comparator.comparing(Facture::getDateEmission).thenComparing(Facture::getNumero))
                .toList();
        List<List<String>> lignes = retenues.stream().map(x -> List.of(
                x.getNumero(),
                Formats.date(x.getDateEmission()),
                nomsClients.getOrDefault(x.getClientId(), ""),
                x.getMissionId() != null ? refsMissions.getOrDefault(x.getMissionId(), "")
                        : texte(x.getReferenceTrajet()),
                montant(x.getMontantHT()),
                montant(x.getMontantTva()),
                montant(x.getMontantTtc()),
                factures.libelleStatut(x))).toList();
        List<Facture> comptees = retenues.stream().filter(x -> x.getStatut() != StatutFacture.ANNULEE).toList();
        String resume = pluriel(retenues.size(), "facture", "factures")
                + " · Total HT " + Formats.mad(somme(comptees, Facture::getMontantHT))
                + " · Total TTC " + Formats.mad(somme(comptees, Facture::getMontantTtc))
                + (comptees.size() < retenues.size() ? " (hors factures annulées)" : "");
        return new Rapport("Facturation", nomFichier("facturation"),
                List.of("N° facture", "Date d'émission", "Client", "Mission", "Montant HT (MAD)", "TVA (MAD)",
                        "Montant TTC (MAD)", "Statut"),
                lignes, resume);
    }

    // ------------------------------------------------------------------ missions

    /** Missions starting in the period, in start order. */
    private Rapport missions(FiltreRapport f) {
        Map<Long, String> nomsClients = parId(clients.lister(), Client::getId, Client::getNom);
        Map<Long, String> chauffeurs = parId(employes.lister(), Employe::getId, Employe::getNomComplet);
        Map<Long, String> camions = parId(vehicules.lister(), Vehicule::getId, Vehicule::getMatricule);
        List<List<String>> lignes = missions.lister().stream()
                .filter(m -> f.dansLaPeriode(m.getDateDebut() == null ? null : m.getDateDebut().toLocalDate()))
                .filter(m -> f.pourLeClient(m.getClientId()))
                .filter(m -> f.avecLeStatut(m.getStatut().getLibelle()))
                .sorted(Comparator.comparing(Mission::getDateDebut).thenComparing(Mission::getReference))
                .map(m -> List.of(
                        m.getReference(),
                        nomsClients.getOrDefault(m.getClientId(), ""),
                        chauffeurs.getOrDefault(m.getChauffeurId(), ""),
                        camions.getOrDefault(m.getVehiculeId(), ""),
                        m.getVilleDepart(),
                        m.getVilleArrivee(),
                        m.getDateDebut().format(DATE_HEURE),
                        m.getDateFin().format(DATE_HEURE),
                        m.getPoidsTonnes() == null ? "" : decimal(m.getPoidsTonnes()),
                        m.getStatut().getLibelle()))
                .toList();
        return new Rapport("Missions", nomFichier("missions"),
                List.of("Référence", "Client", "Chauffeur", "Véhicule", "Départ", "Arrivée", "Début", "Fin",
                        "Poids (t)", "Statut"),
                lignes, pluriel(lignes.size(), "mission", "missions"));
    }

    // ------------------------------------------------------------------ RH

    /** Expired or soon-expiring documents of active employees, expiring in the period, most urgent first. */
    private Rapport conformite(FiltreRapport f) {
        Map<Long, String> noms = parId(employes.lister(), Employe::getId, Employe::getNomComplet);
        List<DocumentConformite> retenus = conformite.alertes().stream()
                .filter(d -> f.dansLaPeriode(d.getDateExpiration()))
                .filter(d -> f.avecLeStatut(conformite.statut(d).getLibelle()))
                .toList();
        List<List<String>> lignes = retenus.stream().map(d -> List.of(
                noms.getOrDefault(d.getEmployeId(), ""),
                d.getType().getLibelle(),
                texte(d.getNumero()),
                Formats.date(d.getDateExpiration()),
                String.valueOf(conformite.joursRestants(d)),
                conformite.statut(d).getLibelle())).toList();
        return new Rapport("Conformité RH", nomFichier("conformite-rh"),
                List.of("Employé", "Document", "Numéro", "Date d'expiration", "Jours restants", "Statut"),
                lignes, pluriel(lignes.size(), "document à traiter", "documents à traiter"));
    }

    /** Staff accounts, by name. */
    private Rapport comptes(FiltreRapport f) {
        List<List<String>> lignes = utilisateurs.lister().stream()
                .filter(u -> f.avecLeStatut(u.isActif() ? ACTIF : INACTIF))
                .map(RapportService::ligneCompte)
                .toList();
        return new Rapport("Comptes utilisateurs", nomFichier("comptes"),
                List.of("Nom", "Prénom", "Email", "Rôle", "Statut"),
                lignes, pluriel(lignes.size(), "compte", "comptes"));
    }

    private static List<String> ligneCompte(Utilisateur u) {
        return List.of(u.getNom(), u.getPrenom(), u.getEmail(), u.getRole().getLibelle(),
                u.isActif() ? ACTIF : INACTIF);
    }

    // ------------------------------------------------------------------ helpers

    private String nomFichier(String sujet) {
        return "rapport-" + sujet + "-" + LocalDate.now(horloge).format(DATE_FICHIER) + ".csv";
    }

    private static <T> Map<Long, String> parId(List<T> elements, Function<T, Long> id, Function<T, String> libelle) {
        return elements.stream().collect(Collectors.toMap(id, e -> texte(libelle.apply(e)), (a, b) -> a));
    }

    private static BigDecimal somme(List<Facture> liste, Function<Facture, BigDecimal> champ) {
        return liste.stream().map(champ).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** "1234,50": two decimals, comma, no thousands separator. */
    static String montant(BigDecimal valeur) {
        return valeur == null ? "" : valeur.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** "24" or "12,5": no useless zeros. */
    private static String decimal(BigDecimal valeur) {
        return valeur.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String texte(String valeur) {
        return valeur == null ? "" : valeur;
    }

    private static String pluriel(long nombre, String singulier, String plurielTexte) {
        return nombre + " " + (nombre > 1 ? plurielTexte : singulier);
    }
}
