package ma.translogix.chauffeur.model;

/** Notification du centre de messages (données de démonstration, comme dans le HTML d'origine). */
public class Notification {
    public final int id;
    public final String type;      // mission | instruction | alert
    public final String title;
    public final String text;
    public final String time;
    public boolean read;
    public final String actionLabel; // peut être null
    public final String actionRef;   // référence de mission, peut être null
    public final String actionTab;   // onglet cible, peut être null

    public Notification(int id, String type, String title, String text, String time, boolean read,
                        String actionLabel, String actionRef, String actionTab) {
        this.id = id; this.type = type; this.title = title; this.text = text; this.time = time;
        this.read = read; this.actionLabel = actionLabel; this.actionRef = actionRef; this.actionTab = actionTab;
    }
}
