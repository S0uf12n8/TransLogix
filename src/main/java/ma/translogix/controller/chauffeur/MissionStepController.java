package ma.translogix.controller.chauffeur;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Gabarit d'une étape de suivi (components/MissionStep.fxml). */
public class MissionStepController {
    @FXML private HBox root;
    @FXML private javafx.scene.layout.VBox textBox;
    @FXML private Region line;
    @FXML private StackPane circle;
    @FXML private Label iconLabel, labelText, subText;

    public HBox getRoot() { return root; }

    /** state : done | current | pending */
    public void setData(String label, String state, String sub, boolean showLine, boolean lastItem) {
        circle.getStyleClass().removeIf(c -> c.startsWith("step-") && !c.equals("step-circle"));
        circle.getStyleClass().add("step-" + state);
        iconLabel.setText("done".equals(state) ? "\uf00c" : ("current".equals(state) ? "\uf3c5" : "\uf111"));
        labelText.setText(label);
        labelText.getStyleClass().removeAll("c-s400", "c-s800");
        labelText.getStyleClass().add("pending".equals(state) ? "c-s400" : "c-s800");
        subText.setText(sub);
        line.setVisible(showLine);
        if (lastItem) textBox.setPadding(javafx.geometry.Insets.EMPTY);   // last:pb-0
    }
}
