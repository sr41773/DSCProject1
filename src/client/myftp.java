// File: myftp.java
import java.io.*;
import java.net.*;

public class myftp {
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private String serverAddress;
    private int serverPort;

    public myftp(String address, int port) {
        this.serverAddress = address;
        this.serverPort = port;
    }

    // connection to server
    public void connect() {
        try {
            socket = new Socket(serverAddress, serverPort);

            //input and output data streams to client
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connected to server " + serverAddress + ":" + serverPort);
        } catch (IOException e) {
            System.out.println("Could not connect to server: " + e.getMessage());
            System.exit(1);
        }
    }

    public void cmd_handler(String command) {
        if (command.isEmpty()) return;

        String[] parts = command.split(" ");  //only take command without spaces
        String cmd = parts[0].toLowerCase();

        try {
            switch (cmd) {
                case "get":
                    get_cmd(command);
                    break;
                case "put":
                    put_cmd(command);
                    break;
                case "quit":
                    output.println(command);
                    read_response();
                    socket.close();
                    System.exit(0);
                    break;
                default:
                    output.println(command);
                    read_response();
                    break;
            }
        } catch (IOException e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }

    private void get_cmd(String command) throws IOException {
        String[] parts = command.split(" ");
        if (parts.length != 2) {
            System.out.println("Usage: get <filename>");
            return;
        }

        output.println(command);
        String response = input.readLine();

        if (response.startsWith("ERROR")) {
            System.out.println(response);
            return;
        }

        if (response.startsWith("SIZE")) {
            long size = Long.parseLong(response.split(" ")[1]);
            String filename = parts[1];
            
            System.out.println("Receiving file: " + filename + " (" + size + " bytes)");
            
            //streams for file
            DataInputStream dataInput = new DataInputStream(socket.getInputStream());
            FileOutputStream fos = new FileOutputStream(filename);
            byte[] buffer = new byte[4096];
            long received = 0;
            
            while (received < size) {
                int bytesToRead = (int) Math.min(buffer.length, size - received);
                int bytesRead = dataInput.read(buffer, 0, bytesToRead);
                
                if (bytesRead == -1) break;
                
                fos.write(buffer, 0, bytesRead);
                received += bytesRead;
                
                System.out.print("\rProgress: " + (received * 100 / size) + "%");
            }
            
            System.out.println("\nFile downloaded successfully");
            fos.close();
            
            // Read the END_OF_LIST marker
            input.readLine();
        }
    }

    private void put_cmd(String command) throws IOException {
        String[] parts = command.split(" ");
        if (parts.length != 2) {
            System.out.println("Usage: put <filename>");
            return;
        }

        File file = new File(parts[1]);
        if (!file.exists()) {
            System.out.println("File not found: " + parts[1]);
            return;
        }

        // Send command and file size
        output.println(command + " " + file.length());
        String response = input.readLine();

        if (response.equals("READY")) {
            System.out.println("Sending file: " + file.getName() + " (" + file.length() + " bytes)");
            
            DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            long sent = 0;
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                dataOutput.write(buffer, 0, bytesRead);
                sent += bytesRead;
                
                System.out.print("\rProgress: " + (sent * 100 / file.length()) + "%");
            }
            
            System.out.println("\nFile uploaded successfully");
            fis.close();
            dataOutput.flush();
            
            read_response();
        } else {
            System.out.println("Server error: " + response);
        }
    }

    private void read_response() throws IOException {
        String line;
        while ((line = input.readLine()) != null && !line.equals("END_OF_LIST")) {
            System.out.println(line);
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java myftp <server_address> <port>");
            return;
        }

        String serverAddress = args[0];
        int port = Integer.parseInt(args[1]);

        //get client machine address and port num
        myftp client = new myftp(serverAddress, port);
        client.connect();

        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

        try {
            while (true) {
                System.out.print("myftp> ");                //continuously have prompt for client terminal
                String command = consoleReader.readLine();
                
                if (command == null || command.trim().isEmpty()) {
                    continue;
                }
                
                client.cmd_handler(command);
            }
        } catch (IOException e) {
            System.out.println("Error reading command: " + e.getMessage());
        }
    }
}