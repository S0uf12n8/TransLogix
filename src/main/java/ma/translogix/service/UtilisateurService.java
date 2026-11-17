package ma.translogix.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import ma.translogix.dao.UtilisateurDao;
import ma.translogix.model.ResultatAudit;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.PasswordHasher;
import ma.translogix.util.ValidationException;

/**
 * Business rules for user accounts. No JavaFX and no SQL here.
 * Gérant rules: exactly one GERANT account (the CEO); it cannot be deleted, demoted or deactivated.
 */
public class UtilisateurService {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final int MOT_DE_PASSE_MIN = 8;

    private static final String GERANT_UNIQUE = "Il existe déjà un compte Gérant (un seul compte dirigeant).";

    /** Module name written in the audit log. */
    public static final String MODULE_AUDIT = "Utilisateurs";

    private final UtilisateurDao dao;
    private final AuditService audit;

    public UtilisateurService(UtilisateurDao dao, AuditService audit) {
        this.dao = dao;
        this.audit = audit;
    }

    public List<Utilisateur> lister() {
        return dao.findAll();
    }

    public Utilisateur creer(String nom, String prenom, String email, String motDePasse, Role role) {
        validerChamps(nom, prenom, email, role);
        if (motDePasse == null || motDePasse.length() < MOT_DE_PASSE_MIN) {
            throw new ValidationException("Le mot de passe doit contenir au moins "
                    + MOT_DE_PASSE_MIN + " caractères.");
        }
        String emailNormalise = email.trim().toLowerCase();
        if (dao.findByEmail(emailNormalise).isPresent()) {
            throw new ValidationException("Cet email est déjà utilisé.");
        }
        if (role == Role.GERANT && gerantExistant().isPresent()) {
            refuser("Création refusée : un compte Gérant existe déjà (" + emailNormalise + ")",
                    GERANT_UNIQUE);
        }
        Utilisateur utilisateur = new Utilisateur(null, nom.trim(), prenom.trim(), emailNormalise,
                PasswordHasher.hash(motDePasse), role, true);
        Utilisateur cree = dao.save(utilisateur);
        audit.enregistrer(MODULE_AUDIT, "Création du compte " + emailNormalise
                + " (" + role.getLibelle() + ")", ResultatAudit.REUSSI);
        return cree;
    }

    public void modifier(Long id, String nom, String prenom, String email, Role role, boolean actif) {
        Utilisateur utilisateur = trouver(id);
        validerChamps(nom, prenom, email, role);
        String emailNormalise = email.trim().toLowerCase();
        dao.findByEmail(emailNormalise)
                .filter(autre -> !autre.getId().equals(id))
                .ifPresent(autre -> {
                    throw new ValidationException("Cet email est déjà utilisé.");
                });
        boolean estLeGerant = utilisateur.getRole() == Role.GERANT;
        if (estLeGerant && role != Role.GERANT) {
            refuser("Modification refusée : rôle du Gérant (" + utilisateur.getEmail() + ")",
                    "Le rôle du Gérant ne peut pas être modifié.");
        }
        if (estLeGerant && !actif) {
            refuser("Modification refusée : désactivation du Gérant (" + utilisateur.getEmail() + ")",
                    "Le compte Gérant ne peut pas être désactivé.");
        }
        if (!estLeGerant && role == Role.GERANT) {
            refuser("Modification refusée : promotion de " + utilisateur.getEmail() + " en Gérant",
                    GERANT_UNIQUE);
        }
        utilisateur.setNom(nom.trim());
        utilisateur.setPrenom(prenom.trim());
        utilisateur.setEmail(emailNormalise);
        utilisateur.setRole(role);
        utilisateur.setActif(actif);
        dao.update(utilisateur);
        audit.enregistrer(MODULE_AUDIT, "Modification du compte " + emailNormalise, ResultatAudit.REUSSI);
    }

    public void supprimer(Long id) {
        Utilisateur utilisateur = trouver(id);
        if (utilisateur.getRole() == Role.GERANT) {
            refuser("Suppression refusée : compte Gérant (" + utilisateur.getEmail() + ")",
                    "Le compte Gérant ne peut pas être supprimé.");
        }
        dao.delete(id);
        audit.enregistrer(MODULE_AUDIT, "Suppression du compte " + utilisateur.getEmail(), ResultatAudit.REUSSI);
    }

    private Utilisateur trouver(Long id) {
        return dao.findById(id)
                .orElseThrow(() -> new ValidationException("Utilisateur introuvable."));
    }

    /** The single CEO account, if it exists yet. */
    public Optional<Utilisateur> gerantExistant() {
        return dao.findAll().stream().filter(u -> u.getRole() == Role.GERANT).findFirst();
    }

    /** Logs a refused Gérant-protection action, then rejects it. */
    private void refuser(String actionAudit, String message) {
        audit.enregistrer(MODULE_AUDIT, actionAudit, ResultatAudit.REFUSE);
        throw new ValidationException(message);
    }

    private void validerChamps(String nom, String prenom, String email, Role role) {
        if (estVide(nom) || estVide(prenom)) {
            throw new ValidationException("Le nom et le prénom sont obligatoires.");
        }
        if (estVide(email) || !EMAIL.matcher(email.trim()).matches()) {
            throw new ValidationException("Email invalide.");
        }
        if (role == null) {
            throw new ValidationException("Choisissez un rôle.");
        }
    }

    private static boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
