package ma.translogix.chauffeur.controller;

import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Gabarit d'un bloc d'information de mission (components/InfoBlock.fxml). */
public class InfoBlockController {
    @FXML private HBox root;
    @FXML private Label iconLabel, titleLabel, mainLabel, subLabel;

    public HBox getRoot() { return root; }

    public void setData(String icon, String title, String main, String sub) {
        iconLabel.setText(icon);
        titleLabel.setText(title.toUpperCase(Locale.FRENCH));  // .eyebrow : text-transform uppercase
        mainLabel.setText(main);
        subLabel.setText(sub);
    }
}
