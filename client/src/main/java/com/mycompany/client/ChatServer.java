package com.mycompany.client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.*;

public class ChatServer {

    // Thread-safe set of all connected client handlers
    private static final ConcurrentHashMap<ClientHandler, Boolean> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = 6666;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) { System.err.println("[WARN] Port không hợp lệ, dùng mặc định 6666"); }
        }

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║      LightMes Chat Server v2.0       ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("[INFO] Port: " + port);

        // Print all non-loopback IPv4 addresses so users know which IP to use
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics != null && nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) continue;
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address) {
                        System.out.println("[INFO] LAN IP: " + addr.getHostAddress()
                                + "  ← Client dùng IP này để kết nối");
                    }
                }
            }
        } catch (SocketException ignored) {}

        System.out.println("[INFO] Đang lắng nghe kết nối...\n");

        try (ServerSocket server = new ServerSocket(port)) {
            server.setReuseAddress(true);
            while (true) {
                Socket socket = server.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.put(handler, Boolean.TRUE);
                handler.start();
                System.out.println(ts() + " [+] Kết nối mới: "
                        + socket.getRemoteSocketAddress() + "  |  Tổng: " + clients.size());
            }
        } catch (IOException e) {
            System.err.println("[FATAL] Server lỗi: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Broadcast a message to ALL connected clients (non-blocking). */
    static void broadcast(String message) {
        for (ClientHandler c : clients.keySet()) {
            c.enqueue(message);
        }
    }

    static void removeClient(ClientHandler h) {
        if (clients.remove(h) != null) {
            System.out.println(ts() + " [-] Ngắt kết nối: " + h.displayName
                    + "  |  Còn lại: " + clients.size());
        }
    }

    private static String ts() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    // -------------------------------------------------------------------------
    static class ClientHandler extends Thread {

        private final Socket socket;
        // Each client has its own outbound queue → broadcast never blocks
        private final LinkedBlockingQueue<String> outQueue = new LinkedBlockingQueue<>();
        private volatile boolean alive = true;
        volatile String displayName = "unknown";

        ClientHandler(Socket socket) {
            this.socket = socket;
            setDaemon(true);
        }

        /** Non-blocking enqueue; safe to call from any thread. */
        void enqueue(String msg) {
            if (alive) outQueue.offer(msg);
        }

        @Override
        public void run() {
            Thread writerThread = startWriterThread();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logIncoming(line);
                    broadcast(line);
                }
            } catch (IOException e) {
                if (alive) System.out.println(ts() + " [READ-ERR] " + displayName + ": " + e.getMessage());
            } finally {
                alive = false;
                writerThread.interrupt();
                removeClient(this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private Thread startWriterThread() {
            Thread t = new Thread(() -> {
                try (BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                    while (alive) {
                        try {
                            String msg = outQueue.poll(2, TimeUnit.SECONDS);
                            if (msg == null) continue;
                            bw.write(msg);
                            bw.newLine();
                            // Drain any additional queued messages before flushing (efficiency)
                            String next;
                            while ((next = outQueue.poll()) != null) {
                                bw.write(next);
                                bw.newLine();
                            }
                            bw.flush();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (IOException e) {
                            System.out.println(ts() + " [WRITE-ERR] " + displayName + ": " + e.getMessage());
                            alive = false;
                            break;
                        }
                    }
                } catch (IOException e) {
                    alive = false;
                }
            }, "writer@" + socket.getRemoteSocketAddress());
            t.setDaemon(true);
            t.start();
            return t;
        }

        private void logIncoming(String msg) {
            if (msg.startsWith("TEXT:")) {
                String body = msg.substring(5);
                int sep = body.indexOf(": ");
                if (sep > 0) displayName = body.substring(0, sep);
                System.out.println(ts() + " [MSG ] " + body);
            } else if (msg.startsWith("FILE:")) {
                String[] parts = msg.substring(5).split(":", 3);
                if (parts.length >= 2) {
                    displayName = parts[0];
                    long sizeKb = parts.length > 2 ? (long) (parts[2].length() * 0.75 / 1024) : 0;
                    System.out.printf("%s [FILE] %s → %s  (%d KB)%n", ts(), parts[0], parts[1], sizeKb);
                }
            }
        }
    }
}