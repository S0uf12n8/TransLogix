package ma.translogix.chauffeur.model;

/** Lignes des différents tableaux. */
public final class Rows {
    private Rows() {}

    public static class MissionRow {
        public final String ref, from, to, client, cargo, dates, cmrLabel, cmrCls, statusLabel, statusCls;
        public final String action1Label, action1Toast;
        public final boolean action1Orange;
        public MissionRow(String ref, String from, String to, String client, String cargo, String dates,
                          String cmrLabel, String cmrCls, String statusLabel, String statusCls,
                          String action1Label, boolean action1Orange, String action1Toast) {
            this.ref = ref; this.from = from; this.to = to; this.client = client; this.cargo = cargo; this.dates = dates;
            this.cmrLabel = cmrLabel; this.cmrCls = cmrCls; this.statusLabel = statusLabel; this.statusCls = statusCls;
            this.action1Label = action1Label; this.action1Orange = action1Orange; this.action1Toast = action1Toast;
        }
        public String searchText() {
            return String.join(" ", ref, from, to, client, cargo, dates, cmrLabel, statusLabel, action1Label, "Détails");
        }
    }

    public static class ExpenseRow {
        public final String ref, mission, type, station, amount, statusLabel, statusCls;
        public ExpenseRow(String ref, String mission, String type, String station, String amount, String statusLabel, String statusCls) {
            this.ref = ref; this.mission = mission; this.type = type; this.station = station; this.amount = amount;
            this.statusLabel = statusLabel; this.statusCls = statusCls;
        }
        public String searchText() { return String.join(" ", ref, mission, type, station, amount, statusLabel); }
    }

    public static class IncidentRow {
        public final String ref, mission, type, severity, severityCls, location, attachment, status;
        public IncidentRow(String ref, String mission, String type, String severity, String severityCls, String location, String attachment, String status) {
            this.ref = ref; this.mission = mission; this.type = type; this.severity = severity; this.severityCls = severityCls;
            this.location = location; this.attachment = attachment; this.status = status;
        }
    }

    public static class ExceptionalRow {
        public final String ref, mission, reason, date;
        public String decisionLabel = "En attente de décision", decisionCls = "amber";
        public boolean decided = false;
        public ExceptionalRow(String ref, String mission, String reason, String date) {
            this.ref = ref; this.mission = mission; this.reason = reason; this.date = date;
        }
    }

    public static class HistoryRow {
        public final String ref, from, to, client, date, proofLabel, proofCls, statusLabel, statusCls, detailsToast;
        public final boolean opensDetail;
        public HistoryRow(String ref, String from, String to, String client, String date, String proofLabel, String proofCls,
                          String statusLabel, String statusCls, boolean opensDetail) {
            this.ref = ref; this.from = from; this.to = to; this.client = client; this.date = date;
            this.proofLabel = proofLabel; this.proofCls = proofCls; this.statusLabel = statusLabel; this.statusCls = statusCls;
            this.opensDetail = opensDetail; this.detailsToast = "Détail archivé non disponible dans la démo.";
        }
        public String searchText() { return String.join(" ", ref, from, to, client, date, proofLabel, statusLabel, "Détails"); }
    }
}
