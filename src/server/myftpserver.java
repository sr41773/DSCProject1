import java.io.*;
import java.net.*;
import java.nio.file.*;

public class myftpserver {
    private int portNum;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    private File currentDirectory;
    private final File BASE_DIR;

    public myftpserver(int portNum) {
        this.portNum = portNum;
        // Use the current working directory as the base directory
        this.BASE_DIR = new File(System.getProperty("user.dir")).getAbsoluteFile();
        this.currentDirectory = BASE_DIR;
        
        // Create base directory if it doesn't exist
        if (!this.BASE_DIR.exists()) {
            this.BASE_DIR.mkdirs();
        }
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(portNum);
            System.out.println("Server is running...");
            System.out.println("Listening on port: " + portNum);
            System.out.println("Base directory: " + BASE_DIR.getAbsolutePath());

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

                switch (cmd) {
                    case "get":
                        handleGet(parts);
                        break;
                    case "put":
                        handlePut(parts);
                        break;
                    case "ls":
                        handleLs();
                        break;
                    case "cd":
                        handleCd(parts);
                        break;
                    case "pwd":
                        handlePwd();
                        break;
                    case "mkdir":
                        handleMkdir(parts);
                        break;
                    case "delete":
                        handleDelete(parts);
                        break;
                    case "quit":
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
            output.println("END_OF_LIST");
            return;
        }

        File file = new File(currentDirectory, parts[1]);
        if (!isSubDirectory(file, BASE_DIR)) {
            output.println("ERROR Access denied: File outside base directory");
            output.println("END_OF_LIST");
            return;
        }

        if (!file.exists() || !file.isFile()) {
            output.println("ERROR File not found: " + parts[1]);
            output.println("END_OF_LIST");
            return;
        }

        try {
            output.println("SIZE " + file.length());
            output.flush();
            
            BufferedOutputStream dataOutput = new BufferedOutputStream(clientSocket.getOutputStream());
            BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                dataOutput.write(buffer, 0, bytesRead);
            }
            
            dataOutput.flush();
            fis.close();
            
            // Wait a bit before sending END_OF_LIST to ensure data is transferred
            Thread.sleep(100);
            output.println("END_OF_LIST");
            output.flush();
        } catch (IOException | InterruptedException e) {
            output.println("ERROR Failed to send file: " + e.getMessage());
            output.println("END_OF_LIST");
        }
    }

    private void handlePut(String[] parts) throws IOException {
        if (parts.length != 3) {
            output.println("ERROR Usage: put <filename> <size>");
            output.println("END_OF_LIST");
            return;
        }

        String filename = parts[1];
        long size;
        try {
            size = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            output.println("ERROR Invalid file size");
            output.println("END_OF_LIST");
            return;
        }

        // Sanitize the filename and create full path
        filename = sanitizeFilename(filename);
        File file = new File(currentDirectory, filename);

        // Check if the target location is within base directory
        if (!isSubDirectory(file, BASE_DIR)) {
            output.println("ERROR Access denied: Cannot write outside base directory");
            output.println("END_OF_LIST");
            return;
        }

        try {
            output.println("READY");
            output.flush();

            BufferedInputStream dataInput = new BufferedInputStream(clientSocket.getInputStream());
            BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[8192];
            long received = 0;
            
            while (received < size) {
                int bytesToRead = (int) Math.min(buffer.length, size - received);
                int bytesRead = dataInput.read(buffer, 0, bytesToRead);
                
                if (bytesRead == -1) {
                    fos.close();
                    file.delete();
                    output.println("ERROR File transfer incomplete");
                    output.println("END_OF_LIST");
                    return;
                }
                
                fos.write(buffer, 0, bytesRead);
                received += bytesRead;
            }
            
            fos.close();
            
            output.println("File uploaded successfully");
            output.println("END_OF_LIST");
        } catch (IOException e) {
            output.println("ERROR Failed to receive file: " + e.getMessage());
            output.println("END_OF_LIST");
            if (file.exists()) {
                file.delete();  // Clean up partial file
            }
        }
    }

    private void handleLs() {
        try {
            Files.list(currentDirectory.toPath())
                .forEach(path -> {
                    File file = path.toFile();
                    output.println(file.getName() + (file.isDirectory() ? "/" : ""));
                });
        } catch (IOException e) {
            output.println("ERROR: Unable to list directory");
        }
        output.println("END_OF_LIST");
    }

    private void handleCd(String[] parts) throws IOException {
        if (parts.length == 1) {
            // If only "cd" is entered, reset to base directory
            currentDirectory = BASE_DIR;
            output.println("Changed to base directory: " + currentDirectory.getCanonicalPath());
            output.println("END_OF_LIST");
            return;
        }
        
        if (parts.length != 2) {
            output.println("Usage: cd <directory>");
            output.println("END_OF_LIST");
            return;
        }

        try {
            File newDir;
            String path = parts[1];

            if (path.equals("..")) {
                // Going up one directory
                newDir = currentDirectory.getParentFile();
                if (newDir == null || !isWithinBaseDir(newDir)) {
                    output.println("Cannot go above base directory");
                    output.println("END_OF_LIST");
                    return;
                }
            } else if (path.equals(".")) {
                // Stay in current directory
                newDir = currentDirectory;
            } else {
                // Change to specified directory
                newDir = new File(currentDirectory, path).getCanonicalFile();
                if (!isWithinBaseDir(newDir)) {
                    output.println("Cannot access directory outside base directory");
                    output.println("END_OF_LIST");
                    return;
                }
            }

            // Check if directory exists and is actually a directory
            if (!newDir.exists() || !newDir.isDirectory()) {
                output.println("Directory does not exist");
                output.println("END_OF_LIST");
                return;
            }

            currentDirectory = newDir;
            output.println("Changed to: " + currentDirectory.getCanonicalPath());
            output.println("END_OF_LIST");

        } catch (IOException e) {
            output.println("ERROR: Invalid directory path");
            output.println("END_OF_LIST");
        }
    }

    
    private boolean isWithinBaseDir(File dir) {
        try {
            String basePath = BASE_DIR.getCanonicalPath();
            String targetPath = dir.getCanonicalPath();
            return targetPath.equals(basePath) || targetPath.startsWith(basePath + File.separator);
        } catch (IOException e) {
            return false;
        }
    }





    
    private void handlePwd() {
        try {
            output.println(currentDirectory.getCanonicalPath());
        } catch (IOException e) {
            output.println("ERROR: Unable to determine current directory");
        }
        output.println("END_OF_LIST");
    }

    private void handleMkdir(String[] parts) {
        if (parts.length != 2) {
            output.println("Usage: mkdir <directory>");
            output.println("END_OF_LIST");
            return;
        }

        File newDir = new File(currentDirectory, sanitizeFilename(parts[1]));
        
        if (!isSubDirectory(newDir, BASE_DIR)) {
            output.println("ERROR: Cannot create directory outside base directory");
            output.println("END_OF_LIST");
            return;
        }

        if (newDir.mkdir()) {
            output.println("Directory created");
        } else {
            output.println("Could not create directory");
        }
        output.println("END_OF_LIST");
    }

    private void handleDelete(String[] parts) {
        if (parts.length != 2) {
            output.println("Usage: delete <filename>");
            output.println("END_OF_LIST");
            return;
        }

        File file = new File(currentDirectory, parts[1]);
        
        if (!isSubDirectory(file, BASE_DIR)) {
            output.println("ERROR: Cannot delete files outside base directory");
            output.println("END_OF_LIST");
            return;
        }

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

    // Helper method to check if a path is within the base directory
    private boolean isSubDirectory(File child, File parent) {
        try {
            String parentPath = parent.getCanonicalPath() + File.separator;
            String childPath = child.getCanonicalPath();
            return childPath.startsWith(parentPath);
        } catch (IOException e) {
            return false;
        }
    }

    // Helper method to sanitize filenames
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java myftpserver <port>");
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);
            myftpserver server = new myftpserver(port);
            server.run();
        } catch (NumberFormatException e) {
            System.out.println("Error: Port must be a valid number");
        }
    }
}
