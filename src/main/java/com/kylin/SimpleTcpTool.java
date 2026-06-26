package com.kylin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class SimpleTcpTool extends JFrame {
    private JTextField ipField = new JTextField("127.0.0.1");
    private JTextField portField = new JTextField("8899");
    private JTextArea logArea = new JTextArea();
    private JTextField sendField = new JTextField();

    private JButton startServerBtn = new JButton("启动服务端");
    private JButton stopServerBtn = new JButton("停止服务端");
    private JButton connectClientBtn = new JButton("连接服务端");
    private JButton disconnectClientBtn = new JButton("断开连接");
    private JButton sendBtn = new JButton("发送");

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;

    private Thread serverThread;
    private volatile boolean serverRunning = false;
    private volatile boolean connected = false;

    public SimpleTcpTool() {
        setTitle("自制TCP调试助手 - 完美版");
        setSize(750, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ========== 顶部控制面板 ==========
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("IP:"));
        topPanel.add(ipField);
        topPanel.add(new JLabel("端口:"));
        topPanel.add(portField);

        topPanel.add(startServerBtn);
        topPanel.add(stopServerBtn);
        topPanel.add(connectClientBtn);
        topPanel.add(disconnectClientBtn);

        add(topPanel, BorderLayout.NORTH);

        // ========== 日志区域 ==========
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // ========== 发送区域 ==========
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(sendField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // ========== 按钮绑定 ==========
        startServerBtn.addActionListener(e -> startServer());
        stopServerBtn.addActionListener(e -> stopServer());
        connectClientBtn.addActionListener(e -> connectClient());
        disconnectClientBtn.addActionListener(e -> disconnectClient());
        sendBtn.addActionListener(e -> sendMessage());

        // 关闭窗口时释放资源
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cleanAll();
            }
        });

        // 初始按钮状态
        refreshButtonState();
    }

    // ========== 日志输出 ==========
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getText().length());
        });
    }

    // ========== 启动服务端 ==========
    private void startServer() {
        if (serverRunning) return;

        serverThread = new Thread(() -> {
            try {
                int port = Integer.parseInt(portField.getText());
                serverSocket = new ServerSocket(port);
                serverRunning = true;
                refreshButtonState();
                log("✅ 服务端已启动，监听端口：" + port);

                while (serverRunning) {
                    try {
                        Socket socket = serverSocket.accept();
                        if (clientSocket != null && !clientSocket.isClosed()) {
                            clientSocket.close();
                        }
                        clientSocket = socket;
                        log("✅ 新客户端连接：" + socket.getInetAddress());
                        initStream(socket);
                        startReadThread();
                    } catch (Exception e) {
                        if (!serverRunning) break;
                    }
                }
            } catch (Exception e) {
                log("❌ 服务端启动失败：" + e.getMessage());
            }
        });
        serverThread.start();
    }

    // ========== 停止服务端 ==========
    private void stopServer() {
        serverRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
        log("🛑 服务端已停止");
        refreshButtonState();
    }

    // ========== 客户端连接 ==========
    private void connectClient() {
        if (connected) return;

        new Thread(() -> {
            try {
                String ip = ipField.getText();
                int port = Integer.parseInt(portField.getText());
                clientSocket = new Socket(ip, port);
                connected = true;
                initStream(clientSocket);
                startReadThread();
                log("✅ 已连接服务端：" + ip + ":" + port);
                refreshButtonState();
            } catch (Exception e) {
                log("❌ 连接失败：" + e.getMessage());
            }
        }).start();
    }

    // ========== 客户端断开 ==========
    private void disconnectClient() {
        try {
            if (clientSocket != null) clientSocket.close();
        } catch (Exception ignored) {}
        connected = false;
        log("🛑 已断开与服务端的连接");
        refreshButtonState();
    }

    // ========== 初始化流 ==========
    private void initStream(Socket socket) throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    // ========== 读取消息 ==========
    private void startReadThread() {
        new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    log("📩 收到：" + line);
                }
            } catch (Exception e) {
                log("⚠️ 连接已断开");
            } finally {
                connected = false;
                refreshButtonState();
            }
        }).start();
    }

    // ========== 发送消息 ==========
    private void sendMessage() {
        String txt = sendField.getText().trim();
        if (txt.isEmpty() || writer == null) return;

        writer.println(txt);
        log("📤 发送：" + txt);
        sendField.setText("");
    }

    // ========== 按钮状态刷新 ==========
    private void refreshButtonState() {
        SwingUtilities.invokeLater(() -> {
            startServerBtn.setEnabled(!serverRunning);
            stopServerBtn.setEnabled(serverRunning);
            connectClientBtn.setEnabled(!connected);
            disconnectClientBtn.setEnabled(connected);
        });
    }

    // ========== 清理 ==========
    private void cleanAll() {
        stopServer();
        disconnectClient();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimpleTcpTool().setVisible(true));
    }
}