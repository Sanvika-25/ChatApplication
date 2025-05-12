import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class SocketServer extends JFrame implements ActionListener {
    ServerSocket server;
    Socket sk;
    InetAddress addr;

    ArrayList<ServerThread> list = new ArrayList<>();
    JTextArea serverTextArea;   // For displaying messages
    JTextField serverInputField; // For sending messages from server

    PrintWriter pw;

    public SocketServer() {
        // Set up server GUI
        super("Server Chat Window");
        setLayout(new BorderLayout());
        
        serverTextArea = new JTextArea();
        serverTextArea.setEditable(false);
        add(new JScrollPane(serverTextArea), BorderLayout.CENTER);

        // Add an input field for the server to send messages
        serverInputField = new JTextField();
        serverInputField.setToolTipText("Enter your message with emojis: :) :(");
        add(serverInputField, BorderLayout.SOUTH);
        
        // Add action listener for sending messages from the server
        serverInputField.addActionListener(this);

        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        try {
            addr = InetAddress.getByName("0.0.0.0");
            server = new ServerSocket(1234, 50, addr);
            serverTextArea.append("Waiting for client connections...\n");

            while (true) {
                sk = server.accept();
                serverTextArea.append("Client connected: " + sk.getInetAddress() + "\n");

                // Create a new thread for each client connection
                ServerThread st = new ServerThread(this, sk);
                addThread(st);
                st.start();
            }
        } catch (IOException e) {
            serverTextArea.append("Error: " + e.getMessage() + "\n");
            System.out.println(e + "-> ServerSocket failed");
        }
    }

    public void addThread(ServerThread st) {
        list.add(st);
    }

    public void removeThread(ServerThread st) {
        list.remove(st); // Remove thread from the list
    }

    public void broadCast(String message) {
        for (ServerThread st : list) {
            st.pw.println(message); // Send the message to all clients
        }
    }

    // Method to display messages on the server UI
    public void displayMessage(String message) {
        serverTextArea.append(message + "\n");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String message = serverInputField.getText();
        message = formatMessage(message);  // Format the message for emojis
        displayMessage("[Server]: " + message);
        broadCast("[Server]: " + message);
        serverInputField.setText(""); // Clear input field after sending
    }

    // Method to replace emojis with corresponding symbols
    public String formatMessage(String message) {
        // Replace common emoji shortcuts with actual symbols
        message = message.replace(":)", "😊");
        message = message.replace(":(", "☹️");
        message = message.replace("<3", "❤️");
        // Add more as needed
        return message;
    }

    public static void main(String[] args) {
        new SocketServer(); // Start the server
    }
}

class ServerThread extends Thread {
    SocketServer server;
    Socket clientSocket;
    PrintWriter pw;
    String name;

    public ServerThread(SocketServer server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            // Reading from client
            BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            pw = new PrintWriter(clientSocket.getOutputStream(), true);

            // Get the client's name and broadcast it
            name = br.readLine();
            server.broadCast("[" + name + "] Entered**");
            server.displayMessage("[" + name + "] has entered the chat");

            String data;
            while ((data = br.readLine()) != null) {
                data = server.formatMessage(data); // Format for emojis
                server.broadCast("[" + name + "] " + data); // Broadcast to all clients
                server.displayMessage("[" + name + "] " + data); // Show message on the server UI
            }
        } catch (Exception e) {
            server.removeThread(this);
            server.broadCast("[" + name + "] Left**");
            server.displayMessage("[" + name + "] has left the chat");
            System.out.println(clientSocket.getInetAddress() + " - [" + name + "] Exit");
            System.out.println(e + "---->");
        }
    }
}