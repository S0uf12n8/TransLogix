package ma.translogix.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ma.translogix.dao.UtilisateurDao;
import ma.translogix.model.ResultatAudit;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.PasswordHasher;
import ma.translogix.util.ValidationException;

/**
 * Login check ([O2]). No JavaFX here: the login screen calls it, then opens the session and the shell.
 * <ul>
 *   <li>Every failure gives the same message, and an unknown email costs the same time as a wrong password,
 *       so the screen never reveals whether an email exists.</li>
 *   <li>Brute force: after {@value #ESSAIS_MAX} failures in a row on the same email, that email is blocked for
 *       {@link #BLOCAGE} (unknown emails too, again so that nothing tells them apart). A success resets it.</li>
 *   <li>Every attempt is written to the audit log (module {@value #MODULE_AUDIT}); the password never is.</li>
 * </ul>
 * The failure counters are kept in memory: they protect the running application and restart at zero with it.
 */
public class AuthService {

    public static final String ECHEC = "Email ou mot de passe incorrect.";
    public static final String MODULE_AUDIT = "Connexion";
    public static final int ESSAIS_MAX = 5;
    public static final Duration BLOCAGE = Duration.ofMinutes(15);

    private final UtilisateurDao dao;
    private final AuditService audit;
    private final Clock horloge;
    private final Map<String, Tentatives> tentatives = new ConcurrentHashMap<>();

    /** Failures in a row for one email, and the end of its block once it reached ESSAIS_MAX. */
    private record Tentatives(int echecs, Instant bloqueJusqua) {
    }

    public AuthService(UtilisateurDao dao, AuditService audit, Clock horloge) {
        this.dao = dao;
        this.audit = audit;
        this.horloge = horloge;
    }

    /**
     * @return the account matching the email (any case) and the password
     * @throws ValidationException with {@link #ECHEC} when the email is unknown, the password is wrong
     *         or the account is deactivated; with a "réessayez dans N minutes" message while the email is blocked
     */
    public Utilisateur connecter(String email, String motDePasse) {
        if (email == null || email.isBlank() || motDePasse == null || motDePasse.isEmpty()) {
            throw new ValidationException(ECHEC);
        }
        String cle = email.trim().toLowerCase(Locale.ROOT);
        Instant maintenant = horloge.instant();
        verifierNonBloque(cle, maintenant);

        Utilisateur utilisateur = dao.findByEmail(cle).orElse(null);
        if (utilisateur == null) {
            // Same work as a wrong password: the response time does not reveal that the email is unknown.
            PasswordHasher.verifierFactice(motDePasse);
            throw echec(cle, maintenant);
        }
        if (!PasswordHasher.verifier(motDePasse, utilisateur.getMotDePasseHash()) || !utilisateur.isActif()) {
            throw echec(cle, maintenant);
        }
        tentatives.remove(cle);
        mettreAJourLeHash(utilisateur, motDePasse);
        audit.enregistrer(utilisateur.getNomComplet() + " (" + utilisateur.getRole().getLibelle() + ")",
                MODULE_AUDIT, "Connexion réussie (" + cle + ")", ResultatAudit.REUSSI);
        return utilisateur;
    }

    /** True while this email is blocked (for the login screen or tests). */
    public boolean estBloque(String email) {
        if (email == null) {
            return false;
        }
        Tentatives t = tentatives.get(email.trim().toLowerCase(Locale.ROOT));
        return t != null && t.bloqueJusqua() != null && horloge.instant().isBefore(t.bloqueJusqua());
    }

    private void verifierNonBloque(String cle, Instant maintenant) {
        Tentatives t = tentatives.get(cle);
        if (t == null || t.bloqueJusqua() == null) {
            return;
        }
        if (maintenant.isBefore(t.bloqueJusqua())) {
            audit.enregistrer(cle, MODULE_AUDIT, "Connexion refusée : compte temporairement bloqué",
                    ResultatAudit.REFUSE);
            throw new ValidationException(messageBlocage(Duration.between(maintenant, t.bloqueJusqua())));
        }
        tentatives.remove(cle); // block over: a fresh series of attempts starts
    }

    /** Counts the failure, blocks the email at the ESSAIS_MAX-th one, and returns the exception to throw. */
    private ValidationException echec(String cle, Instant maintenant) {
        Tentatives t = tentatives.merge(cle, new Tentatives(1, null),
                (avant, un) -> new Tentatives(avant.echecs() + 1, null));
        if (t.echecs() >= ESSAIS_MAX) {
            tentatives.put(cle, new Tentatives(t.echecs(), maintenant.plus(BLOCAGE)));
            audit.enregistrer(cle, MODULE_AUDIT, "Compte bloqué " + BLOCAGE.toMinutes() + " minutes après "
                    + ESSAIS_MAX + " échecs de connexion", ResultatAudit.REFUSE);
            return new ValidationException(messageBlocage(BLOCAGE));
        }
        audit.enregistrer(cle, MODULE_AUDIT, "Échec de connexion (" + t.echecs() + "/" + ESSAIS_MAX + ")",
                ResultatAudit.REFUSE);
        return new ValidationException(ECHEC);
    }

    static String messageBlocage(Duration restant) {
        long minutes = Math.max(1, (restant.toSeconds() + 59) / 60); // rounded up, never "0 minute"
        return "Trop de tentatives de connexion. Réessayez dans " + minutes + (minutes > 1 ? " minutes." : " minute.");
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
