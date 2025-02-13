// File: myftpserver.java
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

    public myftpserver(int portNum) {   // Use the current working directory as the base directory
        this.portNum = portNum;
        this.BASE_DIR = new File(System.getProperty("user.dir")).getAbsoluteFile();
        this.currentDirectory = BASE_DIR;
        
        if (!this.BASE_DIR.exists()) {  //if it doesn't exist create base directory usinf mkdir
            this.BASE_DIR.mkdirs();
        }
    }

    //start runing the server print message to user
    public void run() {
        try {
            serverSocket = new ServerSocket(portNum);
            System.out.println("Server is running...");
            // System.out.println("Listening on port: " + portNum);
            // System.out.println("Base directory: " + BASE_DIR.getAbsolutePath());

            while (true) {
                clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                command_handler(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Error: Could not listen on port " + portNum);
            e.printStackTrace();
        }
    }

    private void command_handler(Socket clientSocket) { //switch statements for the different commands required
        try {
            //for the input for commands
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintWriter(clientSocket.getOutputStream(), true);

            String command;
            while ((command = input.readLine()) != null) {
                String[] parts = command.split(" ");
                String cmd = parts[0].toLowerCase(); //lowercase to ensure user can enter in caps or not

                switch (cmd) { //switch statement
                    case "get":
                        get_cmd(parts);
                        break;
                    case "put":
                        put_cmd(parts);
                        break;
                    case "ls":
                        ls_cmd();
                        break;
                    case "cd":
                        cd_cmd(parts);
                        break;
                    case "pwd":
                        pwd_cmd();
                        break;
                    case "mkdir":
                        mkdir_cmd(parts);
                        break;
                    case "delete":
                        delete_cmd(parts);
                        break;
                    case "quit":
                        output.println("Bye. You quit.");
                        output.println("END_OF_LIST");
                        return;
                    default:
                        output.println("Unknown command"); //if user enters a command that isnt one of the above ones
                        output.println("END_OF_LIST");
                }
            }
        } catch (IOException e) { //error detection
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {   //close socket for client
                    input.close();
                    output.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing the client connection: " + e.getMessage());
            }
        }
    }

    //get command to send files to the client
    private void get_cmd(String[] parts) throws IOException {
        if (parts.length != 2) {
            output.println("ERROR Usage: get <filename>");
            output.println("END_OF_LIST");
            return;
        }

        File file = new File(currentDirectory, parts[1]);
        if (!isSubDirectory(file, BASE_DIR)) {                          //limit access to unauthorized files
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
            
            //two data streams for the data going to and from the client
            BufferedOutputStream dataOutput = new BufferedOutputStream(clientSocket.getOutputStream());
            BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[8192];           //test buffer size change from 4096 to 8192
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                dataOutput.write(buffer, 0, bytesRead);
            }
            
            dataOutput.flush();
            fis.close();
            
      
            Thread.sleep(100);                  //buffer time: waiting till the file transfer is complete
            output.println("END_OF_LIST");
            output.flush();
        } catch (IOException | InterruptedException e) {
            output.println("ERROR Failed to send file: " + e.getMessage());
            output.println("END_OF_LIST");
        }
    }

    //put command
    private void put_cmd(String[] parts) throws IOException {
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
 
        //clean and filter out file name
        File file = new File(currentDirectory, filename.replaceAll("[^a-zA-Z0-9.-]", "_"));
   
        //error handles
        if (!isSubDirectory(file, BASE_DIR)) {
            output.println("ERROR Access denied: Cannot write outside base directory");
            output.println("END_OF_LIST");
            return;
        }

        try {
            output.println("READY");
            output.flush();

            //sending data to and from client end
            BufferedInputStream dataInput = new BufferedInputStream(clientSocket.getInputStream());
            BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[8192]; //8192 <- 4096
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
                
                fos.write(buffer, 0, bytesRead);        //bytes being written to file
                received += bytesRead;
            }
            
            fos.close();
            
            output.println("File uploaded successfully");
            output.println("END_OF_LIST");
        } catch (IOException e) {
            output.println("ERROR Failed to receive file: " + e.getMessage());
            output.println("END_OF_LIST");
            if (file.exists()) {
                file.delete();  
            }
        }
    }

    //ls command - to list files in the current directory
    private void ls_cmd() {
        try {
            Files.list(currentDirectory.toPath()).forEach(path -> {
                    File file = path.toFile();
                    output.println(file.getName() + (file.isDirectory() ? "/" : ""));
            });
        
        } catch (IOException e) {
            output.println("ERROR: Unable to list directory");
        }
        output.println("END_OF_LIST");
    }

    //cd changes directory
    private void cd_cmd(String[] parts) throws IOException {
        if (parts.length == 1) {
         
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
               
                newDir = currentDirectory.getParentFile();          //handling parent dir
                if (newDir == null || !isWithinBaseDir(newDir)) {
                    output.println("Cannot go above base directory");
                    output.println("END_OF_LIST");
                    return;
                }
            } else if (path.equals(".")) { //current dir
            
                newDir = currentDirectory;
            } else {
                
                newDir = new File(currentDirectory, path).getCanonicalFile(); //abs file path to canonical
                if (!isWithinBaseDir(newDir)) {
                    output.println("Cannot access directory outside base directory");
                    output.println("END_OF_LIST");
                    return;
                }
            }

         
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

    //helper method
    private boolean isWithinBaseDir(File dir) {
        try {
            String basePath = BASE_DIR.getCanonicalPath();
            String targetPath = dir.getCanonicalPath();
            return targetPath.equals(basePath) || targetPath.startsWith(basePath + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    
    private void pwd_cmd() { //handles pwd show current directory
        try {
            output.println(currentDirectory.getCanonicalPath());
        } catch (IOException e) {
            output.println("ERROR: Unable to determine current directory");
        }
        output.println("END_OF_LIST");
    }

    private void mkdir_cmd(String[] parts) { //mkdir makes new directory
        if (parts.length != 2) {
            output.println("Usage: mkdir <directory>");
            output.println("END_OF_LIST");
            return;
        }

        File newDir = new File(currentDirectory, parts[1].replaceAll("[^a-zA-Z0-9.-]", "_"));
        
        if (!isSubDirectory(newDir, BASE_DIR)) {
            output.println("ERROR: Cannot create directory outside base directory");
            output.println("END_OF_LIST");
            return;
        }

        if (newDir.mkdir()) { //if there isnt a directory create one else output that you cant make it
            output.println("Directory created");
        } else {
            output.println("Could not create directory");
        }
        output.println("END_OF_LIST");
    }

    private void delete_cmd(String[] parts) { //delete command which deletes file or directory
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

        //if file is there and calls delete command delete it; if not exsisting then print error message
       
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

    //presence of sub direcroty
    private boolean isSubDirectory(File child, File parent) {
        try {
            String parentPath = parent.getCanonicalPath() + File.separator;
            String childPath = child.getCanonicalPath();
            return childPath.startsWith(parentPath);
        } catch (IOException e) {
            return false;
        }
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
