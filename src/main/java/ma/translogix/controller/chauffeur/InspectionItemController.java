package ma.translogix.controller.chauffeur;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/** Gabarit d'un point de contrôle (components/InspectionItem.fxml). */
public class InspectionItemController {
    @FXML private VBox root;
    @FXML private Label iconLabel, labelText, hintText;
    @FXML private RadioButton okRadio, koRadio;
    @FXML private ToggleGroup choiceGroup;
    @FXML private TextField noteField;

    private String label;

    public VBox getRoot() { return root; }
    public String getLabel() { return label; }
    public TextField getNoteField() { return noteField; }

    public void setData(String label, String hint, String icon, Runnable onChange) {
        this.label = label;
        labelText.setText(label);
        hintText.setText(hint);
        iconLabel.setText(icon);
        choiceGroup.selectedToggleProperty().addListener((o, a, b) -> {
            Ui.setVisibleManaged(noteField, koRadio.isSelected());
            onChange.run();
        });
    }

    /** "ok", "ko" ou null. */
    public String getChoice() { return okRadio.isSelected() ? "ok" : (koRadio.isSelected() ? "ko" : null); }
}
