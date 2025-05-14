import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9001;
    
    private String userName;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    
    // GUI Components
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel statusLabel;
    
    public ChatClient() {
        initializeGUI();
    }
    
    private void initializeGUI() {
        // Set up the main frame
        frame = new JFrame("Java Chat Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());
        
        // Chat display area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        frame.add(scrollPane, BorderLayout.CENTER);
        
        // Message input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        inputPanel.add(messageField, BorderLayout.CENTER);
        
        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        frame.add(inputPanel, BorderLayout.SOUTH);
        
        // Status label
        statusLabel = new JLabel("Not connected");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        frame.add(statusLabel, BorderLayout.NORTH);
        
        // Event handlers
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
        
        frame.setVisible(true);
        
        // Start connection process
        promptForUserName();
    }
    
    private void promptForUserName() {
        userName = JOptionPane.showInputDialog(
                frame,
                "Enter your name:",
                "Login",
                JOptionPane.PLAIN_MESSAGE);
                
        if (userName == null || userName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Username cannot be empty.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        
        // Connect to server
        connect();
    }
    
    private void connect() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            // Start reading messages from server in a separate thread
            new Thread(new IncomingMessageHandler()).start();
            
            // Enable UI components
            sendButton.setEnabled(true);
            messageField.setEnabled(true);
            messageField.requestFocus();
            statusLabel.setText("Connected as: " + userName);
            
            // First message from server should be asking for username
            String serverResponse = reader.readLine();
            if (serverResponse != null && serverResponse.startsWith("Please enter your name:")) {
                writer.println(userName);
                addToChat("Connected to the chat server");
            }
            
        } catch (IOException e) {
            addToChat("Error connecting to server: " + e.getMessage());
            JOptionPane.showMessageDialog(
                    frame,
                    "Cannot connect to server: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            writer.println(message);
            messageField.setText("");
        }
        messageField.requestFocus();
    }
    
    private void addToChat(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                chatArea.append(message + "\n");
                // Auto-scroll to bottom
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
        });
    }
    
    private void disconnect() {
        if (socket != null && !socket.isClosed()) {
            try {
                if (writer != null) {
                    writer.println("bye");
                }
                socket.close();
                reader.close();
                writer.close();
            } catch (IOException e) {
                System.out.println("Error disconnecting: " + e.getMessage());
            }
        }
    }
    
    // Handler for incoming messages from server
    private class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    addToChat(message);
                }
            } catch (IOException e) {
                addToChat("Connection to server lost: " + e.getMessage());
                sendButton.setEnabled(false);
                messageField.setEnabled(false);
                statusLabel.setText("Disconnected");
            }
        }
    }
    
    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatClient();
            }
        });
    }
}