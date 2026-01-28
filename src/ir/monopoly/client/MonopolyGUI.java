package ir.monopoly.client;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileInputStream;
import java.util.*;

public class MonopolyGUI extends Application {
    private NetworkClient client;
    private int myPlayerId = -1;
    private boolean hasRolledThisTurn = false;

    // UI Elements
    private Pane boardPane;
    private TextArea logArea;
    private Label balanceLabel, playerInfoLabel, statusLabel;
    private ListView<String> propertyList;
    private final Map<Integer, double[]> tileCoords = new HashMap<>();
    private final Map<Integer, Circle> playerTokens = new HashMap<>();
    private final Map<String, Image> iconCache = new HashMap<>();
    private Image bgImage, centerImage;

    private static final double BOARD_SIZE = 680;
    private static final double TILE_SIZE = BOARD_SIZE / 11.0;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) {
        loadAssets(); // Restoring your Christmas assets

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Restore Luxury Christmas Background
        if (bgImage != null) {
            root.setBackground(new Background(new BackgroundImage(bgImage, null, null, null,
                    new BackgroundSize(1.0, 1.0, true, true, false, true))));
        } else {
            root.setStyle("-fx-background-color: #0F3B2E;"); // Festive Green fallback
        }

        // Center: Board Setup
        boardPane = new Pane();
        boardPane.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        boardPane.setStyle("-fx-background-color: rgba(253, 245, 230, 0.9); -fx-border-color: #D4AF37; -fx-border-width: 4; -fx-background-radius: 15;");
        drawFestiveBoard();
        addCenterArt();
        initTokens();

        StackPane centerContainer = new StackPane(boardPane);
        centerContainer.setPadding(new Insets(15));
        root.setCenter(centerContainer);

        // Right: Restored Dashboard
        root.setRight(createFestiveRightPanel());

        // Bottom: Restored Luxury Controls
        root.setBottom(createControlPanel());

        Scene scene = new Scene(root, 1150, 850);
        primaryStage.setTitle("Monopoly Professional - Christmas Edition 🎄");
        primaryStage.setScene(scene);
        primaryStage.show();

        connectToServer();
    }

    private void loadAssets() {
        String path = "assets/";
        bgImage = safeLoad(path + "main_bg.jpg");
        centerImage = safeLoad(path + "center_board.png");
        String[] icons = {"go", "jail", "parking", "gotojail", "chest", "chance", "tax", "utility", "train"};
        for (String s : icons) {
            Image img = safeLoad(path + "icon_" + s + ".png");
            if (img != null) iconCache.put(s, img);
        }
    }

    private Image safeLoad(String p) {
        try { return new Image(new FileInputStream(p)); } catch (Exception e) { return null; }
    }

    private VBox createFestiveRightPanel() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10, 20, 10, 10));
        box.setPrefWidth(320);
        box.setStyle("-fx-background-color: rgba(255, 255, 240, 0.85); -fx-background-radius: 15; -fx-border-color: #D4AF37; -fx-border-width: 0 0 0 4;");

        playerInfoLabel = new Label("Connecting...");
        playerInfoLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 18));

        balanceLabel = new Label("💰 Balance: $1500");
        balanceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        balanceLabel.setTextFill(Color.DARKGREEN);

        propertyList = new ListView<>();
        propertyList.setPrefHeight(180);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(250);

        statusLabel = new Label("Waiting...");
        statusLabel.setTextFill(Color.FIREBRICK);

        box.getChildren().addAll(playerInfoLabel, balanceLabel, new Separator(), new Label("Properties:"), propertyList, new Label("Log:"), logArea, statusLabel);
        return box;
    }

    private HBox createControlPanel() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-background-radius: 20;");

        Button roll = createLuxBtn("🎲 ROLL", "#D4AF37");
        Button buy = createLuxBtn("🏠 BUY", "#27ae60");
        Button trade = createLuxBtn("🤝 TRADE", "#2980b9");
        Button end = createLuxBtn("⏭ END", "#c0392b");

        roll.setOnAction(e -> { client.sendMessage("ROLL"); hasRolledThisTurn = true; roll.setDisable(true); });
        buy.setOnAction(e -> client.sendMessage("BUY"));
        trade.setOnAction(e -> showTradeDialog());
        end.setOnAction(e -> client.sendMessage("END_TURN"));

        box.getChildren().addAll(roll, buy, trade, end);
        return box;
    }

    private void drawFestiveBoard() {
        for (int i = 0; i < 40; i++) {
            double[] pos = getTilePosition(i);
            Rectangle r = new Rectangle(pos[0], pos[1], TILE_SIZE, TILE_SIZE);
            r.setFill(Color.web("#FDF5E6")); r.setStroke(Color.DARKSLATEGRAY);
            boardPane.getChildren().add(r);

            tileCoords.put(i, new double[]{pos[0] + TILE_SIZE/2, pos[1] + TILE_SIZE/2});

            ImageView iv = new ImageView();
            iv.setFitWidth(TILE_SIZE * 0.75); iv.setFitHeight(TILE_SIZE * 0.75);
            iv.setX(pos[0] + TILE_SIZE * 0.125); iv.setY(pos[1] + TILE_SIZE * 0.125);
            Image icon = getCorrectIcon(i);
            if (icon != null) { iv.setImage(icon); boardPane.getChildren().add(iv); }
        }
    }

    private void addCenterArt() {
        double inner = BOARD_SIZE - (2 * TILE_SIZE) - 40;
        Rectangle art = new Rectangle(TILE_SIZE + 20, TILE_SIZE + 20, inner, inner);
        if (centerImage != null) art.setFill(new ImagePattern(centerImage));
        art.setEffect(new DropShadow(15, Color.GOLD));
        boardPane.getChildren().add(art);
    }

    private void initTokens() {
        Color[] colors = {Color.RED, Color.CYAN, Color.LIME, Color.GOLD};
        for (int i = 1; i <= 4; i++) {
            Circle c = new Circle(10, colors[i-1]);
            c.setStroke(Color.WHITE);
            c.setCenterX(tileCoords.get(0)[0]); c.setCenterY(tileCoords.get(0)[1]);
            playerTokens.put(i, c); boardPane.getChildren().add(c);
        }
    }

    private void processMessage(String json) {
        Platform.runLater(() -> {
            String type = getJsonVal(json, "type");
            String msg = getJsonVal(json, "message");
            if (!msg.isEmpty()) logArea.appendText("➤ " + msg + "\n");

            switch (type) {
                case "CONNECTED" -> myPlayerId = Integer.parseInt(getJsonVal(json, "playerId"));
                case "TURN_UPDATE" -> {
                    int curr = Integer.parseInt(getJsonVal(json, "currentPlayer"));
                    boolean isMe = (curr == myPlayerId);
                    statusLabel.setText(isMe ? "⭐ YOUR TURN" : "Wait for P" + curr);
                    hasRolledThisTurn = false;
                }
                case "ROLL_UPDATE" -> moveSmoothly(Integer.parseInt(getJsonVal(json, "playerId")), Integer.parseInt(getJsonVal(json, "currentPosition")));
                case "PLAYER_STATS" -> {
                    if (Integer.parseInt(getJsonVal(json, "playerId")) == myPlayerId)
                        balanceLabel.setText("💰 Balance: $" + getJsonVal(json, "balance"));
                }
                case "SHOW_CARD" -> {
                    Alert a = new Alert(Alert.AlertType.INFORMATION, getJsonVal(json, "text"));
                    a.show();
                }
                case "TRADE_REQUEST" -> handleTradeUI(json);
            }
        });
    }

    private void moveSmoothly(int pId, int tileIdx) {
        Circle token = playerTokens.get(pId);
        double[] target = tileCoords.get(tileIdx);
        if (token != null && target != null) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(500), token);
            double offset = (pId * 5) - 10;
            tt.setToX(target[0] - token.getCenterX() + offset);
            tt.setToY(target[1] - token.getCenterY() + offset);
            tt.play();
        }
    }

    private Button createLuxBtn(String t, String color) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15;");
        b.setMinWidth(100);
        return b;
    }

    private void showTradeDialog() {
        TextInputDialog d = new TextInputDialog("2 500");
        d.setHeaderText("TRADE: [TargetID] [Amount]");
        d.showAndWait().ifPresent(v -> client.sendMessage("TRADE " + v));
    }

    private void handleTradeUI(String json) {
        int from = Integer.parseInt(getJsonVal(json, "from"));
        int cash = Integer.parseInt(getJsonVal(json, "cash"));
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "P" + from + " offers $" + cash + ". Accept?", ButtonType.YES, ButtonType.NO);
        if (a.showAndWait().get() == ButtonType.YES) client.sendMessage("ACCEPT_TRADE");
    }

    private double[] getTilePosition(int i) {
        if (i <= 10) return new double[]{BOARD_SIZE - TILE_SIZE - (i * TILE_SIZE), BOARD_SIZE - TILE_SIZE};
        if (i <= 20) return new double[]{0, BOARD_SIZE - TILE_SIZE - ((i - 10) * TILE_SIZE)};
        if (i <= 30) return new double[]{(i - 20) * TILE_SIZE, 0};
        return new double[]{BOARD_SIZE - TILE_SIZE, (i - 30) * TILE_SIZE};
    }

    private Image getCorrectIcon(int i) {
        if (i == 0) return iconCache.get("go"); if (i == 10) return iconCache.get("jail");
        if (i == 20) return iconCache.get("parking"); if (i == 30) return iconCache.get("gotojail");
        if (i == 2 || i == 17 || i == 33) return iconCache.get("chest");
        if (i == 7 || i == 22 || i == 36) return iconCache.get("chance");
        if (i == 4 || i == 38) return iconCache.get("tax");
        if (i == 12 || i == 28) return iconCache.get("utility");
        if (i == 5 || i == 15 || i == 25 || i == 35) return iconCache.get("train");
        return null;
    }

    private void connectToServer() {
        new Thread(() -> {
            client = new NetworkClient();
            client.setOnMessageReceived(this::processMessage);
            try { client.connect("localhost", 8080); } catch (Exception e) {}
        }).start();
    }

    private String getJsonVal(String j, String k) {
        try {
            String p = "\"" + k + "\":"; int s = j.indexOf(p) + p.length();
            if (j.charAt(s) == '\"') return j.substring(s + 1, j.indexOf("\"", s + 1));
            int e = j.indexOf(",", s); if (e == -1) e = j.indexOf("}", s);
            return j.substring(s, e).trim();
        } catch (Exception e) { return ""; }
    }
}
