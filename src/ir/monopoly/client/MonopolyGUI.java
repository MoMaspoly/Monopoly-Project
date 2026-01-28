package ir.monopoly.client;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class MonopolyGUI extends Application {
    private NetworkClient client;
    private int myPlayerId = -1;
    private boolean hasRolledThisTurn = false;
    private boolean isMyTurn = false;
    private boolean auctionActive = false;
    private boolean awaitingBuyDecision = false;
    private boolean inJail = false;
    private int jailTurns = 0;
    private int currentBidderId = -1;

    private Pane boardPane;
    private TextArea logArea;
    private Label balanceLabel, playerInfoLabel, statusLabel, auctionStatusLabel, jailStatusLabel;
    private ListView<String> propertyList;
    private Button btnRoll, btnBuy, btnTrade, btnEndTurn, btnUndo, btnRedo, btnLeaderboard;
    private Button btnPass, btnBid;
    private Button btnJailDouble, btnJailPay, btnJailCard;
    private TextField bidAmountField;
    private VBox auctionPanel;
    private VBox jailPanel;

    private final Map<Integer, double[]> tileCoords = new HashMap<>();
    private final Map<Integer, Circle> playerTokens = new HashMap<>();
    private final Map<String, Image> iconCache = new HashMap<>();
    private Image bgImage, centerImage;

    private static final double BOARD_SIZE = 680;
    private static final double TILE_SIZE = BOARD_SIZE / 11.0;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) {

        loadAssets();

        VBox root = new VBox();
        root.setSpacing(0);

        if (bgImage != null) {
            root.setBackground(new Background(new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(1, 1, true, true, false, true)
            )));
        }

        boardPane = new Pane();
        boardPane.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        boardPane.setMinSize(BOARD_SIZE, BOARD_SIZE);
        boardPane.setMaxSize(BOARD_SIZE, BOARD_SIZE);

        boardPane.setStyle("""
        -fx-background-color: rgba(253,245,230,0.9);
        -fx-border-color: #D4AF37;
        -fx-border-width: 4;
        -fx-background-radius: 15;
    """);

        drawFestiveBoard();
        addCenterArt();
        initTokens();

        StackPane boardWrapper = new StackPane(boardPane);
        boardWrapper.setAlignment(Pos.CENTER);
        boardWrapper.setPadding(Insets.EMPTY);

        VBox rightPanel = createRightPanel();

        HBox mainRow = new HBox();
        mainRow.setSpacing(0);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox.setHgrow(boardWrapper, Priority.NEVER);
        HBox.setHgrow(rightPanel, Priority.NEVER);

        mainRow.getChildren().addAll(boardWrapper, spacer, rightPanel);

        HBox controls = createControlPanel();

        VBox.setVgrow(mainRow, Priority.ALWAYS);

        root.getChildren().addAll(mainRow, controls);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        double width = Math.min(1000, screen.getWidth() - 50);
        double height = Math.min(850, screen.getHeight() - 50);

        Scene scene = new Scene(root, width, height);


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
        try { return new Image(new FileInputStream(p)); } catch (FileNotFoundException e) { return null; }
    }

    private VBox createRightPanel() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10, 10, 10, 20));
        box.setPrefWidth(320);
        box.setStyle("-fx-background-color: rgba(255, 255, 240, 0.85); -fx-background-radius: 15; -fx-border-color: #D4AF37; -fx-border-width: 0 0 0 4;");

        playerInfoLabel = new Label("Connecting...");
        playerInfoLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 18));

        balanceLabel = new Label("💰 Balance: $1500");
        balanceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        balanceLabel.setTextFill(Color.DARKGREEN);

        propertyList = new ListView<>();
        propertyList.setPrefHeight(120);
        propertyList.getItems().add("No properties owned");

        jailStatusLabel = new Label("You are FREE!");
        jailStatusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        jailStatusLabel.setTextFill(Color.DARKRED);

        btnJailDouble = new Button("🎲 Try Double");
        btnJailDouble.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
        btnJailDouble.setOnAction(e -> {
            client.sendMessage("ROLL");
            hasRolledThisTurn = true;
            updateControlStates();
        });

        btnJailPay = new Button("💰 Pay $50 Fine");
        btnJailPay.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnJailPay.setOnAction(e -> {
            client.sendMessage("ROLL");
            hasRolledThisTurn = true;
            updateControlStates();
        });

        btnJailCard = new Button("🃏 Use Jail Card");
        btnJailCard.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold;");
        btnJailCard.setOnAction(e -> {
            showAlert("Jail Card", "Use jail card feature needs server implementation");
        });

        HBox jailButtons = new HBox(10, btnJailDouble, btnJailPay, btnJailCard);
        jailButtons.setAlignment(Pos.CENTER);

        jailPanel = new VBox(10, jailStatusLabel, jailButtons);
        jailPanel.setPadding(new Insets(10));
        jailPanel.setStyle("-fx-background-color: rgba(255, 230, 230, 0.9); -fx-background-radius: 10; -fx-border-color: #c0392b; -fx-border-width: 2;");
        jailPanel.setVisible(false);

        auctionStatusLabel = new Label("No active auction");
        auctionStatusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        auctionStatusLabel.setTextFill(Color.PURPLE);
        auctionStatusLabel.setWrapText(true);

        bidAmountField = new TextField();
        bidAmountField.setPromptText("Bid amount");
        bidAmountField.setPrefWidth(100);

        btnBid = new Button("💵 BID");
        btnBid.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnBid.setOnAction(e -> {
            try {
                int amount = Integer.parseInt(bidAmountField.getText());
                client.sendMessage("BID " + amount);
                bidAmountField.clear();
            } catch (NumberFormatException ex) {
                showAlert("Invalid Bid", "Please enter a valid number!");
            }
        });

        btnPass = new Button("⏭ PASS");
        btnPass.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnPass.setOnAction(e -> client.sendMessage("PASS"));

        HBox auctionControls = new HBox(10, bidAmountField, btnBid, btnPass);
        auctionControls.setAlignment(Pos.CENTER);

        auctionPanel = new VBox(10, auctionStatusLabel, auctionControls);
        auctionPanel.setPadding(new Insets(10));
        auctionPanel.setStyle("-fx-background-color: rgba(255, 255, 200, 0.8); -fx-background-radius: 10; -fx-border-color: #9b59b6; -fx-border-width: 2;");
        auctionPanel.setVisible(false);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(150);

        statusLabel = new Label("Waiting for players...");
        statusLabel.setTextFill(Color.FIREBRICK);

        btnLeaderboard = new Button("🏆 LEADERBOARD");
        btnLeaderboard.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;");
        btnLeaderboard.setMaxWidth(Double.MAX_VALUE);
        btnLeaderboard.setOnAction(e -> client.sendMessage("GET_TOP_K"));

        box.getChildren().addAll(playerInfoLabel, balanceLabel, new Separator(),
                new Label("Properties:"), propertyList, new Separator(),
                jailPanel, new Separator(),
                auctionPanel, new Separator(),
                btnLeaderboard, new Separator(), new Label("Log:"), logArea, statusLabel);
        return box;
    }

    private HBox createControlPanel() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 20;");

        btnRoll = createBtn("🎲 ROLL", "#D4AF37");
        btnBuy = createBtn("🏠 BUY", "#27ae60");
        btnTrade = createBtn("🤝 TRADE", "#2980b9");
        btnUndo = createBtn("↶ UNDO", "#8e44ad");
        btnRedo = createBtn("↷ REDO", "#3498db");
        btnEndTurn = createBtn("⏭ END", "#c0392b");

        btnRoll.setOnAction(e -> {
            client.sendMessage("ROLL");
            hasRolledThisTurn = true;
            updateControlStates();
        });
        btnBuy.setOnAction(e -> client.sendMessage("BUY"));
        btnTrade.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("50");
            dialog.setTitle("Trade Proposal");
            dialog.setHeaderText("Propose a Trade");
            dialog.setContentText("Enter cash amount to offer:");
            dialog.showAndWait().ifPresent(amount -> {
                TextInputDialog targetDialog = new TextInputDialog("2");
                targetDialog.setTitle("Trade Target");
                targetDialog.setHeaderText("Select Trade Partner");
                targetDialog.setContentText("Enter target player ID (2, 3, or 4):");
                targetDialog.showAndWait().ifPresent(target -> {
                    client.sendMessage("TRADE " + target + " " + amount);
                });
            });
        });
        btnUndo.setOnAction(e -> client.sendMessage("UNDO"));
        btnRedo.setOnAction(e -> client.sendMessage("REDO"));
        btnEndTurn.setOnAction(e -> client.sendMessage("END_TURN"));

        updateControlStates();

        box.getChildren().addAll(btnRoll, btnBuy, btnTrade, btnUndo, btnRedo, btnEndTurn);
        return box;
    }

    private void processMessage(String json) {
        Platform.runLater(() -> {
            try {
                String type = getJsonVal(json, "type");
                String msg = getJsonVal(json, "message");

                switch (type) {
                    case "CONNECTED" -> {
                        myPlayerId = Integer.parseInt(getJsonVal(json, "playerId"));
                        playerInfoLabel.setText("👤 Player " + myPlayerId);
                        logArea.appendText("➤ Connected as Player " + myPlayerId + "\n");
                    }
                    case "TURN_UPDATE" -> {
                        int curr = Integer.parseInt(getJsonVal(json, "currentPlayer"));
                        isMyTurn = (curr == myPlayerId);

                        if (isMyTurn) hasRolledThisTurn = false;

                        updateControlStates();
                        statusLabel.setText(isMyTurn ? "⭐ YOUR TURN" : "Wait for P" + curr);

                        logArea.appendText("➤ Turn: Player " + curr + "\n");
                    }
                    case "ROLL_UPDATE" -> {
                        int pId = Integer.parseInt(getJsonVal(json, "playerId"));
                        int pos = Integer.parseInt(getJsonVal(json, "currentPosition"));
                        moveToken(pId, pos);
                        logArea.appendText("➤ Player " + pId + " moved to position " + pos + "\n");

                        if (pId == myPlayerId && pos == 10 && inJail) {
                            jailStatusLabel.setText("🔒 IN JAIL (Position: " + pos + ")");
                        }
                    }
                    case "PLAYER_STATS" -> {
                        int playerId = Integer.parseInt(getJsonVal(json, "playerId"));
                        int balance = Integer.parseInt(getJsonVal(json, "balance"));
                        if (playerId == myPlayerId) {
                            balanceLabel.setText("💰 Balance: $" + balance);
                        }
                    }
                    case "PLAYER_STATUS" -> {
                        int playerId = Integer.parseInt(getJsonVal(json, "playerId"));
                        String status = getJsonVal(json, "status");
                        if (playerId == myPlayerId) {
                            inJail = status.equals("IN_JAIL");
                            jailTurns = Integer.parseInt(getJsonVal(json, "jailTurns"));

                            if (inJail) {
                                jailPanel.setVisible(true);
                                jailStatusLabel.setText("🔒 IN JAIL - Turn " + jailTurns + "/3\nRoll doubles or pay $50 next turn!");
                                updateControlStates();
                            } else {
                                jailPanel.setVisible(false);
                            }
                        }
                    }
                    case "PROPERTY_LIST" -> {
                        int playerId = Integer.parseInt(getJsonVal(json, "playerId"));
                        if (playerId == myPlayerId) {
                            String propertiesStr = getJsonVal(json, "properties");
                            propertiesStr = unescapeJson(propertiesStr);
                            updatePropertyList(propertiesStr);
                        }
                    }
                    case "BUY_OFFER" -> {
                        String property = getJsonVal(json, "property");
                        int price = Integer.parseInt(getJsonVal(json, "price"));
                        String offerMsg = getJsonVal(json, "message");
                        offerMsg = unescapeJson(offerMsg);

                        awaitingBuyDecision = true;

                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Buy Property");
                        alert.setHeaderText("Buy " + property + " for $" + price + "?");
                        alert.setContentText(offerMsg);

                        ButtonType buyButton = new ButtonType("Buy");
                        ButtonType auctionButton = new ButtonType("Start Auction");
                        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                        alert.getButtonTypes().setAll(buyButton, auctionButton, cancelButton);

                        alert.showAndWait().ifPresent(response -> {
                            if (response == buyButton) {
                                client.sendMessage("BUY");
                            } else if (response == auctionButton) {
                                client.sendMessage("PASS");
                            }
                            awaitingBuyDecision = false;
                            updateControlStates();
                        });

                        updateControlStates();
                    }
                    case "AUCTION_START" -> {
                        String property = getJsonVal(json, "property");
                        auctionActive = true;
                        auctionPanel.setVisible(true);
                        auctionStatusLabel.setText("🎪 Auction started for: " + property + "\nMin bid: $" + getJsonVal(json, "minBid"));

                        updateControlStates();
                        logArea.appendText("➤ Auction started for " + property + "\n");
                    }
                    case "AUCTION_UPDATE" -> {
                        String property = getJsonVal(json, "property");
                        int currentBid = Integer.parseInt(getJsonVal(json, "currentBid"));
                        currentBidderId = Integer.parseInt(getJsonVal(json, "currentBidder"));
                        String status = getJsonVal(json, "status");
                        status = unescapeJson(status);

                        auctionStatusLabel.setText("🏷️ " + property +
                                "\n💰 Current bid: $" + currentBid +
                                "\n👤 Current bidder: Player " + currentBidderId +
                                "\n⏳ " + status);

                        boolean myAuctionTurn = (currentBidderId == myPlayerId);
                        btnBid.setDisable(!myAuctionTurn);
                        btnPass.setDisable(!myAuctionTurn);
                        bidAmountField.setDisable(!myAuctionTurn);

                        if (myAuctionTurn) {
                            bidAmountField.setPromptText("Min: $" + (currentBid + 1));
                        }
                    }
                    case "AUCTION_END" -> {
                        int winnerId = Integer.parseInt(getJsonVal(json, "winner"));
                        String property = getJsonVal(json, "property");
                        int amount = Integer.parseInt(getJsonVal(json, "amount"));

                        auctionActive = false;
                        auctionPanel.setVisible(false);
                        currentBidderId = -1;

                        updateControlStates();

                        if (winnerId > 0) {
                            logArea.appendText("➤ Player " + winnerId + " won " + property + " for $" + amount + "\n");
                            showAlert("Auction Ended", "Player " + winnerId + " won " + property + " for $" + amount);
                        } else {
                            logArea.appendText("➤ Auction for " + property + " ended with no winner\n");
                        }
                    }
                    case "TRADE_REQUEST" -> {
                        int fromId = Integer.parseInt(getJsonVal(json, "from"));
                        String fromName = getJsonVal(json, "fromName");
                        fromName = unescapeJson(fromName);
                        int cash = Integer.parseInt(getJsonVal(json, "cash"));
                        String tradeMsg = getJsonVal(json, "message");
                        tradeMsg = unescapeJson(tradeMsg);

                        Alert tradeAlert = new Alert(Alert.AlertType.CONFIRMATION);
                        tradeAlert.setTitle("Trade Request");
                        tradeAlert.setHeaderText("Trade Offer from " + fromName);
                        tradeAlert.setContentText(tradeMsg + "\n\nDo you accept?");

                        tradeAlert.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                client.sendMessage("ACCEPT_TRADE");
                            } else {
                                client.sendMessage("REJECT_TRADE");
                            }
                        });
                    }
                    case "SHOW_CARD" -> {
                        String text = getJsonVal(json, "text");
                        text = unescapeJson(text);

                        if (text.contains("MONOPOLY LEADERBOARD") || text.contains("TOP 3 BY WEALTH")) {
                            showLeaderboardDialog(text);
                        } else if (text.contains("AUCTION") || text.contains("JAIL") || text.contains("Doubles")) {
                            logArea.appendText("➤ " + text + "\n");
                            if (text.contains("IN_JAIL") || text.contains("Jail") || text.contains("Doubles")) {
                                updateJailStatusFromMessage(text);
                            }
                        } else {
                            logArea.appendText("➤ " + text + "\n");
                            Alert a = new Alert(Alert.AlertType.INFORMATION, text);
                            a.setHeaderText("Game Event");
                            a.show();
                        }
                    }
                    case "EVENT_LOG" -> {
                        String unescapedMsg = unescapeJson(msg);
                        logArea.appendText("➤ " + unescapedMsg + "\n");
                    }
                    case "ERROR" -> {
                        String unescapedMsg = unescapeJson(msg);
                        logArea.appendText("❌ Error: " + unescapedMsg + "\n");
                        Alert a = new Alert(Alert.AlertType.ERROR, unescapedMsg);
                        a.setHeaderText("Error");
                        a.show();
                    }
                    default -> {
                        if (!msg.isEmpty()) {
                            String unescapedMsg = unescapeJson(msg);
                            logArea.appendText("➤ " + unescapedMsg + "\n");
                        }
                    }
                }
            } catch (Exception e) {
                logArea.appendText("❌ Error processing message: " + e.getMessage() + "\n");
                logArea.appendText("Raw JSON: " + json + "\n");
            }
        });
    }

    private void updateJailStatusFromMessage(String message) {
        if (message.contains("IN_JAIL") || message.contains("sent to Jail")) {
            inJail = true;
            jailPanel.setVisible(true);
            jailStatusLabel.setText("🔒 " + message);
            updateControlStates();
        } else if (message.contains("FREE") || message.contains("free") || message.contains("paid")) {
            inJail = false;
            jailPanel.setVisible(false);
            updateControlStates();
        }
    }

    private void updatePropertyList(String propertiesStr) {
        propertyList.getItems().clear();

        if (propertiesStr == null || propertiesStr.isEmpty() || propertiesStr.equals("null")) {
            propertyList.getItems().add("No properties owned");
            return;
        }

        String[] properties = propertiesStr.split("\\|");

        for (String prop : properties) {
            String[] parts = prop.split(",");
            if (parts.length >= 6) {
                try {
                    String name = parts[0];
                    String color = parts[1];
                    int houses = Integer.parseInt(parts[2]);
                    boolean hotel = Boolean.parseBoolean(parts[3]);
                    boolean mortgaged = Boolean.parseBoolean(parts[4]);
                    int price = Integer.parseInt(parts[5]);

                    StringBuilder display = new StringBuilder();
                    display.append("🏠 ").append(name);
                    display.append(" (").append(color).append(")");
                    display.append(" - $").append(price);

                    if (hotel) {
                        display.append(" 🏨");
                    } else if (houses > 0) {
                        display.append(" 🏠×").append(houses);
                    }

                    if (mortgaged) {
                        display.append(" ⚠️");
                    }

                    propertyList.getItems().add(display.toString());
                } catch (NumberFormatException e) {
                    propertyList.getItems().add("Error: " + prop);
                }
            }
        }

        if (propertyList.getItems().isEmpty()) {
            propertyList.getItems().add("No properties owned");
        }
    }

    private void updateControlStates() {
        boolean canRollNormal = isMyTurn && !hasRolledThisTurn && !auctionActive && !inJail;
        boolean canRollInJail = isMyTurn && !hasRolledThisTurn && inJail;

        btnRoll.setDisable(!canRollNormal && !canRollInJail);
        btnBuy.setDisable(!isMyTurn || !awaitingBuyDecision || auctionActive || inJail);
        btnTrade.setDisable(!isMyTurn || auctionActive || inJail);
        btnEndTurn.setDisable(!isMyTurn || auctionActive);
        btnUndo.setDisable(!isMyTurn || auctionActive);
        btnRedo.setDisable(!isMyTurn || auctionActive);
        btnBid.setDisable(!auctionActive);
        btnPass.setDisable(!auctionActive);

        btnJailDouble.setDisable(!canRollInJail);
        btnJailPay.setDisable(!canRollInJail);
        btnJailCard.setDisable(!canRollInJail);

        if (inJail && isMyTurn) {
            btnRoll.setText("🎲 TRY DOUBLE");
            btnRoll.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12;");
        } else {
            btnRoll.setText("🎲 ROLL");
            btnRoll.setStyle("-fx-background-color: #D4AF37; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12;");
        }
    }

    private String unescapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showLeaderboardDialog(String leaderboardText) {
        TextArea textArea = new TextArea(leaderboardText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(15);
        textArea.setPrefColumnCount(30);
        textArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px;");

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🏆 Leaderboard");
        dialog.setHeaderText("Top Players Ranking");
        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    private void moveToken(int pId, int tileIdx) {
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

    private void drawFestiveBoard() {
        for (int i = 0; i < 40; i++) {
            double[] pos = calculateTilePos(i);
            Rectangle r = new Rectangle(pos[0], pos[1], TILE_SIZE, TILE_SIZE);
            r.setFill(Color.web("#FDF5E6")); r.setStroke(Color.DARKSLATEGRAY);
            boardPane.getChildren().add(r);

            tileCoords.put(i, new double[]{pos[0] + TILE_SIZE/2, pos[1] + TILE_SIZE/2});

            ImageView iv = new ImageView();
            iv.setFitWidth(TILE_SIZE * 0.7); iv.setFitHeight(TILE_SIZE * 0.7);
            iv.setX(pos[0] + TILE_SIZE * 0.15); iv.setY(pos[1] + TILE_SIZE * 0.15);
            Image icon = getIconForTile(i);
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
        Color[] colors = {Color.RED, Color.BLUE, Color.LIME, Color.GOLD};
        for (int i = 1; i <= 4; i++) {
            Circle c = new Circle(10, colors[i-1]);
            c.setStroke(Color.WHITE);
            c.setCenterX(tileCoords.get(0)[0]); c.setCenterY(tileCoords.get(0)[1]);
            playerTokens.put(i, c); boardPane.getChildren().add(c);
        }
    }

    private Button createBtn(String t, String color) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12;");
        b.setMinWidth(90);
        return b;
    }

    private double[] calculateTilePos(int i) {
        if (i <= 10) return new double[]{BOARD_SIZE - TILE_SIZE - (i * TILE_SIZE), BOARD_SIZE - TILE_SIZE};
        if (i <= 20) return new double[]{0, BOARD_SIZE - TILE_SIZE - ((i - 10) * TILE_SIZE)};
        if (i <= 30) return new double[]{(i - 20) * TILE_SIZE, 0};
        return new double[]{BOARD_SIZE - TILE_SIZE, (i - 30) * TILE_SIZE};
    }

    private Image getIconForTile(int i) {
        if (i == 0) return iconCache.get("go");
        if (i == 10) return iconCache.get("jail");
        if (i == 20) return iconCache.get("parking");
        if (i == 30) return iconCache.get("gotojail");
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
            try {
                client.connect("localhost", 8080);
                logArea.appendText("➤ Connecting to server...\n");
            } catch (Exception e) {
                logArea.appendText("❌ Failed to connect: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private String getJsonVal(String j, String k) {
        try {
            String p = "\"" + k + "\":";
            int s = j.indexOf(p);
            if (s == -1) return "";
            s += p.length();

            if (s >= j.length()) return "";

            if (j.charAt(s) == '\"') {
                int end = j.indexOf("\"", s + 1);
                if (end == -1) return "";
                return j.substring(s + 1, end);
            } else {
                int end = j.indexOf(",", s);
                if (end == -1) end = j.indexOf("}", s);
                if (end == -1) return "";
                return j.substring(s, end).trim();
            }
        } catch (Exception e) {
            return "";
        }
    }
}