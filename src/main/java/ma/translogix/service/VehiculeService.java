package ma.translogix.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import ma.translogix.dao.VehiculeDao;
import ma.translogix.model.StatutVehicule;
import ma.translogix.model.Vehicule;
import ma.translogix.util.ValidationException;

/** Fleet rules ([H3]): matricule required and unique, capacity above 0. */
public class VehiculeService {

    private final VehiculeDao dao;

    public VehiculeService(VehiculeDao dao) {
        this.dao = dao;
    }

    /** All vehicles, by matricule. */
    public List<Vehicule> lister() {
        return dao.findAll().stream().sorted(Comparator.comparing(Vehicule::getMatricule)).toList();
    }

    public Vehicule trouver(Long id) {
        if (id == null) {
            throw new ValidationException("Choisissez un véhicule.");
        }
        return dao.findById(id).orElseThrow(() -> new ValidationException("Véhicule introuvable."));
    }

    public long compter(StatutVehicule statut) {
        return dao.findAll().stream().filter(v -> v.getStatut() == statut).count();
    }

    /** A new vehicle starts EN_SERVICE. */
    public Vehicule creer(String matricule, String marque, String modele, BigDecimal capaciteTonnes) {
        Vehicule v = new Vehicule(null, null, null, null, null, StatutVehicule.EN_SERVICE);
        remplir(v, matricule, marque, modele, capaciteTonnes);
        return dao.save(v);
    }

    public void modifier(Long id, String matricule, String marque, String modele, BigDecimal capaciteTonnes) {
        Vehicule v = trouver(id);
        remplir(v, matricule, marque, modele, capaciteTonnes);
        dao.update(v);
    }

    /** EN_SERVICE / MAINTENANCE / HORS_SERVICE. Missions already planned keep their vehicle. */
    public void changerStatut(Long id, StatutVehicule statut) {
        if (statut == null) {
            throw new ValidationException("Choisissez un statut.");
        }
        Vehicule v = trouver(id);
        v.setStatut(statut);
        dao.update(v);
    }

    private void remplir(Vehicule v, String matricule, String marque, String modele, BigDecimal capaciteTonnes) {
        String m = matricule == null ? "" : matricule.trim().toUpperCase(Locale.ROOT);
        if (m.isEmpty()) {
            throw new ValidationException("Le matricule est obligatoire.");
        }
        dao.findByMatricule(m)
                .filter(autre -> !autre.getId().equals(v.getId()))
                .ifPresent(autre -> {
                    throw new ValidationException("Le matricule " + m + " est déjà utilisé par un autre véhicule.");
                });
        if (capaciteTonnes == null || capaciteTonnes.signum() <= 0) {
            throw new ValidationException("La capacité doit être supérieure à 0 tonne.");
        }
        v.setMatricule(m);
        v.setMarque(vide(marque));
        v.setModele(vide(modele));
        v.setCapaciteTonnes(capaciteTonnes);
    }

    private static String vide(String texte) {
        return texte == null || texte.isBlank() ? null : texte.trim();
    }
}
