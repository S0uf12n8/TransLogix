package ma.translogix.service;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import ma.translogix.dao.ClientDao;
import ma.translogix.dao.FactureDao;
import ma.translogix.model.Client;
import ma.translogix.util.ValidationException;

/**
 * Business rules for clients. Issue [C2]. No JavaFX and no SQL here.
 * Name required; ICE unique when filled; email valid when filled; a client with invoices cannot be deleted.
 */
public class ClientService {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final ClientDao dao;
    private final FactureDao factureDao;

    public ClientService(ClientDao dao, FactureDao factureDao) {
        this.dao = dao;
        this.factureDao = factureDao;
    }

    /** All clients, by name. */
    public List<Client> lister() {
        return dao.findAll().stream()
                .sorted(Comparator.comparing(Client::getNom, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Client trouver(Long id) {
        return dao.findById(id).orElseThrow(() -> new ValidationException("Client introuvable."));
    }

    public Client creer(String nom, String ice, String telephone, String email, String adresse,
                        int delaiPaiementJours) {
        Client client = new Client(null, null, null, null, null, null, delaiPaiementJours);
        remplir(client, nom, ice, telephone, email, adresse, delaiPaiementJours);
        return dao.save(client);
    }

    public void modifier(Long id, String nom, String ice, String telephone, String email, String adresse,
                         int delaiPaiementJours) {
        Client client = trouver(id);
        remplir(client, nom, ice, telephone, email, adresse, delaiPaiementJours);
        dao.update(client);
    }

    /** Refused while the client has invoices: deleting it would break the invoice history. */
    public void supprimer(Long id) {
        Client client = trouver(id);
        if (!factureDao.findByClient(id).isEmpty()) {
            throw new ValidationException("Impossible de supprimer « " + client.getNom()
                    + " » : ce client a des factures.");
        }
        dao.delete(id);
    }

    /** Validates every field, then copies them into the client. Shared by creer and modifier. */
    private void remplir(Client client, String nom, String ice, String telephone, String email,
                         String adresse, int delaiPaiementJours) {
        if (vide(nom)) {
            throw new ValidationException("Le nom du client est obligatoire.");
        }
        String iceNormalise = vide(ice) ? null : ice.trim(); // blank ICE stored as null (UNIQUE in the database)
        if (iceNormalise != null) {
            dao.findByIce(iceNormalise)
                    .filter(autre -> !autre.getId().equals(client.getId()))
                    .ifPresent(autre -> {
                        throw new ValidationException("Cet ICE est déjà utilisé par « " + autre.getNom() + " ».");
                    });
        }
        String emailNormalise = vide(email) ? null : email.trim().toLowerCase();
        if (emailNormalise != null && !EMAIL.matcher(emailNormalise).matches()) {
            throw new ValidationException("Adresse email invalide.");
        }
        if (delaiPaiementJours < 0) {
            throw new ValidationException("Le délai de paiement ne peut pas être négatif.");
        }
        client.setNom(nom.trim());
        client.setIce(iceNormalise);
        client.setTelephone(vide(telephone) ? null : telephone.trim());
        client.setEmail(emailNormalise);
        client.setAdresse(vide(adresse) ? null : adresse.trim());
        client.setDelaiPaiementJours(delaiPaiementJours);
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
