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
    // Khai báo biến (đã thêm final cho gọn gàng và không bị lặp)
    private final JFrame frame = new JFrame("Phần mềm Chat");
    private final JTextArea messageArea = new JTextArea(20, 40);
    private JTextField textField = new JTextField(32); 
    private final JButton sendButton = new JButton("Gửi");
    
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;

    public ChatClientGUI() {
        // Thiết lập Giao diện (Layout)
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 14));
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(textField);
        bottomPanel.add(sendButton);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        // Xử lý sự kiện khi bấm nút "Gửi" hoặc nhấn Enter
        ActionListener sendListener = e -> {
            String msg = textField.getText();
            if (!msg.trim().isEmpty()) {
                out.println(clientName + ": " + msg); // Gửi kèm tên
                textField.setText(""); // Xóa trắng ô nhập
            }
        };
        textField.addActionListener(sendListener);
        sendButton.addActionListener(sendListener);
    }

    private void connectToServer() throws IOException {
        // Cửa sổ popup hỏi tên người dùng lúc mới mở
        clientName = JOptionPane.showInputDialog(
            frame, "Nhập tên của bạn:", "Đăng nhập Chat", JOptionPane.PLAIN_MESSAGE
        );
        if (clientName == null || clientName.trim().isEmpty()) {
            clientName = "Khách_" + (int)(Math.random() * 100);
        }
        frame.setTitle("Phần mềm Chat - Đang dùng tên: " + clientName);

        // Kết nối Socket
        Socket socket = new Socket("127.0.0.1", 6666);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Chạy Thread riêng để nhận tin nhắn từ Server mà không làm đơ giao diện
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    messageArea.append(line + "\n");
                    // Tự động cuộn xuống dòng mới nhất
                    messageArea.setCaretPosition(messageArea.getDocument().getLength());
                }
            } catch (IOException ex) {
                messageArea.append("\n[Hệ thống] Đã mất kết nối tới Server.\n");
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        ChatClientGUI client = new ChatClientGUI();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.pack();
        client.frame.setLocationRelativeTo(null); // Hiển thị ở giữa màn hình
        client.frame.setVisible(true);
        
        client.connectToServer();
    }
}