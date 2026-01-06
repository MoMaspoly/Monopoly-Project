package ir.monopoly.client;

import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class MonopolyGUI extends Application {

    private NetworkClient client;

    // --- المان‌های رابط کاربری ---
    private TextArea logArea;
    private Label statusLabel;
    private Label playerInfoLabel;
    private Label balanceLabel; // نمایش پول
    private ListView<String> propertyList; // نمایش املاک

    private Button btnRoll, btnBuy, btnEndTurn;
    private Button btnBuild, btnMortgage, btnTrade;

    private int myPlayerId = -1;
    private boolean hasRolledThisTurn = false; // جلوگیری از تاس مجدد

    private final Map<Integer, double[]> tileCoordinates = new HashMap<>();
    private final Map<Integer, Circle> playerTokens = new HashMap<>();
    private Pane boardPane;

    private static final double BOARD_SIZE = 650;
    private static final double TILE_SIZE = BOARD_SIZE / 11.0;

    private static final Color BOARD_TILE_COLOR = Color.web("#FDF5E6");
    private static final Color BORDER_COLOR = Color.web("#2F4F4F");

    private Image bgImage, centerImage;
    private Map<String, Image> iconImages = new HashMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        loadImages();
        BorderPane root = new BorderPane();

        // 1. پس‌زمینه
        if (bgImage != null) {
            BackgroundImage bg = new BackgroundImage(bgImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, new BackgroundSize(1.0, 1.0, true, true, false, true));
            root.setBackground(new Background(bg));
        } else {
            root.setStyle("-fx-background-color: #0F3B2E;");
        }

        // 2. برد بازی
        boardPane = new Pane();
        boardPane.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        boardPane.setStyle("-fx-background-color: rgba(253, 245, 230, 0.9); -fx-background-radius: 15; -fx-border-color: #D4AF37; -fx-border-width: 4; -fx-border-radius: 15;");

        // رسم کامل برد (این بار کدش هست!)
        drawProBoard(boardPane);
        addCenterImageDisplay(boardPane);
        initPlayerTokens(boardPane);

        StackPane centerContainer = new StackPane(boardPane);
        centerContainer.setPrefSize(BOARD_SIZE + 40, BOARD_SIZE + 40);
        root.setCenter(centerContainer);

        // 3. پنل راست (داشبورد اطلاعات)
        VBox rightPanel = createRightPanel();
        root.setRight(rightPanel);

        // 4. پنل پایین (دکمه‌ها)
        HBox bottomPanel = createBottomPanel();
        root.setBottom(bottomPanel);

        // --- اکشن دکمه‌ها ---

        btnRoll.setOnAction(e -> {
            if (!hasRolledThisTurn) {
                sendToServer("ROLL");
                btnRoll.setDisable(true); // قفل کردن دکمه
                hasRolledThisTurn = true;
                btnBuy.setDisable(false); // فعال شدن خرید بعد از حرکت
                btnEndTurn.setDisable(false); // فعال شدن پایان نوبت
            }
        });

        btnBuy.setOnAction(e -> sendToServer("BUY"));

        btnEndTurn.setOnAction(e -> {
            sendToServer("END_TURN");
            disableControls(true); // قفل کردن همه چیز تا نوبت بعدی
        });

        btnBuild.setOnAction(e -> sendToServer("BUILD_REQUEST"));
        btnMortgage.setOnAction(e -> sendToServer("MORTGAGE_REQUEST"));
        btnTrade.setOnAction(e -> showTradeDialog());

        Scene scene = new Scene(root, 1080, 800);
        primaryStage.setTitle("Monopoly Game 🎄");
        primaryStage.setScene(scene);
        primaryStage.show();

        connectToServerInBackGround();
    }

    // --- ساخت پنل راست (اطلاعات بازیکن) ---
    private VBox createRightPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setPrefWidth(280);
        box.setStyle("-fx-background-color: rgba(255, 250, 240, 0.95); -fx-border-color: #8B0000; -fx-border-width: 0 0 0 4;");

        Label title = new Label("📊 Player Dashboard");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#8B0000"));

        playerInfoLabel = new Label("Connecting...");
        playerInfoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        // نمایش پول
        balanceLabel = new Label("💰 Balance: $1500");
        balanceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        balanceLabel.setTextFill(Color.GREEN);
        balanceLabel.setStyle("-fx-border-color: green; -fx-padding: 5; -fx-border-radius: 5;");
        balanceLabel.setMaxWidth(Double.MAX_VALUE);
        balanceLabel.setAlignment(Pos.CENTER);

        // لیست املاک
        Label propTitle = new Label("🏠 My Properties:");
        propTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        propertyList = new ListView<>();
        propertyList.setPrefHeight(150);
        propertyList.setStyle("-fx-background-color: transparent;");

        // لاگ بازی
        Label logTitle = new Label("📜 Game Log:");
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(200);

        statusLabel = new Label("Waiting...");
        statusLabel.setTextFill(Color.GRAY);

        box.getChildren().addAll(title, playerInfoLabel, balanceLabel, new Separator(), propTitle, propertyList, new Separator(), logTitle, logArea, statusLabel);
        return box;
    }

    private HBox createBottomPanel() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPrefHeight(90);
        box.setStyle("-fx-background-color: linear-gradient(to bottom, #8B0000, #500000); -fx-border-color: #D4AF37; -fx-border-width: 3 0 0 0;");

        btnRoll = createLuxuryButton("🎲 ROLL");
        btnBuy = createLuxuryButton("🏠 BUY");
        btnEndTurn = createLuxuryButton("⏭ END TURN");

        btnBuild = createLuxuryButton("🔨 BUILD");
        btnMortgage = createLuxuryButton("🏦 MORTGAGE");
        btnTrade = createLuxuryButton("🤝 TRADE");

        // رنگ‌بندی خاص
        btnBuy.setStyle("-fx-background-color: #4682B4; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
        btnEndTurn.setStyle("-fx-background-color: #2F4F4F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");

        disableControls(true); // اول بازی دکمه‌ها خاموش

        box.getChildren().addAll(btnRoll, btnBuy, btnBuild, btnMortgage, btnTrade, btnEndTurn);
        return box;
    }

    // --- پردازش پیام‌های سرور ---
    private void processMessage(String json) {
        Platform.runLater(() -> {
            try {
                String type = extractJsonValue(json, "type");
                String message = extractJsonValue(json, "message");

                if (message != null && !message.isEmpty()) logArea.appendText("➤ " + message + "\n");

                switch (type) {
                    case "CONNECTED":
                        myPlayerId = Integer.parseInt(extractJsonValue(json, "playerId"));
                        playerInfoLabel.setText("👤 You are Player " + myPlayerId);
                        statusLabel.setText("Connected!");
                        break;

                    case "TURN_UPDATE":
                        int currentPlayer = Integer.parseInt(extractJsonValue(json, "currentPlayer"));
                        if (currentPlayer == myPlayerId) {
                            statusLabel.setText("⭐ YOUR TURN! Roll the Dice ⭐");
                            statusLabel.setTextFill(Color.RED);
                            // فعال کردن دکمه‌ها برای نوبت جدید
                            hasRolledThisTurn = false;
                            btnRoll.setDisable(false);
                            btnBuy.setDisable(true); // خرید فقط بعد از حرکت فعال میشه
                            btnEndTurn.setDisable(true); // پایان نوبت فقط بعد از حرکت

                            // دکمه‌های مدیریتی همیشه فعال در نوبت
                            btnBuild.setDisable(false);
                            btnMortgage.setDisable(false);
                            btnTrade.setDisable(false);
                        } else {
                            statusLabel.setText("⏳ Waiting for Player " + currentPlayer);
                            statusLabel.setTextFill(Color.GRAY);
                            disableControls(true);
                        }
                        break;

                    case "ROLL_UPDATE":
                        int pId = Integer.parseInt(extractJsonValue(json, "playerId"));
                        int pos = Integer.parseInt(extractJsonValue(json, "currentPosition"));
                        moveToken(pId, pos);
                        break;

                    case "PLAYER_STATS":
                        if (Integer.parseInt(extractJsonValue(json, "playerId")) == myPlayerId) {
                            String bal = extractJsonValue(json, "balance");
                            balanceLabel.setText("💰 Balance: $" + bal);
                        }
                        break;

                    case "BUY_UPDATE":
                        boolean success = Boolean.parseBoolean(extractJsonValue(json, "success"));
                        if (success && Integer.parseInt(extractJsonValue(json, "playerId")) == myPlayerId) {
                            String propName = message.replace("Bought ", "");
                            propertyList.getItems().add(propName);
                        }
                        break;

                    case "SHOW_CARD":
                        showCardPopup(extractJsonValue(json, "text"));
                        break;

                    case "HOUSE_BUILT":
                        int tId = Integer.parseInt(extractJsonValue(json, "tileId"));
                        int count = Integer.parseInt(extractJsonValue(json, "count"));
                        drawHouseOnTile(tId, count);
                        break;
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // --- رسم گرافیکی خانه و هتل ---
    private void drawHouseOnTile(int tileIndex, int count) {
        Platform.runLater(() -> {
            if (!tileCoordinates.containsKey(tileIndex)) return;
            double[] pos = tileCoordinates.get(tileIndex);
            Rectangle house = new Rectangle(12, 12);
            if (count < 5) {
                house.setFill(Color.LIMEGREEN); house.setStroke(Color.BLACK);
            } else {
                house.setFill(Color.RED); house.setWidth(16); house.setHeight(16);
            }
            house.setX(pos[0] - 6); house.setY(pos[1] - (TILE_SIZE/2) + 5);
            boardPane.getChildren().add(house);
        });
    }

    // --- لود تصاویر ---
    private void loadImages() {
        bgImage = safeLoad("assets/main_bg.jpg");
        centerImage = safeLoad("assets/center_board.png"); // فرمت را چک کن
        String[] iconNames = {"go", "jail", "parking", "gotojail", "chest", "chance", "tax", "utility", "train"};
        for (String name : iconNames) {
            Image img = safeLoad("assets/icon_" + name + ".png");
            if (img != null) iconImages.put(name, img);
        }
    }
    private Image safeLoad(String path) {
        try { return new Image(new FileInputStream(path)); } catch (Exception e) { return null; }
    }

    // --- رسم کامل برد (بخش گم شده اضافه شد) ---
    private void drawProBoard(Pane pane) {
        for (int i = 0; i < 40; i++) {
            double x = 0, y = 0;
            if (i < 10) { x = BOARD_SIZE - (TILE_SIZE * (i + 1)); y = BOARD_SIZE - TILE_SIZE; }
            else if (i < 20) { x = 0; y = BOARD_SIZE - (TILE_SIZE * (i - 10 + 1)); }
            else if (i < 30) { x = TILE_SIZE * (i - 20); y = 0; }
            else { x = BOARD_SIZE - TILE_SIZE; y = TILE_SIZE * (i - 30); }

            Rectangle rect = new Rectangle(x, y, TILE_SIZE, TILE_SIZE);
            rect.setFill(BOARD_TILE_COLOR); rect.setStroke(BORDER_COLOR); rect.setStrokeWidth(1);

            Rectangle colorBar = new Rectangle();
            Color tileColor = getTileColor(i);

            if (tileColor != null) {
                double barThickness = TILE_SIZE / 4.5;
                if (i >= 10 && i < 20) { colorBar.setWidth(barThickness); colorBar.setHeight(TILE_SIZE); colorBar.setX(x + TILE_SIZE - barThickness); colorBar.setY(y); }
                else if (i >= 30) { colorBar.setWidth(barThickness); colorBar.setHeight(TILE_SIZE); colorBar.setX(x); colorBar.setY(y); }
                else if (i >= 20 && i < 30) { colorBar.setWidth(TILE_SIZE); colorBar.setHeight(barThickness); colorBar.setX(x); colorBar.setY(y + TILE_SIZE - barThickness); }
                else { colorBar.setWidth(TILE_SIZE); colorBar.setHeight(barThickness); colorBar.setX(x); colorBar.setY(y); }
                colorBar.setFill(tileColor); colorBar.setStroke(Color.BLACK); colorBar.setStrokeWidth(0.5);
            } else { colorBar.setVisible(false); }

            Text textIndex = new Text(x + 4, y + 10, String.valueOf(i));
            textIndex.setFont(Font.font("Arial", 8)); textIndex.setFill(Color.GRAY);

            pane.getChildren().addAll(rect, colorBar, textIndex);

            Image icon = getTileImage(i);
            if (icon != null) {
                ImageView iv = new ImageView(icon);
                double iconSize = TILE_SIZE * 0.6;
                iv.setFitWidth(iconSize); iv.setFitHeight(iconSize); iv.setPreserveRatio(true);
                iv.setLayoutX(x + (TILE_SIZE - iconSize)/2); iv.setLayoutY(y + (TILE_SIZE - iconSize)/2);
                pane.getChildren().add(iv);
            }
            tileCoordinates.put(i, new double[]{x + TILE_SIZE / 2, y + TILE_SIZE / 2});
        }
    }

    private void addCenterImageDisplay(Pane pane) {
        double centerSpace = BOARD_SIZE - (2 * TILE_SIZE);
        double padding = 20;
        Rectangle centerRect = new Rectangle(TILE_SIZE + padding, TILE_SIZE + padding, centerSpace - (2 * padding), centerSpace - (2 * padding));
        centerRect.setArcWidth(30); centerRect.setArcHeight(30);

        if (centerImage != null) {
            centerRect.setFill(new ImagePattern(centerImage));
            centerRect.setEffect(new InnerShadow(10, Color.rgb(0,0,0,0.5)));
        } else {
            centerRect.setFill(Color.TRANSPARENT);
            Text fallback = new Text(BOARD_SIZE/2 - 80, BOARD_SIZE/2, "Merry\nMonopoly");
            fallback.setFont(Font.font(40)); fallback.setRotate(-45); fallback.setFill(Color.RED);
            pane.getChildren().add(fallback);
        }
        centerRect.setStroke(Color.web("#D4AF37")); centerRect.setStrokeWidth(3);
        DropShadow glow = new DropShadow(); glow.setColor(Color.GOLD); glow.setRadius(15);
        centerRect.setEffect(glow);
        pane.getChildren().add(centerRect);
    }

    private void initPlayerTokens(Pane pane) {
        Color[] colors = {Color.RED, Color.CYAN, Color.LIME, Color.GOLD};
        for (int i = 1; i <= 4; i++) {
            Circle token = new Circle(9, colors[i-1]);
            token.setStroke(Color.WHITE); token.setStrokeWidth(1.5); token.setEffect(new DropShadow(3, Color.BLACK));
            double[] startPos = tileCoordinates.get(0);
            if (startPos != null) positionToken(token, startPos, i);
            playerTokens.put(i, token); pane.getChildren().add(token);
        }
    }

    private void moveToken(int playerId, int tileIndex) {
        if (playerTokens.containsKey(playerId) && tileCoordinates.containsKey(tileIndex)) {
            Circle token = playerTokens.get(playerId);
            double[] targetPos = tileCoordinates.get(tileIndex);
            TranslateTransition tt = new TranslateTransition(Duration.seconds(0.5), token);
            token.setTranslateX(0); token.setTranslateY(0); positionToken(token, targetPos, playerId);
            tt.play();
        }
    }

    private void positionToken(Circle token, double[] pos, int playerId) {
        double offsetX = (playerId % 2 == 0) ? 6 : -6; double offsetY = (playerId > 2) ? 6 : -6;
        token.setCenterX(pos[0] + offsetX); token.setCenterY(pos[1] + offsetY);
    }

    // --- سایر متدها ---
    private void disableControls(boolean disable) {
        btnRoll.setDisable(disable); btnBuy.setDisable(disable); btnEndTurn.setDisable(disable);
        btnBuild.setDisable(disable); btnMortgage.setDisable(disable); btnTrade.setDisable(disable);
        double op = disable ? 0.6 : 1.0;
        btnRoll.setOpacity(op); btnBuy.setOpacity(op); btnEndTurn.setOpacity(op);
        btnBuild.setOpacity(op); btnMortgage.setOpacity(op); btnTrade.setOpacity(op);
    }
    private void connectToServerInBackGround() {
        new Thread(() -> {
            client = new NetworkClient();
            client.setOnMessageReceived(this::processMessage);
            try { client.connect("localhost", 8080); } catch (IOException e) {
                Platform.runLater(() -> statusLabel.setText("❌ Server Not Found"));
            }
        }).start();
    }
    private void sendToServer(String msg) { if (client != null) client.sendMessage(msg); }
    private Button createLuxuryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: linear-gradient(#D42426, #8B0000); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
        btn.setMinWidth(100);
        return btn;
    }
    private void showCardPopup(String t) {
        Platform.runLater(() -> { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setContentText(t); a.show(); });
    }
    private void showTradeDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Trade"); dialog.setHeaderText("Propose Trade");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(trade -> sendToServer("TRADE " + trade));
    }
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":"; int start = json.indexOf(searchKey); if (start == -1) return ""; start += searchKey.length();
        char firstChar = json.charAt(start); if (firstChar == '"') { int end = json.indexOf("\"", start + 1); return json.substring(start + 1, end); } else { int end = json.indexOf(",", start); if (end == -1) end = json.indexOf("}", start); return json.substring(start, end).trim(); }
    }
    private Image getTileImage(int i) {
        if (i == 0) return iconImages.get("go"); if (i == 10) return iconImages.get("jail");
        if (i == 20) return iconImages.get("parking"); if (i == 30) return iconImages.get("gotojail");
        if (i == 2 || i == 17 || i == 33) return iconImages.get("chest");
        if (i == 7 || i == 22 || i == 36) return iconImages.get("chance");
        if (i == 4 || i == 38) return iconImages.get("tax");
        if (i == 12 || i == 28) return iconImages.get("utility");
        if (i == 5 || i == 15 || i == 25 || i == 35) return iconImages.get("train");
        return null;
    }
    private Color getTileColor(int i) {
        if (i == 1 || i == 3) return Color.web("#8B4513"); if (i == 6 || i == 8 || i == 9) return Color.web("#87CEEB");
        if (i == 11 || i == 13 || i == 14) return Color.web("#DB7093"); if (i == 16 || i == 18 || i == 19) return Color.web("#FFA500");
        if (i == 21 || i == 23 || i == 24) return Color.web("#B22222"); if (i == 26 || i == 27 || i == 29) return Color.web("#FFD700");
        if (i == 31 || i == 32 || i == 34) return Color.web("#228B22"); if (i == 37 || i == 39) return Color.web("#00008B");
        return null;
    }
}
