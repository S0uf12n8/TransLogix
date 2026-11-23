package ma.translogix.chauffeur.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import ma.translogix.chauffeur.AppContext;
import ma.translogix.chauffeur.model.MissionData;
import ma.translogix.chauffeur.util.Ui;
import ma.translogix.model.Employe;

/** Section 10 : Messagerie Dispatching (démo, réponse automatique après 1,2 s). */
public class ChatController {
    @FXML private ScrollPane chatScroll;
    @FXML private VBox chatMessages;
    @FXML private TextField chatInput;

    @FXML
    private void initialize() {
        AppContext ctx = AppContext.get();
        ctx.register(this);
        Employe driver = ctx.chauffeur();
        String prenom = driver == null || driver.getPrenom() == null || driver.getPrenom().isBlank()
                ? "chauffeur" : driver.getPrenom();
        MissionData mission = ctx.missionData.get(ctx.selectedMissionRef);
        String consigne = mission == null ? "Consultez votre ordre de transport dans le carnet de route."
                : "Mission " + mission.ref + " : " + mission.from + " → " + mission.to + ".";
        addChatMessage("Bonjour " + prenom + ", " + consigne, "in");
        addChatMessage("Merci de confirmer votre ETA à la prochaine pause.", "in");
    }

    public void scrollChat() {
        Platform.runLater(() -> { chatScroll.layout(); chatScroll.setVvalue(1.0); });
    }

    private void addChatMessage(String text, String from) {
        HBox wrap = new HBox();
        wrap.setAlignment("out".equals(from) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        VBox bubble = new VBox();
        bubble.getStyleClass().addAll("bubble", from);
        Label p = new Label(text);
        p.getStyleClass().add("bubble-text");
        p.setWrapText(true);
        Label t = new Label(Ui.nowTime());
        t.getStyleClass().add("bubble-time");
        bubble.getChildren().addAll(p, t);
        // max-width: 80 %
        bubble.maxWidthProperty().bind(chatMessages.widthProperty().subtract(32).multiply(0.8));
        p.maxWidthProperty().bind(bubble.maxWidthProperty().subtract(24));
        wrap.getChildren().add(bubble);
        chatMessages.getChildren().add(wrap);
        scrollChat();
    }

    private void sendChat(String text) {
        addChatMessage(text, "out");
        PauseTransition d = new PauseTransition(Duration.millis(1200));
        d.setOnFinished(e -> addChatMessage("Message bien reçu. (Réponse automatique de démonstration)", "in"));
        d.play();
    }

    @FXML private void quickEta() {
        MissionData mission = AppContext.get().missionData.get(AppContext.get().selectedMissionRef);
        sendChat("ETA confirmé" + (mission == null || mission.end.isBlank()
                ? "." : " : arrivée prévue à " + mission.end + "."));
    }
    @FXML private void quickPause() { sendChat("Je commence ma pause réglementaire."); }
    @FXML private void quickHelp() { sendChat("J'ai besoin d'assistance, merci de me rappeler."); }

    @FXML
    private void submitChat() {
        if (!Ui.requireText(chatInput)) return;
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;
        sendChat(text);
        chatInput.clear();
    }
}
