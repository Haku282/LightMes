package com.mycompany.client; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
    private static final Set<PrintWriter> clientWriters = new HashSet<>();

    public static void main(String[] args) throws Exception {
        System.out.println("[SERVER] Chat Server is running on port 6666...");
        try (ServerSocket listener = new ServerSocket(6666)) {
            while (true) {
                new ClientHandler(listener.accept()).start();
            }
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket; 
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override 
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                // Add new client to the broadcast list
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                String message;
                // Receive message from a Client
                while ((message = in.readLine()) != null) {
                    
                    // Split the string based on ": " sent by the Client
                    String[] parts = message.split(": ", 2); 
                    
                    if (parts.length == 2) {
                        String userName = parts[0]; // User name part
                        String content = parts[1];  // Message content part
                        System.out.println("[RECEIVE] User [" + userName + "] sent: " + content);
                    } else {
                        // Fallback in case a system message doesn't have ": "
                        System.out.println("[RECEIVE] " + message);
                    }

                    // Broadcast the original 'message' to ALL other Clients
                    synchronized (clientWriters) {
                        for (PrintWriter writer : clientWriters) {
                            writer.println(message);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("[DISCONNECT] A client disconnected due to a connection error.");
            } finally {
                // When client exits, remove from the list
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