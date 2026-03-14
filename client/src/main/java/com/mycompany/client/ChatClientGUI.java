package com.mycompany.client; 

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ChatClientGUI {
    
    private final JFrame frame = new JFrame("Chat Client");
    private final JTextArea messageArea = new JTextArea(20, 40);
    private JTextField textField = new JTextField(32); 
    private final JButton sendButton = new JButton("Send");
    
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;

    public ChatClientGUI() {
        // UI Setup (Layout)
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 14));
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(textField);
        bottomPanel.add(sendButton);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        // Handle 'Send' button click or Enter key
        ActionListener sendListener = e -> {
            String msg = textField.getText();
            if (!msg.trim().isEmpty()) {
                out.println(clientName + ": " + msg); // Send message with name
                textField.setText(""); // Clear input field
            }
        };
        textField.addActionListener(sendListener);
        sendButton.addActionListener(sendListener);
    }

    private void connectToServer() throws IOException {
        // Prompt for username upon opening
        clientName = JOptionPane.showInputDialog(
            frame, "Enter your name:", "Chat Login", JOptionPane.PLAIN_MESSAGE
        );
        if (clientName == null || clientName.trim().isEmpty()) {
            clientName = "Guest_" + (int)(Math.random() * 100);
        }
        frame.setTitle("Chat Client - User: " + clientName);

        // Socket Connection
        Socket socket = new Socket("10.0.8.100", 6666);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Automatically notify others upon joining
        out.println("System: " + clientName + " has joined the chat room!");

        // Run a separate Thread to receive messages from the Server
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    messageArea.append(line + "\n");
                    // Auto-scroll to the newest line
                    messageArea.setCaretPosition(messageArea.getDocument().getLength());
                }
            } catch (IOException ex) {
                messageArea.append("\n[System] Lost connection to the Server.\n");
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        ChatClientGUI client = new ChatClientGUI();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.pack();
        client.frame.setLocationRelativeTo(null); // Center on screen
        client.frame.setVisible(true);
        
        client.connectToServer();
    }
}