package ma.translogix.chauffeur.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Fiche détaillée d'une mission (objet missionData du HTML d'origine). */
public class MissionData {
    public final String ref, from, to, client, contact, loadingPlace, loadingDetail, unloadingPlace, unloadingDetail;
    public final String date, start, end, cargo, cargoDetail;
    public final List<String> instructions;
    public String status, statusCls;
    public int done;
    public final String[] times = new String[6];
    /** Links this view row back to the main app's Mission and Vehicule. */
    public Long missionId;
    public String vehicle = "—";

    public MissionData(String ref, String from, String to, String status, String statusCls, String client, String contact,
                       String loadingPlace, String loadingDetail, String unloadingPlace, String unloadingDetail,
                       String date, String start, String end, String cargo, String cargoDetail,
                       int done, String... instructions) {
        this.ref = ref; this.from = from; this.to = to; this.status = status; this.statusCls = statusCls;
        this.client = client; this.contact = contact; this.loadingPlace = loadingPlace; this.loadingDetail = loadingDetail;
        this.unloadingPlace = unloadingPlace; this.unloadingDetail = unloadingDetail; this.date = date;
        this.start = start; this.end = end; this.cargo = cargo; this.cargoDetail = cargoDetail; this.done = done;
        this.instructions = new ArrayList<>(Arrays.asList(instructions));
    }
}
