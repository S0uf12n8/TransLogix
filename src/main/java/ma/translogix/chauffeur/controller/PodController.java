package ma.translogix.chauffeur.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.FileChooser;
import ma.translogix.chauffeur.AppContext;
import ma.translogix.chauffeur.model.MissionData;
import ma.translogix.chauffeur.util.FileInput;
import ma.translogix.chauffeur.util.Ui;
import ma.translogix.controller.common.Dialogues;

/** Section 8 : Preuve de livraison (photo, signature à main levée, quantité, remarques). */
public class PodController {
    @FXML private ComboBox<String> podMission;
    @FXML private HBox podPhotoBox;
    @FXML private Button podPhotoBtn;
    @FXML private Label podPhotoName, podEmpty;
    @FXML private StackPane podPhotoPreviewBox;
    @FXML private ImageView podPhotoPreview;
    @FXML private Pane sigPane;
    @FXML private Canvas sigCanvas;
    @FXML private TextField podReceiver, podQty;
    @FXML private TextArea podRemarks;
    @FXML private VBox podList;

    private record PodEntry(String title, String detail) {}

    private FileInput photo;
    private final List<List<double[]>> strokes = new ArrayList<>();   // coordonnées normalisées (0..1)
    private final List<PodEntry> entries = new ArrayList<>();          // preuves de la session
    private boolean sigDirty = false;

    @FXML
    private void initialize() {
        AppContext ctx = AppContext.get();
        List<String> refs = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (MissionData m : ctx.missionData.values()) {
            refs.add(m.ref);
            labels.add(m.ref + " · " + m.from + " → " + m.to);
        }
        podMission.setItems(FXCollections.observableArrayList(labels));
        int selected = refs.indexOf(ctx.selectedMissionRef);
        podMission.getSelectionModel().select(selected >= 0 ? selected : 0);
        Ui.numberOnly(podQty);

        photo = new FileInput(podPhotoBtn, podPhotoName,
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
        photo.setOnChange(this::previewPodPhoto);

        // zone de signature : le canvas suit la taille du panneau
        sigCanvas.widthProperty().bind(sigPane.widthProperty());
        sigCanvas.heightProperty().bind(sigPane.heightProperty());
        sigCanvas.widthProperty().addListener((o, a, b) -> redraw());
        sigCanvas.heightProperty().addListener((o, a, b) -> redraw());
        sigPane.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            List<double[]> s = new ArrayList<>();
            s.add(norm(e));
            strokes.add(s);
        });
        sigPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (strokes.isEmpty()) return;
            strokes.get(strokes.size() - 1).add(norm(e));
            sigDirty = true;
            redraw();
        });
    }

    private double[] norm(MouseEvent e) {
        double w = Math.max(1, sigPane.getWidth()), h = Math.max(1, sigPane.getHeight());
        return new double[]{e.getX() / w, e.getY() / h};
    }

    private void redraw() {
        GraphicsContext g = sigCanvas.getGraphicsContext2D();
        double w = sigCanvas.getWidth(), h = sigCanvas.getHeight();
        g.clearRect(0, 0, w, h);
        g.setLineWidth(2.2);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setStroke(Color.web("#172033"));
        for (List<double[]> s : strokes) {
            if (s.size() < 2) continue;
            g.beginPath();
            g.moveTo(s.get(0)[0] * w, s.get(0)[1] * h);
            for (int i = 1; i < s.size(); i++) g.lineTo(s.get(i)[0] * w, s.get(i)[1] * h);
            g.stroke();
        }
    }

    @FXML
    private void clearSignature() {
        strokes.clear();
        sigDirty = false;
        redraw();
    }

    private void previewPodPhoto(File f) {
        if (f != null) {
            podPhotoPreview.setImage(new Image(f.toURI().toString(), 0, 192, true, true));
            Ui.setVisibleManaged(podPhotoPreviewBox, true);
        } else {
            podPhotoPreview.setImage(null);
            Ui.setVisibleManaged(podPhotoPreviewBox, false);
        }
    }

    @FXML
    private void submitPod() {
        // validation native des champs « required » dans l'ordre du DOM
        if (photo.getFile() == null) { Ui.invalid(podPhotoBox, "Veuillez sélectionner un fichier."); return; }
        if (!Ui.requireText(podReceiver)) return;
        if (!Ui.requireNumber(podQty, 0.0)) return;

        AppContext ctx = AppContext.get();
        if (!sigDirty) { ctx.toast("La signature du réceptionnaire est requise."); return; }
        String selection = podMission.getValue();
        String ref = Ui.firstToken(selection);   // le combo affiche « REF · … » : on isole la référence
        if (ref.isBlank()) { Ui.invalid(podMission, "Veuillez sélectionner une mission."); return; }
        String receiver = podReceiver.getText().trim();
        long qty = Long.parseLong(podQty.getText());
        String heure = Ui.nowTime();

        try {
            ctx.livrer(ref);                     // écriture réelle dans le service TransLogix
        } catch (RuntimeException e) {
            Dialogues.erreur(e.getMessage());
            return;
        }

        ctx.addNotification("mission", "Preuve de livraison transmise",
                ref + " · reçue par " + receiver + " (" + qty + " palette(s)).", null, ref, null);
        entries.add(0, new PodEntry(selection == null ? ref : selection,
                "Reçu par " + receiver + " · " + qty + " palette(s) · " + heure));
        rebuildPodList();

        // reset du formulaire
        podMission.getSelectionModel().selectFirst();
        photo.reset();
        podReceiver.clear();
        podQty.clear();
        podRemarks.clear();
        clearSignature();
        ctx.toast("Preuve de livraison transmise.");
    }

    private void rebuildPodList() {
        podList.getChildren().clear();
        if (entries.isEmpty()) {
            podList.getChildren().add(podEmpty);
            return;
        }
        for (PodEntry e : entries) {
            VBox li = new VBox();
            li.getStyleClass().add("pod-entry");
            Label t1 = new Label(e.title());
            t1.getStyleClass().addAll("fs-12", "fw-b", "c-s800");
            Label t2 = new Label(e.detail());
            t2.getStyleClass().addAll("fs-12", "c-s600", "pt-1");
            t2.setWrapText(true);
            li.getChildren().addAll(t1, t2);
            podList.getChildren().add(li);
        }
    }
}
