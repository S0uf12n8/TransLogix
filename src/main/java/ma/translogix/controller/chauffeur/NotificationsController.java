package ma.translogix.controller.chauffeur;

import java.io.IOException;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Section 6 : Notifications (liste filtrable, marquage lu / non lu). */
public class NotificationsController {
    @FXML private HBox filterBar;
    @FXML private VBox notifList;
    @FXML private Label notifCount;

    private String notifFilter = "all";

    @FXML
    private void initialize() {
        EspaceChauffeurUi.get().notifications.addListener((ListChangeListener<Notification>) c -> refresh());
        refresh();
    }

    /** Rafraîchit la liste si les nœuds FXML sont déjà initialisés. */
    public void refresh() {
        if (notifList != null) render();
    }

    private void render() {
        EspaceChauffeurUi ctx = EspaceChauffeurUi.get();
        notifList.getChildren().clear();
        long shown = ctx.notifications.stream().filter(n -> "all".equals(notifFilter) || n.type.equals(notifFilter)).count();
        if (shown == 0) {
            Label empty = new Label("Aucune notification dans cette catégorie.");
            empty.getStyleClass().add("notif-empty");
            empty.setMaxWidth(Double.MAX_VALUE);
            notifList.getChildren().add(empty);
        } else {
            int i = 0;
            for (Notification n : ctx.notifications) {
                if (!("all".equals(notifFilter) || n.type.equals(notifFilter))) continue;
                i++;
                try {
                    FXMLLoader l = new FXMLLoader(getClass().getResource("/ma/translogix/fxml/chauffeur/components/NotificationItem.fxml"));
                    Node node = l.load();
                    NotificationItemController c = l.getController();
                    c.setData(n, i == shown, this::openNotification);
                    c.setMenu(menuNotification(n));
                    notifList.getChildren().add(node);
                } catch (IOException e) { throw new IllegalStateException(e); }
            }
        }
        long unread = ctx.notifications.stream().filter(n -> !n.read).count();
        notifCount.setText(unread + " non lue(s) · " + ctx.notifications.size() + " au total");
        ctx.updateNotifBadges((int) unread);
    }

    /** Clic droit sur une notification : ouvrir (si possible) et basculer l'état de lecture. */
    private ContextMenu menuNotification(Notification n) {
        MenuItem ouvrir = new MenuItem("Ouvrir");
        ouvrir.setDisable(n.actionRef == null && n.actionTab == null);
        ouvrir.setOnAction(e -> openNotification(n));
        MenuItem lecture = new MenuItem(n.read ? "Marquer comme non lue" : "Marquer comme lue");
        lecture.setOnAction(e -> {
            n.read = !n.read;
            refresh();
        });
        return new ContextMenu(ouvrir, lecture);
    }

    @FXML
    private void onFilter(javafx.event.ActionEvent e) {
        Button btn = (Button) e.getSource();
        notifFilter = String.valueOf(btn.getUserData());
        filterBar.getChildren().forEach(b -> b.getStyleClass().remove("is-active"));
        btn.getStyleClass().add("is-active");
        render();
    }

    private void openNotification(Notification n) {
        n.read = true;
        refresh();
        if (n.actionRef != null) EspaceChauffeurUi.get().openMissionDetail(n.actionRef);
        else if (n.actionTab != null) EspaceChauffeurUi.get().navigate(n.actionTab);
    }

    @FXML
    private void markAll() {
        EspaceChauffeurUi.get().notifications.forEach(n -> n.read = true);
        refresh();
        EspaceChauffeurUi.get().toast("Toutes les notifications sont marquées comme lues.");
    }
}
