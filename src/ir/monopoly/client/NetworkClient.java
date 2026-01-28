package ir.monopoly.client;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Consumer<String> onMessageReceived;
    private volatile boolean running = false;

    public void connect(String ip, int port) throws IOException {
        this.socket = new Socket(ip, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.running = true;
        Thread listenerThread = new Thread(this::listenToServer);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenToServer() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                if (onMessageReceived != null) {
                    onMessageReceived.accept(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Network Error: Connection lost.");
        } finally {
            close();
        }
    }

    public void sendMessage(String msg) {
        if (out != null && !socket.isClosed()) {
            out.println(msg);
        }
    }

    public void setOnMessageReceived(Consumer<String> listener) {
        this.onMessageReceived = listener;
    }

    public void close() {
        running = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}
