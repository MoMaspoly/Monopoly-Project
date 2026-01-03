package ir.monopoly.client;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Consumer<String> onMessageReceived; // تابعی که وقتی پیام آمد اجرا شود

    public void connect(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // یک ترد جداگانه که همیشه گوش‌به‌زنگ سرور است
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection closed.");
            }
        }).start();
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    public void setOnMessageReceived(Consumer<String> listener) {
        this.onMessageReceived = listener;
    }
}
