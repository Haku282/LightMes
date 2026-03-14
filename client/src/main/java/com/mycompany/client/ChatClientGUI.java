package com.mycompany.client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class ChatClientGUI {

    // Color palette
    private static final Color CLR_HEADER    = new Color(28, 86, 158);
    private static final Color CLR_BG        = new Color(235, 238, 243);
    private static final Color CLR_DIVIDER   = new Color(208, 213, 224);
    private static final Color CLR_ACCENT    = new Color(28, 86, 158);
    private static final Color CLR_STATUS_ON  = new Color(134, 239, 172);
    private static final Color CLR_STATUS_OFF = new Color(252, 165, 165);

    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 MB

    // Swing components
    private final JFrame     frame        = new JFrame("LightMes");
    private final JTextPane  chatPane     = new JTextPane();
    private final JTextField textField    = new JTextField();
    private final JButton    sendButton   = new JButton("Gui >");
    private final JButton    emojiButton  = new JButton("Emoji");
    private final JButton    attachButton = new JButton("File");
    private final JLabel     statusLabel  = new JLabel("  Dang ket noi...");
    private final JLabel     titleLabel   = new JLabel("LightMes Chat");

    // State
    private PrintWriter out;
    private String clientName = "Guest";

    private final ConcurrentHashMap<String, byte[]> fileStorage = new ConcurrentHashMap<>();

    public ChatClientGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        buildUI();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CLR_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));

        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(Color.WHITE);

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(170, 205, 255));

        header.add(titleLabel,  BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);

        chatPane.setContentType("text/html");
        chatPane.setEditable(false);
        chatPane.setBackground(CLR_BG);
        chatPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        chatPane.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        resetChat();

        JScrollPane scroll = new JScrollPane(chatPane);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        chatPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                byte[] data = fileStorage.get(e.getDescription());
                if (data != null) downloadFile(e.getDescription(), data);
            }
        });

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CLR_DIVIDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        textField.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CLR_DIVIDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        styleIconBtn(emojiButton,  new Font("Segoe UI Emoji", Font.PLAIN, 16));
        styleIconBtn(attachButton, new Font("Segoe UI Emoji", Font.PLAIN, 16));

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        leftBtns.setBackground(Color.WHITE);
        leftBtns.add(emojiButton);
        leftBtns.add(attachButton);

        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setBackground(CLR_ACCENT);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setBackground(Color.WHITE);
        inputRow.add(leftBtns,  BorderLayout.WEST);
        inputRow.add(textField, BorderLayout.CENTER);

        bottom.add(inputRow,   BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(header, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);

        ActionListener sendAction = e -> sendText();
        textField.addActionListener(sendAction);
        sendButton.addActionListener(sendAction);
        attachButton.addActionListener(e -> sendFile());
        setupEmojiPicker();
    }

    private static void styleIconBtn(JButton btn, Font font) {
        btn.setFont(font);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    }

    private void resetChat() {
        chatPane.setText(
            "<html><body id='chatBody' style='" +
            "font-family:Arial,sans-serif;" +
            "background:#EBECF3;padding:10px;margin:0;'></body></html>");
    }

    private void setupEmojiPicker() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(CLR_DIVIDER));
        String[] emojis = {
            "\uD83D\uDE00", "\uD83D\uDE02", "\uD83E\uDD70", "\uD83D\uDE0E",
            "\uD83D\uDE22", "\uD83D\uDE21", "\uD83D\uDC4D", "\u2764\uFE0F",
            "\uD83C\uDF89", "\uD83D\uDD25", "\uD83E\uDD14", "\uD83D\uDE05",
            "\uD83D\uDE0D", "\uD83D\uDE4F", "\uD83D\uDCAF", "\u2705",
            "\uD83C\uDFB5", "\uD83E\uDD23"
        };
        JPanel grid = new JPanel(new GridLayout(3, 6, 2, 2));
        grid.setBackground(Color.WHITE);
        grid.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        for (String em : emojis) {
            JButton b = new JButton(em);
            b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> {
                textField.setText(textField.getText() + em);
                popup.setVisible(false);
                textField.requestFocus();
            });
            grid.add(b);
        }
        popup.add(grid);
        emojiButton.addActionListener(
            e -> popup.show(emojiButton, 0, -(int) popup.getPreferredSize().getHeight() - 4));
    }

    // =========================================================================
    // Send actions
    // =========================================================================
    private void sendText() {
        String msg = textField.getText().trim();
        if (!msg.isEmpty() && out != null) {
            out.println("TEXT:" + clientName + ": " + msg);
            textField.setText("");
        }
    }

    private void sendFile() {
        if (out == null) {
            JOptionPane.showMessageDialog(frame,
                "Chua ket noi toi server!", "Loi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile();

        new Thread(() -> {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                if (bytes.length > MAX_FILE_BYTES) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(frame,
                            "File qua lon! Gioi han 10 MB.", "Loi", JOptionPane.ERROR_MESSAGE));
                    return;
                }
                String b64 = Base64.getEncoder().encodeToString(bytes);
                out.println("FILE:" + clientName + ":" + file.getName() + ":" + b64);
                appendSystemMsg("Da gui: " + file.getName()
                    + " (" + bytes.length / 1024 + " KB)");
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(frame,
                        "Khong the doc file: " + ex.getMessage(), "Loi",
                        JOptionPane.ERROR_MESSAGE));
            }
        }, "file-sender").start();
    }

    private void downloadFile(String fileId, byte[] data) {
        String name = fileId.contains("_")
            ? fileId.substring(fileId.indexOf('_') + 1) : fileId;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(name));
        if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try (FileOutputStream fos = new FileOutputStream(fc.getSelectedFile())) {
                fos.write(data);
                JOptionPane.showMessageDialog(frame, "Da luu file thanh cong!", "Thanh cong",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Loi khi luu file: " + e.getMessage(),
                    "Loi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================================
    // HTML helpers
    // =========================================================================
    /**
     * Escapes text for safe HTML embedding and converts supplementary Unicode
     * codepoints (U+10000+, i.e. most emoji) to numeric HTML entities such as
     * &#x1F600; so Swing's HTMLEditorKit can render them correctly.
     *
     * RAW surrogate pairs inside HTML strings are NOT handled by Swing's HTML
     * parser -- that is the root cause of the "emoji shows as a box" bug.
     */
    private static String escHtml(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length() * 2);
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            switch (cp) {
                case '&':  sb.append("&amp;");  break;
                case '<':  sb.append("&lt;");   break;
                case '>':  sb.append("&gt;");   break;
                case '"':  sb.append("&quot;"); break;
                default:
                    if (cp > 0xFFFF) {
                        sb.append("&#x").append(Integer.toHexString(cp)).append(';');
                    } else {
                        sb.appendCodePoint(cp);
                    }
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private String buildFileHtml(String fileName, String fileId, byte[] data) {
        boolean isImg = fileName.toLowerCase()
            .matches(".*\\.(png|jpg|jpeg|gif|bmp|webp)$");
        if (isImg && data != null) {
            try {
                File tmp = new File(System.getProperty("java.io.tmpdir"),
                    "lm_" + Math.abs(fileId.hashCode()) + "_" + fileName);
                if (!tmp.exists()) Files.write(tmp.toPath(), data);
                String url = tmp.toURI().toURL().toString();
                return "<img src='" + url + "' width='200'><br>"
                     + "<a href='" + fileId + "' style='color:#1A56A0;font-size:11px;'>"
                     + "Tai anh goc</a>";
            } catch (IOException ex) {
                // fall through to generic display
            }
        }
        return "<b>" + escHtml(fileName) + "</b><br>"
             + "<a href='" + fileId + "' style='color:#1A56A0;'>"
             + "Nhan de tai ve</a>";
    }

    private void appendToChat(String sender, String text,
                               boolean isFile, String fileId, byte[] fileData) {
        SwingUtilities.invokeLater(() -> {
            try {
                HTMLDocument  doc  = (HTMLDocument)  chatPane.getDocument();
                HTMLEditorKit kit  = (HTMLEditorKit) chatPane.getEditorKit();
                String time    = LocalTime.now().format(TIME_FMT);
                String content = isFile ? buildFileHtml(text, fileId, fileData) : escHtml(text);
                String html;

                if ("System".equalsIgnoreCase(sender)) {
                    html = "<table width='100%' border='0' cellpadding='2' cellspacing='0'>"
                         + "<tr><td align='center'>"
                         + "<font face='Arial' size='2' color='#7A828F'>"
                         + "<i>" + content + "</i></font></td></tr></table>";

                } else if (sender.equals(clientName)) {
                    html = "<table width='100%' border='0' cellpadding='0' cellspacing='2'>"
                         + "<tr><td align='right'>"
                         + "<table bgcolor='#DCF8C6' border='0' cellpadding='8' cellspacing='0'>"
                         + "<tr><td>"
                         + "<font face='Segoe UI Emoji,Arial' size='3' color='#000000'>"
                         + content + "</font>"
                         + "&nbsp;<font size='1' color='#A0A8B0'>" + time + "</font>"
                         + "</td></tr></table></td></tr></table>";

                } else {
                    String safeSender = escHtml(sender);
                    html = "<table width='100%' border='0' cellpadding='0' cellspacing='2'>"
                         + "<tr><td align='left'>"
                         + "<table bgcolor='#FFFFFF' border='0' cellpadding='0' cellspacing='0' width='270'>"
                         + "<tr><td style='border-bottom:1px solid #E0E4ED;padding:5px 10px 4px 10px;'>"
                         + "<font face='Arial' size='2' color='#1A56A0'><b>"
                         + safeSender + "</b></font></td></tr>"
                         + "<tr><td style='padding:6px 10px 6px 10px;'>"
                         + "<font face='Segoe UI Emoji,Arial' size='3' color='#000000'>"
                         + content + "</font>"
                         + "&nbsp;<font size='1' color='#A0A8B0'>" + time + "</font>"
                         + "</td></tr></table></td></tr></table>";
                }

                kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
                chatPane.setCaretPosition(doc.getLength());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void appendSystemMsg(String msg) {
        appendToChat("System", msg, false, null, null);
    }

    // =========================================================================
    // Server connection
    // =========================================================================
    private void connectToServer() {
        String name = JOptionPane.showInputDialog(
            frame, "Nhap ten hien thi cua ban:", "Dang nhap", JOptionPane.PLAIN_MESSAGE);
        clientName = (name == null || name.trim().isEmpty())
            ? "Guest_" + (int)(Math.random() * 1000)
            : name.trim();
        frame.setTitle("LightMes - " + clientName);
        titleLabel.setText("LightMes - " + clientName);

        JTextField ipField   = new JTextField("127.0.0.1", 17);
        JTextField portField = new JTextField("6666", 7);
        JPanel addrPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        addrPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        addrPanel.add(new JLabel("Server IP:"));  addrPanel.add(ipField);
        addrPanel.add(new JLabel("Port:"));        addrPanel.add(portField);

        int choice = JOptionPane.showConfirmDialog(frame, addrPanel,
            "Ket noi toi Server", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) { System.exit(0); }

        final String ip = ipField.getText().trim().isEmpty()
            ? "127.0.0.1" : ipField.getText().trim();
        int parsedPort = 6666;
        try { parsedPort = Integer.parseInt(portField.getText().trim()); }
        catch (NumberFormatException ignored) {}
        final int port = parsedPort;

        new Thread(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 10_000);

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(
                    new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)),
                    true);

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Online: " + clientName);
                    statusLabel.setForeground(CLR_STATUS_ON);
                });
                out.println("TEXT:System: " + clientName + " da tham gia phong chat!");

                String line;
                while ((line = reader.readLine()) != null) {
                    processLine(line);
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Mat ket noi");
                    statusLabel.setForeground(CLR_STATUS_OFF);
                });
                appendSystemMsg("Mat ket noi toi server: " + ex.getMessage());
            }
        }, "reader-thread").start();
    }

    /**
     * Parses a server line.
     * TEXT format : TEXT:senderName: message body
     * FILE format : FILE:senderName:filename:base64data
     *
     * FILE is parsed manually (first two ':' after prefix) to correctly handle
     * filenames that might contain colons on non-Windows systems.
     */
    private void processLine(String line) {
        if (line.startsWith("TEXT:")) {
            String body = line.substring(5);
            int sep = body.indexOf(": ");
            if (sep > 0) {
                appendToChat(body.substring(0, sep), body.substring(sep + 2), false, null, null);
            } else {
                appendSystemMsg(body);
            }
        } else if (line.startsWith("FILE:")) {
            String payload = line.substring(5);
            int s1 = payload.indexOf(':');
            if (s1 < 0) return;
            int s2 = payload.indexOf(':', s1 + 1);
            if (s2 < 0) return;

            String sender   = payload.substring(0, s1);
            String fileName = payload.substring(s1 + 1, s2);
            String b64      = payload.substring(s2 + 1);

            try {
                byte[] data   = Base64.getDecoder().decode(b64);
                String fileId = System.currentTimeMillis() + "_" + fileName;
                fileStorage.put(fileId, data);
                appendToChat(sender, fileName, true, fileId, data);
            } catch (IllegalArgumentException e) {
                appendSystemMsg("Loi giai ma file '" + fileName + "' tu " + sender);
            }
        }
    }

    // =========================================================================
    // Entry point
    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClientGUI client = new ChatClientGUI();
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setSize(500, 720);
            client.frame.setMinimumSize(new Dimension(380, 520));
            client.frame.setLocationRelativeTo(null);
            client.frame.setVisible(true);
            client.connectToServer();
        });
    }
}