package ma.translogix.controller.chauffeur;

import java.io.File;
import java.util.function.Consumer;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

/** Équivalent JavaFX d'un champ &lt;input type="file"&gt; (bouton + nom du fichier). */
public final class FileInput {
    private final Button button;
    private final Label nameLabel;
    private File file;
    private Consumer<File> onChange = f -> {};

    public FileInput(Button button, Label nameLabel, FileChooser.ExtensionFilter... filters) {
        this.button = button;
        this.nameLabel = nameLabel;
        button.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().addAll(filters);
            File f = fc.showOpenDialog(button.getScene().getWindow());
            if (f != null) set(f);
        });
    }

    private void set(File f) {
        file = f;
        nameLabel.setText(f.getName());
        onChange.accept(f);
    }

    public File getFile() { return file; }

    public void setOnChange(Consumer<File> c) { this.onChange = c; }

    public void reset() {
        file = null;
        nameLabel.setText("Aucun fichier choisi");
        onChange.accept(null);
    }
}
