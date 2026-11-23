package ma.translogix.service;

import ma.translogix.dao.UtilisateurDao;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.PasswordHasher;
import ma.translogix.util.ValidationException;

/**
 * Login check ([O2]). No JavaFX here: the login screen calls it, then opens the session and the shell.
 * Every failure gives the same message, so the screen never reveals whether an email exists.
 */
public class AuthService {

    public static final String ECHEC = "Email ou mot de passe incorrect.";

    private final UtilisateurDao dao;

    public AuthService(UtilisateurDao dao) {
        this.dao = dao;
    }

    /**
     * @return the account matching the email (any case) and the password
     * @throws ValidationException with {@link #ECHEC} when the email is unknown, the password is wrong
     *         or the account is deactivated
     */
    public Utilisateur connecter(String email, String motDePasse) {
        if (email == null || email.isBlank() || motDePasse == null || motDePasse.isEmpty()) {
            throw new ValidationException(ECHEC);
        }
        Utilisateur utilisateur = dao.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (utilisateur == null) {
            // Same work as a wrong password: the response time does not reveal that the email is unknown.
            PasswordHasher.verifierFactice(motDePasse);
            throw new ValidationException(ECHEC);
        }
        if (!PasswordHasher.verifier(motDePasse, utilisateur.getMotDePasseHash()) || !utilisateur.isActif()) {
            throw new ValidationException(ECHEC);
        }
        mettreAJourLeHash(utilisateur, motDePasse);
        return utilisateur;
    }

    /**
     * An old hash (iteration 2, SHA-256) or one with fewer iterations than today is replaced right after a
     * successful login: the only moment the clear password is known. Accounts migrate without any reset.
     */
    private void mettreAJourLeHash(Utilisateur utilisateur, String motDePasse) {
        if (PasswordHasher.aMettreAJour(utilisateur.getMotDePasseHash())) {
            utilisateur.setMotDePasseHash(PasswordHasher.hash(motDePasse));
            dao.update(utilisateur);
        }
    }
}
