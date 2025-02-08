// File: myftpserver.java
import java.io.*;
import java.net.*;

public class myftpserver {
    private int portNum;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    private File currentDirectory;
    private final String SERVER_DIR = "src/server"; // Base server directory

    public myftpserver(int portNum) {
        this.portNum = portNum;
        this.currentDirectory = new File(".").getAbsoluteFile();
        if (!this.currentDirectory.exists()) {
            this.currentDirectory.mkdirs();
        }
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(portNum);
            System.out.println("Server is running...");
            System.out.println("Listening on port: " + portNum);

            while (true) {
                clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Error: Could not listen on port " + portNum);
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintWriter(clientSocket.getOutputStream(), true);

            String command;
            while ((command = input.readLine()) != null) {
                String[] parts = command.split(" ");
                String cmd = parts[0].toLowerCase();

               
                switch (cmd) {    //get statements for the client server
                    case "get":
                        handleGet(parts);
                        break;
                    case "put":     //put statements for the client server
                        handlePut(parts);
                        break;
                    case "ls":      //ls statements which lists the files and directories
                        handleLs();
                        break;
                    case "cd":    //cd is change directory
                        handleCd(parts);
                        break;
                    case "pwd":    //prints working directory
                        handlePwd();
                        break;
                    case "mkdir":    //makes new directory
                        handleMkdir(parts);
                        break;
                    case "delete":    //deletes the files
                        handleDelete(parts);
                        break;
                    case "quit":    //quit files or directories
                        output.println("Bye. You quit.");
                        output.println("END_OF_LIST");
                        return;
                    default:
                        output.println("Unknown command");
                        output.println("END_OF_LIST");
                }
            }
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    input.close();
                    output.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing the client connection: " + e.getMessage());
            }
        }
    }

    private void handleGet(String[] parts) throws IOException {
        if (parts.length != 2) {
            output.println("ERROR Usage: get <filename>");
            return;
        }

        File file = new File(currentDirectory, parts[1]);
        if (!file.exists() || !file.isFile()) {
            output.println("ERROR File not found: " + parts[1]);
            return;
        }

        try {
            output.println("SIZE " + file.length());
            output.flush();
            
            DataOutputStream dataOutput = new DataOutputStream(clientSocket.getOutputStream());
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                dataOutput.write(buffer, 0, bytesRead);
            }
            
            dataOutput.flush();
            fis.close();
            
            output.println("END_OF_LIST");
            output.flush();
        } catch (IOException e) {
            output.println("ERROR Failed to send file: " + e.getMessage());
        }
    }

        //handles the put using file
    private void handlePut(String[] parts) throws IOException {
        if (parts.length != 3) {  // Command, filename, and size
            output.println("ERROR Usage: put <filename> <size>");
            return;
        }

        String filename = parts[1];
        long size;
        try {
            size = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            output.println("ERROR Invalid file size");
            return;
        }

        // Ensure we're saving to the server directory
        File serverDir = new File(SERVER_DIR);
        if (!serverDir.exists()) {
            serverDir.mkdirs();
        }
        
        // Create file in the server directory
        File file = new File(serverDir, filename);
        
        try {
            output.println("READY");
            output.flush();
            
            DataInputStream dataInput = new DataInputStream(clientSocket.getInputStream());
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            long received = 0;
            
            while (received < size) {
                int bytesToRead = (int) Math.min(buffer.length, size - received);
                int bytesRead = dataInput.read(buffer, 0, bytesToRead);
                
                if (bytesRead == -1) break;
                
                fos.write(buffer, 0, bytesRead);
                received += bytesRead;
            }
            
            fos.close();
            
            output.println("File uploaded successfully to server directory");
            output.println("END_OF_LIST");
        } catch (IOException e) {
            output.println("ERROR Failed to receive file: " + e.getMessage());
            output.println("END_OF_LIST");
            if (file.exists()) {
                file.delete();  // Clean up partial file
            }
        }
    }

//ls using the file
    private void handleLs() {
        File[] files = currentDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                output.println(file.getName() + (file.isDirectory() ? "/" : ""));
            }
        }
        output.println("END_OF_LIST");
    }

        //cd using the file
    private void handleCd(String[] parts) {
        if (parts.length != 2) {
            output.println("Usage: cd <directory>");
            output.println("END_OF_LIST");
            return;
        }
    
        String path = parts[1];
        File newDir;
    
        if (path.equals("..")) {
            newDir = currentDirectory.getParentFile();
            if (newDir == null) {
                output.println("Already at the root directory.");
                output.println("END_OF_LIST");
                return;
            }
        } else {
            newDir = new File(currentDirectory, path);
        }
    
        if (newDir.exists() && newDir.isDirectory()) {
            currentDirectory = newDir;
            output.println("Changed to " + currentDirectory.getAbsolutePath());
        } else {
            output.println("Not a directory or does not exist.");
        }
    
        output.println("END_OF_LIST");
    }

    private void handlePwd() {
        output.println(currentDirectory.getAbsolutePath());
        output.println("END_OF_LIST");
    }

        //mkdir using the file
    private void handleMkdir(String[] parts) {
        if (parts.length != 2) {
            output.println("Usage: mkdir <directory>");
            output.println("END_OF_LIST");
            return;
        }

        File newDir = new File(currentDirectory, parts[1]);
        if (newDir.mkdir()) {
            output.println("Directory created");
        } else {
            output.println("Could not create directory");
        }
        output.println("END_OF_LIST");
    }

        //delete using the file
    private void handleDelete(String[] parts) {
        if (parts.length != 2) {
            output.println("Usage: delete <filename>");
            output.println("END_OF_LIST");
            return;
        }

        File file = new File(currentDirectory, parts[1]);
        if (file.exists()) {
            if (file.delete()) {
                output.println("File deleted");
            } else {
                output.println("Could not delete file");
            }
        } else {
            output.println("File not found");
        }
        output.println("END_OF_LIST");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java myftpserver <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        myftpserver server = new myftpserver(port);
        server.run();
    }
}