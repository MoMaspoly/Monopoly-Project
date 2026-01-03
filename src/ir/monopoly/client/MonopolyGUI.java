package ir.monopoly.client;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
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
import java.util.Random;

public class MonopolyGUI extends Application {

    private NetworkClient client;
    private TextArea logArea;
    private Label statusLabel;
    private Label playerInfoLabel;
    private Button btnRoll, btnBuy, btnEndTurn;
    private int myPlayerId = -1;

    private final Map<Integer, double[]> tileCoordinates = new HashMap<>();
    private final Map<Integer, Circle> playerTokens = new HashMap<>();

    // تنظیمات سایز
    private static final double BOARD_SIZE = 650; // برد کمی بزرگتر
    private static final double TILE_SIZE = BOARD_SIZE / 11.0;

    // رنگ‌های گرم‌تر و هماهنگ با عکس
    private static final Color BOARD_TILE_COLOR = Color.web("#FDF5E6"); // کرم (Old Lace)
    private static final Color BORDER_COLOR = Color.web("#2F4F4F");     // سبز لجنی تیره

    private Image bgImage, centerImage;
    private Map<String, Image> iconImages = new HashMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        loadImages(); // بارگذاری عکس‌ها

        BorderPane root = new BorderPane();

        // 1. تنظیم پس‌زمینه (Background) به صورت Full Cover
        if (bgImage != null) {
            BackgroundImage bg = new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(1.0, 1.0, true, true, false, true) // حالت Cover
            );
            root.setBackground(new Background(bg));
        } else {
            root.setStyle("-fx-background-color: #0F3B2E;");
        }

        // 2. پنل وسط (برد بازی)
        Pane boardPane = new Pane();
        boardPane.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        // استایل قاب برد (شفاف‌تر)
        boardPane.setStyle(
                "-fx-background-color: rgba(253, 245, 230, 0.85);" + // کرم نیمه شفاف
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #D4AF37; -fx-border-width: 4; -fx-border-radius: 15;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 30, 0, 0, 0);"
        );

        drawProBoard(boardPane);
        addCenterImageDisplay(boardPane); // عکس وسط اصلاح شده
        initPlayerTokens(boardPane);

        StackPane centerContainer = new StackPane(boardPane);
        centerContainer.setPrefSize(BOARD_SIZE + 40, BOARD_SIZE + 40);
        addSnowEffect(centerContainer); // برف ملایم
        root.setCenter(centerContainer);

        // 3. پنل راست
        VBox rightPanel = new VBox(10);
        rightPanel.setStyle("-fx-padding: 15; -fx-background-color: rgba(255, 250, 240, 0.9); -fx-border-color: #8B0000; -fx-border-width: 0 0 0 4;");
        rightPanel.setPrefWidth(280);

        Label headerLabel = new Label("🎅 Santa's Log");
        headerLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        headerLabel.setTextFill(Color.web("#8B0000"));

        playerInfoLabel = new Label("Connecting...");
        playerInfoLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F3B2E;");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(400);
        logArea.setStyle("-fx-control-inner-background: #FFFBF0; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px;");

        statusLabel = new Label("Waiting for server...");
        statusLabel.setTextFill(Color.DARKGRAY);
        statusLabel.setFont(Font.font(12));

        rightPanel.getChildren().addAll(headerLabel, playerInfoLabel, new Separator(), logArea, statusLabel);
        root.setRight(rightPanel);

        // 4. پنل پایین
        HBox bottomPanel = new HBox(25);
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPrefHeight(90);
        // گرادینت قرمز شیک
        bottomPanel.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(139,0,0,0.95), rgba(80,0,0,0.95)); -fx-border-color: #D4AF37; -fx-border-width: 3 0 0 0;");

        btnRoll = createLuxuryButton("🎲 ROLL");
        btnBuy = createLuxuryButton("🏠 BUY");
        btnEndTurn = createLuxuryButton("⏭ END TURN");

        disableControls(true);

        bottomPanel.getChildren().addAll(btnRoll, btnBuy, btnEndTurn);
        root.setBottom(bottomPanel);

        // اکشن‌ها
        btnRoll.setOnAction(e -> sendToServer("ROLL"));
        btnBuy.setOnAction(e -> sendToServer("BUY"));
        btnEndTurn.setOnAction(e -> sendToServer("END_TURN"));

        Scene scene = new Scene(root, 1000, 750);
        primaryStage.setTitle("Monopoly Christmas Edition 🎄");
        primaryStage.setScene(scene);
        primaryStage.show();

        connectToServerInBackGround();
    }

    // --- لودر هوشمند عکس ---
    private void loadImages() {
        // سعی می‌کنیم عکس‌ها را بخوانیم، اگر نبود رنگ خالی می‌گذاریم که کرش نکند
        bgImage = safeLoad("assets/main_bg.jpg");
        centerImage = safeLoad("assets/center_board.jpg"); // یا .jpg

        String[] iconNames = {"go", "jail", "parking", "gotojail", "chest", "chance", "tax", "utility", "train"};
        for (String name : iconNames) {
            Image img = safeLoad("assets/icon_" + name + ".jpg");
            if (img != null) iconImages.put(name, img);
        }
    }

    private Image safeLoad(String path) {
        try {
            return new Image(new FileInputStream(path));
        } catch (Exception e) {
            return null;
        }
    }

    // --- نمایش عکس وسط (اصلاح شده) ---
    private void addCenterImageDisplay(Pane pane) {
        double centerSpace = BOARD_SIZE - (2 * TILE_SIZE);
        double padding = 20; // فاصله از خانه‌ها

        // 1. ساخت یک مستطیل برای وسط صفحه
        Rectangle centerRect = new Rectangle(
                TILE_SIZE + padding,
                TILE_SIZE + padding,
                centerSpace - (2 * padding),
                centerSpace - (2 * padding)
        );
        centerRect.setArcWidth(30); // گوشه‌های گرد
        centerRect.setArcHeight(30);

        // 2. پر کردن مستطیل با عکس (ImagePattern خودکار عکس را فیت می‌کند)
        if (centerImage != null) {
            centerRect.setFill(new ImagePattern(centerImage));
            // افکت سایه داخلی برای عمق
            centerRect.setEffect(new InnerShadow(10, Color.rgb(0,0,0,0.5)));
        } else {
            // اگر عکس نبود، متن بنویس
            centerRect.setFill(Color.TRANSPARENT);
            Text fallback = new Text(BOARD_SIZE/2 - 80, BOARD_SIZE/2, "Merry\nMonopoly");
            fallback.setFont(Font.font(40));
            fallback.setRotate(-45);
            fallback.setFill(Color.RED);
            pane.getChildren().add(fallback);
        }

        // 3. اضافه کردن قاب طلایی دور عکس
        centerRect.setStroke(Color.web("#D4AF37"));
        centerRect.setStrokeWidth(3);

        // افکت درخشش دور قاب
        DropShadow glow = new DropShadow();
        glow.setColor(Color.GOLD);
        glow.setRadius(15);
        centerRect.setEffect(glow);

        pane.getChildren().add(centerRect);
    }

    // --- رسم برد ---
    private void drawProBoard(Pane pane) {
        for (int i = 0; i < 40; i++) {
            double x = 0, y = 0;
            if (i < 10) { x = BOARD_SIZE - (TILE_SIZE * (i + 1)); y = BOARD_SIZE - TILE_SIZE; }
            else if (i < 20) { x = 0; y = BOARD_SIZE - (TILE_SIZE * (i - 10 + 1)); }
            else if (i < 30) { x = TILE_SIZE * (i - 20); y = 0; }
            else { x = BOARD_SIZE - TILE_SIZE; y = TILE_SIZE * (i - 30); }

            // خانه بازی (کرم رنگ)
            Rectangle rect = new Rectangle(x, y, TILE_SIZE, TILE_SIZE);
            rect.setFill(BOARD_TILE_COLOR);
            rect.setStroke(BORDER_COLOR);
            rect.setStrokeWidth(1);

            // نوار رنگی (باریک‌تر و شیک‌تر)
            Rectangle colorBar = new Rectangle();
            Color tileColor = getTileColor(i);

            if (tileColor != null) {
                double barThickness = TILE_SIZE / 4.5;
                if (i >= 10 && i < 20) { // چپ
                    colorBar.setWidth(barThickness); colorBar.setHeight(TILE_SIZE); colorBar.setX(x + TILE_SIZE - barThickness); colorBar.setY(y);
                } else if (i >= 30) { // راست
                    colorBar.setWidth(barThickness); colorBar.setHeight(TILE_SIZE); colorBar.setX(x); colorBar.setY(y);
                } else if (i >= 20 && i < 30) { // بالا
                    colorBar.setWidth(TILE_SIZE); colorBar.setHeight(barThickness); colorBar.setX(x); colorBar.setY(y + TILE_SIZE - barThickness);
                } else { // پایین
                    colorBar.setWidth(TILE_SIZE); colorBar.setHeight(barThickness); colorBar.setX(x); colorBar.setY(y);
                }
                colorBar.setFill(tileColor);
                colorBar.setStroke(Color.BLACK); colorBar.setStrokeWidth(0.5);
            } else { colorBar.setVisible(false); }

            // شماره خانه (کوچک و گوشه)
            Text textIndex = new Text(x + 4, y + 10, String.valueOf(i));
            textIndex.setFont(Font.font("Arial", 8));
            textIndex.setFill(Color.GRAY);

            pane.getChildren().addAll(rect, colorBar, textIndex);

            // --- اضافه کردن آیکون ---
            Image icon = getTileImage(i);
            if (icon != null) {
                ImageView iv = new ImageView(icon);
                // سایز آیکون را محدود می‌کنیم تا کل خانه را نگیرد
                double iconSize = TILE_SIZE * 0.6;
                iv.setFitWidth(iconSize);
                iv.setFitHeight(iconSize);
                iv.setPreserveRatio(true);

                // وسط چین کردن آیکون در خانه
                iv.setLayoutX(x + (TILE_SIZE - iconSize)/2);
                iv.setLayoutY(y + (TILE_SIZE - iconSize)/2);

                pane.getChildren().add(iv);
            }

            tileCoordinates.put(i, new double[]{x + TILE_SIZE / 2, y + TILE_SIZE / 2});
        }
    }

    // --- تصویر مربوط به هر خانه ---
    private Image getTileImage(int i) {
        if (i == 0) return iconImages.get("go");
        if (i == 10) return iconImages.get("jail");
        if (i == 20) return iconImages.get("parking");
        if (i == 30) return iconImages.get("gotojail");
        if (i == 2 || i == 17 || i == 33) return iconImages.get("chest");
        if (i == 7 || i == 22 || i == 36) return iconImages.get("chance");
        if (i == 4 || i == 38) return iconImages.get("tax");
        if (i == 12 || i == 28) return iconImages.get("utility");
        if (i == 5 || i == 15 || i == 25 || i == 35) return iconImages.get("train");
        return null;
    }

    private Button createLuxuryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: linear-gradient(#D42426, #8B0000); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; " +
                        "-fx-background-radius: 20; -fx-border-color: #D4AF37; -fx-border-radius: 20; -fx-border-width: 2;"
        );
        btn.setMinWidth(120);
        btn.setEffect(new DropShadow(5, Color.BLACK));
        return btn;
    }

    // --- لاجیک ---
    private void processMessage(String json) {
        Platform.runLater(() -> {
            try {
                String type = extractJsonValue(json, "type");
                String message = extractJsonValue(json, "message");
                if (message != null && !message.isEmpty()) logArea.appendText("🎁 " + message + "\n");
                switch (type) {
                    case "CONNECTED":
                        String pIdStr = extractJsonValue(json, "playerId");
                        if(!pIdStr.isEmpty()) { myPlayerId = Integer.parseInt(pIdStr); playerInfoLabel.setText("👤 You are Player " + myPlayerId); statusLabel.setText("Connected!"); }
                        break;
                    case "TURN_UPDATE":
                        int currentPlayer = Integer.parseInt(extractJsonValue(json, "currentPlayer"));
                        if (currentPlayer == myPlayerId) { statusLabel.setText("⭐ YOUR TURN! ⭐"); statusLabel.setTextFill(Color.web("#8B0000")); disableControls(false); }
                        else { statusLabel.setText("Turn: Player " + currentPlayer); statusLabel.setTextFill(Color.GRAY); disableControls(true); }
                        break;
                    case "ROLL_UPDATE":
                        moveToken(Integer.parseInt(extractJsonValue(json, "playerId")), Integer.parseInt(extractJsonValue(json, "currentPosition"))); break;
                }
            } catch (Exception e) { /* Ignore */ }
        });
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
        }
    }
    private void positionToken(Circle token, double[] pos, int playerId) {
        double offsetX = (playerId % 2 == 0) ? 6 : -6; double offsetY = (playerId > 2) ? 6 : -6;
        token.setCenterX(pos[0] + offsetX); token.setCenterY(pos[1] + offsetY);
    }
    private void connectToServerInBackGround() {
        new Thread(() -> {
            client = new NetworkClient();
            client.setOnMessageReceived(this::processMessage);
            try { client.connect("localhost", 8080); } catch (IOException e) { Platform.runLater(() -> statusLabel.setText("❌ Server Not Found")); }
        }).start();
    }
    private void sendToServer(String msg) { if (client != null) client.sendMessage(msg); }
    private void disableControls(boolean disable) {
        btnRoll.setDisable(disable); btnBuy.setDisable(disable); btnEndTurn.setDisable(disable);
        double op = disable ? 0.6 : 1.0; btnRoll.setOpacity(op); btnBuy.setOpacity(op); btnEndTurn.setOpacity(op);
    }
    private void addSnowEffect(Pane pane) {
        Random rand = new Random();
        for (int i = 0; i < 50; i++) {
            Circle snow = new Circle(rand.nextInt(3) + 1, Color.WHITE);
            snow.setOpacity(0.5); snow.setTranslateX(rand.nextInt((int)BOARD_SIZE)); snow.setTranslateY(rand.nextInt((int)BOARD_SIZE));
            pane.getChildren().add(snow);
        }
    }
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":"; int start = json.indexOf(searchKey); if (start == -1) return ""; start += searchKey.length();
        char firstChar = json.charAt(start); if (firstChar == '"') { int end = json.indexOf("\"", start + 1); return json.substring(start + 1, end); } else { int end = json.indexOf(",", start); if (end == -1) end = json.indexOf("}", start); return json.substring(start, end).trim(); }
    }
    private Color getTileColor(int i) {
        if (i == 1 || i == 3) return Color.web("#8B4513"); if (i == 6 || i == 8 || i == 9) return Color.web("#87CEEB");
        if (i == 11 || i == 13 || i == 14) return Color.web("#DB7093"); if (i == 16 || i == 18 || i == 19) return Color.web("#FFA500");
        if (i == 21 || i == 23 || i == 24) return Color.web("#B22222"); if (i == 26 || i == 27 || i == 29) return Color.web("#FFD700");
        if (i == 31 || i == 32 || i == 34) return Color.web("#228B22"); if (i == 37 || i == 39) return Color.web("#00008B");
        return null;
    }
}
