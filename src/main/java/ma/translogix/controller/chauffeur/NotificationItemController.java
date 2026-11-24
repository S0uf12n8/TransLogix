package ma.translogix.controller.chauffeur;

import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/** Gabarit d'une ligne de notification (components/NotificationItem.fxml). */
public class NotificationItemController {
    @FXML private HBox root;
    @FXML private StackPane iconBox;
    @FXML private Label iconLabel, titleLabel, typePill, newPill, textLabel, timeLabel, actionLabel;

    public HBox getRoot() { return root; }

    public void setData(Notification n, boolean last, Consumer<Notification> onClick) {
        String icon, tint, label, cls;
        switch (n.type) {
            case "mission":     icon = "\uf4d7"; tint = "ib-blue";   label = "Mission";     cls = "blue";  break;
            case "instruction": icon = "\uf46d"; tint = "ib-orange"; label = "Instruction"; cls = "amber"; break;
            default:            icon = "\uf071"; tint = "ib-red";    label = "Alerte";      cls = "red";
        }
        iconLabel.setText(icon);
        iconBox.getStyleClass().removeIf(c -> c.startsWith("ib-"));
        iconBox.getStyleClass().add(tint);
        titleLabel.setText(n.title);
        Ui.setPill(typePill, cls, label);
        Ui.setPill(newPill, "green", "Nouveau");
        Ui.setVisibleManaged(newPill, !n.read);
        textLabel.setText(n.text);
        timeLabel.setText(n.time);
        if (n.actionLabel != null) actionLabel.setText(n.actionLabel + " →");
        Ui.setVisibleManaged(actionLabel, n.actionLabel != null);
        if (!n.read) root.getStyleClass().add("unread");
        if (last) root.getStyleClass().add("last");
        root.setOnMouseClicked(e -> onClick.accept(n));
    }
}
