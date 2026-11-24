package ma.translogix.dao.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ma.translogix.dao.CritereFacture;
import ma.translogix.dao.FactureDao;
import ma.translogix.model.Facture;
import ma.translogix.util.Recherche;

/** Keeps invoices in a map. Used by tests and by the UI until the JDBC DAO exists ([C5]). */
public class InMemoryFactureDao implements FactureDao {
    private final Map<Long, Facture> donnees = new LinkedHashMap<>();
    private long prochainId = 1;

    @Override
    public List<Facture> findAll() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public Optional<Facture> findById(Long id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public List<Facture> findByClient(Long clientId) {
        return donnees.values().stream().filter(f -> clientId.equals(f.getClientId())).toList();
    }

    @Override
    public Optional<Facture> findByMission(Long missionId) {
        return donnees.values().stream().filter(f -> missionId.equals(f.getMissionId())).findFirst();
    }

    @Override
    public List<Facture> search(CritereFacture c) {
        CritereFacture critere = c == null ? CritereFacture.TOUT : c;
        String motCle = critere.motCle();
        boolean sansMotCle = motCle == null || motCle.isBlank();
        List<Long> idsClients = critere.idsClientsMotes() == null ? List.of() : critere.idsClientsMotes();
        return donnees.values().stream()
                .filter(f -> sansMotCle || Recherche.correspond(motCle, f.getNumero(), f.getReferenceTrajet())
                        || idsClients.contains(f.getClientId()))
                .filter(f -> critere.clientId() == null || critere.clientId().equals(f.getClientId()))
                .filter(f -> critere.statut() == null || critere.statut().equals(f.getStatut().getLibelle()))
                .filter(f -> critere.du() == null
                        || (f.getDateEmission() != null && !f.getDateEmission().isBefore(critere.du())))
                .filter(f -> critere.au() == null
                        || (f.getDateEmission() != null && !f.getDateEmission().isAfter(critere.au())))
                .filter(f -> critere.montantMin() == null
                        || (f.getMontantTtc() != null && f.getMontantTtc().compareTo(critere.montantMin()) >= 0))
                .filter(f -> critere.montantMax() == null
                        || (f.getMontantTtc() != null && f.getMontantTtc().compareTo(critere.montantMax()) <= 0))
                .toList();
    }

    @Override
    public Facture save(Facture facture) {
        facture.setId(prochainId++);
        donnees.put(facture.getId(), facture);
        return facture;
    }

    @Override
    public void update(Facture facture) {
        if (!donnees.containsKey(facture.getId())) {
            throw new IllegalArgumentException("Facture inconnue : " + facture.getId());
        }
        donnees.put(facture.getId(), facture);
    }

    @Override
    public void delete(Long id) {
        donnees.remove(id);
    }
}
