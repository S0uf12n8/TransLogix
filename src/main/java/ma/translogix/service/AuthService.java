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
        Utilisateur utilisateur = dao.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ValidationException(ECHEC));
        if (!PasswordHasher.verifier(motDePasse, utilisateur.getMotDePasseHash()) || !utilisateur.isActif()) {
            throw new ValidationException(ECHEC);
        }
        return utilisateur;
    }
}
