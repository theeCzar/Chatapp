import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 9001;
    private static HashSet<String> userNames = new HashSet<>();
    private static HashSet<ClientHandler> clients = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Chat Server is running...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());
                
                // Create a new handler for this client
                ClientHandler newClient = new ClientHandler(socket);
                clients.add(newClient);
                
                // Start a new thread for this client
                new Thread(newClient).start();
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Broadcasts a message to all connected clients
    static void broadcast(String message, ClientHandler excludeClient) {
        for (ClientHandler client : clients) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
    }

    // Remove a client from the set
    static void removeClient(ClientHandler client) {
        String userName = client.getUserName();
        if (userName != null) {
            userNames.remove(userName);
            System.out.println("User " + userName + " has left the chat");
        }
        clients.remove(client);
    }

    // Add a username to the set
    static boolean addUserName(String userName) {
        if (userName == null || userName.trim().isEmpty() || userNames.contains(userName)) {
            return false;
        }
        userNames.add(userName);
        return true;
    }

    // ClientHandler class to handle each client connection
    static class ClientHandler implements Runnable {
        private Socket socket;
        private String userName;
        private BufferedReader reader;
        private PrintWriter writer;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getUserName() {
            return this.userName;
        }

        @Override
        public void run() {
            try {
                // Set up input and output streams
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                // Ask for user's name
                writer.println("Please enter your name:");
                userName = reader.readLine();
                
                // Validate the username
                while (!addUserName(userName)) {
                    writer.println("Username already taken or invalid. Please choose another name:");
                    userName = reader.readLine();
                }

                // Inform everyone about the new user
                writer.println("Welcome " + userName + " to the chat room!");
                broadcast(userName + " has joined the chat", this);

                // Process messages from this client
                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {
                    // Check for exit command
                    if (clientMessage.equalsIgnoreCase("bye")) {
                        break;
                    }
                    // Broadcast the message
                    broadcast(userName + ": " + clientMessage, this);
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Remove the client and close resources
                try {
                    removeClient(this);
                    socket.close();
                    if (reader != null) reader.close();
                    if (writer != null) writer.close();
                } catch (IOException e) {
                    System.out.println("Error closing client resources: " + e.getMessage());
                }
                
                // Inform others that this client has left
                broadcast(userName + " has left the chat", this);
            }
        }

        // Send a message to this client
        void sendMessage(String message) {
            writer.println(message);
        }
    }
}