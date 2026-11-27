package ma.translogix.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import ma.translogix.dao.EmployeDao;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;
import ma.translogix.model.TypeContrat;
import ma.translogix.util.ValidationException;

/**
 * Staff register rules ([M2], [M3]). No JavaFX and no SQL here.
 * An employee is never deleted: "Désactiver" keeps the history (missions, documents) and hides
 * the employee from the active lists.
 */
public class EmployeService {

    private final EmployeDao dao;
    private final Clock clock;

    public EmployeService(EmployeDao dao, Clock clock) {
        this.dao = dao;
        this.clock = clock;
    }

    /** Head-count figures for the RH dashboard and the staff page (active employees only, except "chauffeurs"). */
    public record Effectifs(int actifs, int chauffeursActifs, int chauffeurs, int administratifEtDepot,
                            int cdi, int cdd, int essai, int recrutementsDuMois) {
    }

    /** One slice of the head-count pie chart, in {@link Poste} order. */
    public record Part(String libelle, long nombre) {
    }

    /** Every employee, active or not, in creation order. */
    public List<Employe> lister() {
        return dao.findAll();
    }

    /** Active drivers, by name: the people the dispatcher can assign and RH must keep compliant. */
    public List<Employe> listerChauffeurs() {
        return dao.findByPoste(Poste.CHAUFFEUR).stream()
                .filter(Employe::isActif)
                .sorted(Comparator.comparing(Employe::getNom).thenComparing(Employe::getPrenom))
                .toList();
    }

    public List<Employe> listerParPoste(Poste poste) {
        return dao.findByPoste(poste);
    }

    public Employe trouver(Long id) {
        return dao.findById(id).orElseThrow(() -> new ValidationException("Employé introuvable."));
    }

    /** The employee linked to a login account (the driver's own page uses it). */
    public Optional<Employe> trouverParUtilisateur(Long utilisateurId) {
        return dao.findByUtilisateurId(utilisateurId);
    }

    public Employe creer(String nom, String prenom, String cin, String cnss, Poste poste,
                         TypeContrat typeContrat, LocalDate dateEmbauche) {
        String cinNormalise = valider(nom, prenom, cin, cnss, poste, typeContrat, dateEmbauche);
        if (dao.findByCin(cinNormalise).isPresent()) {
            throw new ValidationException("Ce CIN est déjà enregistré pour un autre employé.");
        }
        Employe employe = new Employe(null, nouveauMatricule(dateEmbauche.getYear()), nom.trim(), prenom.trim(),
                cinNormalise, vide(cnss) ? null : cnss.trim(), poste, typeContrat, dateEmbauche, true, null);
        return dao.save(employe);
    }

    /** Changes the employee's details. The matricule never changes. */
    public void modifier(Long id, String nom, String prenom, String cin, String cnss, Poste poste,
                         TypeContrat typeContrat, LocalDate dateEmbauche) {
        Employe employe = trouver(id);
        String cinNormalise = valider(nom, prenom, cin, cnss, poste, typeContrat, dateEmbauche);
        dao.findByCin(cinNormalise)
                .filter(autre -> !autre.getId().equals(id))
                .ifPresent(autre -> {
                    throw new ValidationException("Ce CIN est déjà enregistré pour un autre employé.");
                });
        employe.setNom(nom.trim());
        employe.setPrenom(prenom.trim());
        employe.setCin(cinNormalise);
        employe.setCnss(vide(cnss) ? null : cnss.trim());
        employe.setPoste(poste);
        employe.setTypeContrat(typeContrat);
        employe.setDateEmbauche(dateEmbauche);
        dao.update(employe);
    }

    /** "Désactiver": the employee leaves the active lists; nothing is deleted. */
    public void desactiver(Long id) {
        Employe employe = trouver(id);
        if (!employe.isActif()) {
            throw new ValidationException("Cet employé est déjà désactivé.");
        }
        employe.setActif(false);
        dao.update(employe);
    }

    /** Links a login account to an employee (one account = at most one employee). */
    public void lierCompte(Long employeId, Long utilisateurId) {
        Employe employe = trouver(employeId);
        dao.findByUtilisateurId(utilisateurId)
                .filter(autre -> !autre.getId().equals(employeId))
                .ifPresent(autre -> {
                    throw new ValidationException("Ce compte est déjà lié à " + autre.getNomComplet() + ".");
                });
        employe.setUtilisateurId(utilisateurId);
        dao.update(employe);
    }

    public Effectifs effectifs() {
        YearMonth moisCourant = YearMonth.now(clock);
        List<Employe> tous = dao.findAll();
        List<Employe> actifs = tous.stream().filter(Employe::isActif).toList();
        int chauffeurs = (int) tous.stream().filter(e -> e.getPoste() == Poste.CHAUFFEUR).count();
        int chauffeursActifs = (int) actifs.stream().filter(e -> e.getPoste() == Poste.CHAUFFEUR).count();
        return new Effectifs(
                actifs.size(),
                chauffeursActifs,
                chauffeurs,
                actifs.size() - chauffeursActifs,
                compter(actifs, TypeContrat.CDI),
                compter(actifs, TypeContrat.CDD),
                compter(actifs, TypeContrat.ESSAI),
                (int) actifs.stream().filter(e -> YearMonth.from(e.getDateEmbauche()).equals(moisCourant)).count());
    }

    /** Active employees per post, in enum order, with zero for posts that have nobody. */
    public List<Part> employesParPoste() {
        List<Employe> actifs = dao.findAll().stream().filter(Employe::isActif).toList();
        return Arrays.stream(Poste.values())
                .map(poste -> new Part(poste.getLibelle(),
                        actifs.stream().filter(e -> e.getPoste() == poste).count()))
                .toList();
    }

    /** MAT-YYYY-NN: YYYY = hiring year, NN = next free number for that year (2 digits minimum). */
    private String nouveauMatricule(int annee) {
        String prefixe = "MAT-" + annee + "-";
        int dernier = dao.findAll().stream()
                .map(Employe::getMatricule)
                .filter(m -> m != null && m.startsWith(prefixe))
                .mapToInt(m -> numero(m.substring(prefixe.length())))
                .max()
                .orElse(0);
        String matricule;
        do {
            dernier++;
            matricule = prefixe + String.format("%02d", dernier);
        } while (dao.findByMatricule(matricule).isPresent());
        return matricule;
    }

    /** @return the normalised CIN (trimmed, upper case) */
    private String valider(String nom, String prenom, String cin, String cnss, Poste poste,
                           TypeContrat typeContrat, LocalDate dateEmbauche) {
        if (vide(nom) || vide(prenom)) {
            throw new ValidationException("Le nom et le prénom sont obligatoires.");
        }
        if (vide(cin)) {
            throw new ValidationException("Le CIN est obligatoire.");
        }
        if (!vide(cnss) && !cnss.trim().matches("\\d+")) {
            throw new ValidationException("Le numéro CNSS ne doit contenir que des chiffres.");
        }
        if (poste == null) {
            throw new ValidationException("Choisissez un poste.");
        }
        if (typeContrat == null) {
            throw new ValidationException("Choisissez un type de contrat.");
        }
        if (dateEmbauche == null) {
            throw new ValidationException("La date d'embauche est obligatoire.");
        }
        if (dateEmbauche.isAfter(LocalDate.now(clock))) {
            throw new ValidationException("La date d'embauche ne peut pas être dans le futur.");
        }
        return cin.trim().toUpperCase(Locale.ROOT);
    }

    private static int compter(List<Employe> employes, TypeContrat type) {
        return (int) employes.stream().filter(e -> e.getTypeContrat() == type).count();
    }

    private static int numero(String texte) {
        try {
            return Integer.parseInt(texte);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean vide(String texte) {
        return texte == null || texte.isBlank();
    }
}
