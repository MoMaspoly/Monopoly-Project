package ir.monopoly.client;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Handles the low-level socket communication for the client side.
 * It listens for incoming server messages on a background thread.
 */
public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Consumer<String> onMessageReceived;
    private volatile boolean running = false;

    /**
     * Connects to the Monopoly server.
     * @param ip The server IP (usually "localhost").
     * @param port The server port (usually 8080).
     */
    public void connect(String ip, int port) throws IOException {
        this.socket = new Socket(ip, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.running = true;

        // Start a background thread to listen for server messages continuously
        Thread listenerThread = new Thread(this::listenToServer);
        listenerThread.setDaemon(true); // Ensures thread closes when GUI exits
        listenerThread.start();
    }

    /**
     * Background loop that reads strings from the server.
     */
    private void listenToServer() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                if (onMessageReceived != null) {
                    // Pass the raw JSON string to the GUI dispatcher
                    onMessageReceived.accept(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Network Error: Connection lost.");
        } finally {
            close();
        }
    }

    /**
     * Sends a command string to the server.
     * @param msg The raw command (e.g., "ROLL" or "BUY").
     */
    public void sendMessage(String msg) {
        if (out != null && !socket.isClosed()) {
            out.println(msg);
        }
    }

    /**
     * Sets the listener function (the GUI processMessage method).
     */
    public void setOnMessageReceived(Consumer<String> listener) {
        this.onMessageReceived = listener;
    }

    /**
     * Safely closes the network connection.
     */
    public void close() {
        running = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}
