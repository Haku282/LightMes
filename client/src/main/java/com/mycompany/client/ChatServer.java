package com.mycompany.client; // Đã thêm package

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
    // Đã thêm 'final' để đảm bảo an toàn cho đa luồng (sửa lỗi Synchronization)
    private static final Set<PrintWriter> clientWriters = new HashSet<>();

    public static void main(String[] args) throws Exception {
        System.out.println("[SERVER] Chat Server đang chạy tại port 6666...");
        try (ServerSocket listener = new ServerSocket(6666)) {
            while (true) {
                new ClientHandler(listener.accept()).start();
            }
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket; // Đã thêm 'final'
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override // Đã thêm @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                // Thêm client mới vào danh sách phát sóng
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                String message;
                // Nhận tin nhắn từ 1 Client và phát cho TẤT CẢ
                while ((message = in.readLine()) != null) {
                    System.out.println("[LOG] Đã nhận và chuyển tiếp: " + message);
                    synchronized (clientWriters) {
                        for (PrintWriter writer : clientWriters) {
                            writer.println(message);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("[DISCONNECT] Một client đã thoát.");
            } finally {
                // Khi client thoát, xóa khỏi danh sách
                if (out != null) {
                    synchronized (clientWriters) {
                        clientWriters.remove(out);
                    }
                }
                try { socket.close(); } catch (IOException e) {}
            }
        }
    }
}