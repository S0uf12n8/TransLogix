package ma.translogix.service;

import java.util.List;
import java.util.regex.Pattern;
import ma.translogix.dao.UtilisateurDao;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.PasswordHasher;
import ma.translogix.util.ValidationException;

/** Business rules for user accounts. No JavaFX and no SQL here. */
public class UtilisateurService {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final int MOT_DE_PASSE_MIN = 8;

    private final UtilisateurDao dao;

    public UtilisateurService(UtilisateurDao dao) {
        this.dao = dao;
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
        Utilisateur utilisateur = new Utilisateur(null, nom.trim(), prenom.trim(), emailNormalise,
                PasswordHasher.hash(motDePasse), role, true);
        return dao.save(utilisateur);
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
        if (utilisateur.getRole() == Role.GERANT && role != Role.GERANT && estDernierGerant(id)) {
            throw new ValidationException("Impossible de retirer le rôle du dernier gérant.");
        }
        utilisateur.setNom(nom.trim());
        utilisateur.setPrenom(prenom.trim());
        utilisateur.setEmail(emailNormalise);
        utilisateur.setRole(role);
        utilisateur.setActif(actif);
        dao.update(utilisateur);
    }

    public void supprimer(Long id) {
        Utilisateur utilisateur = trouver(id);
        if (utilisateur.getRole() == Role.GERANT && estDernierGerant(id)) {
            throw new ValidationException("Impossible de supprimer le dernier gérant.");
        }
        dao.delete(id);
    }

    private Utilisateur trouver(Long id) {
        return dao.findById(id)
                .orElseThrow(() -> new ValidationException("Utilisateur introuvable."));
    }

    private boolean estDernierGerant(Long id) {
        return dao.findAll().stream()
                .noneMatch(u -> u.getRole() == Role.GERANT && !u.getId().equals(id));
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
