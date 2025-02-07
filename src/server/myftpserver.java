// Server side
import java.io.*;
import java.net.*;

public class myftpserver {
    int portNum;                            // port number
    ServerSocket serverS;
    Socket clientS;
    BufferedReader input;
    PrintWriter output;

    public myftpserver(int portNum) {
        this.portNum = portNum;
    }

    public void run() {
        try {
            this.serverS = new ServerSocket(portNum);          
            System.out.println("""
                               Server is running...
                               Listening to port number: """ + portNum);
            
            while(true) {                                                   
                this.clientS = serverS.accept();                //accepting client connection
                System.out.println("Client connected: " + clientS.getInetAddress().getHostAddress());
                
                clientHandler(this.clientS);                          //handling client
            
            }

        } catch (IOException e) {
            System.out.println("Error: Could not listen to port: " + portNum);
            e.printStackTrace();
        }
    }

    private void clientHandler(Socket clientS) {
        try {
            //input and output stream handler for client socket
            input = new BufferedReader(new InputStreamReader(clientS.getInputStream()));
            output = new PrintWriter(clientS.getOutputStream(), true);

            String inputCommand;
            while ((inputCommand = input.readLine()) != null) {
                String[] cmdParts = inputCommand.split(" ");            //split for parameters
                String cmd = cmdParts[0];
                
                switch (cmd) {
                    case "get":
                        if (cmdParts.length == 2) {                 //initializing the number of command parameters
                            String fileName = cmdParts[1];
                            File file = new File(System.getProperty("user.dir"), fileName);
                            
                            if (!file.exists()) {
                                output.println("File does not exist");
                                output.println("END_OF_LIST");
                            } else {
                                output.println("SIZE " + file.length());                
                                getCommand_sendFile(fileName, output, clientS.getOutputStream());
                            }
                        } else {
                            output.println("Usage: get <filename>");
                            output.println("END_OF_LIST");
                        }
                        break;
                    case "put":
                        if (cmdParts.length == 2) {
                            String fileName = cmdParts[1];
                            output.println("READY");  // signal to client 
                            putCommand_receiveFile(fileName, clientS.getInputStream());
                        } else {
                            output.println("Usage: put <filename>");
                            output.println("END_OF_LIST");  
                        }
                        break;
                    case "pwd":
                        output.println("Current Directory: " + System.getProperty("user.dir"));
                        output.println("END_OF_LIST");  // added end marker
                        break;
                    case "ls":
                        String listing = lsCommand();                   //gets respective files
                                                            //issue; change "-----" to confirm end of list
                        output.println(listing);
                        if (!listing.endsWith("END_OF_LIST")) {
                            output.println("END_OF_LIST");
                        }
                        break;
                    case "delete":
                        if (cmdParts.length == 2) {
                            String fileName = cmdParts[1];
                            File fileToDelete = new File(System.getProperty("user.dir"), fileName);
                            if (fileToDelete.exists()) {
                                if (fileToDelete.delete()) {
                                    output.println("File deleted successfully");
                                } else {
                                    output.println("Failed to delete file");
                                }
                            } else {
                                output.println("File does not exist");
                            }
                            output.println("END_OF_LIST");
                        } else {
                            output.println("Usage: delete <filename>");
                            output.println("END_OF_LIST");
                        }
                        break;
                    case "cd":
                        if (cmdParts.length == 2) {
                            String dirName = cmdParts[1];
                            File newDir;
                            if (dirName.equals("..")) {
                                newDir = new File(System.getProperty("user.dir")).getParentFile();
                            } else {
                                newDir = new File(System.getProperty("user.dir"), dirName);
                            }
                            
                            if (newDir.exists() && newDir.isDirectory()) {
                                System.setProperty("user.dir", newDir.getAbsolutePath());
                                output.println("Directory changed to: " + System.getProperty("user.dir"));
                                output.println("END_OF_LIST");  
                            } else {
                                output.println("Directory does not exist");
                                output.println("END_OF_LIST");  
                            }
                        } else {
                            output.println("Usage: cd <directory> or cd ..");
                            output.println("END_OF_LIST");  
                        }
                        break;
                    case "mkdir":
                        if (cmdParts.length == 2) {
                            String dirName = cmdParts[1];
                            File newDir = new File(System.getProperty("user.dir"), dirName);
                            if (newDir.mkdir()) {
                                output.println("Directory created successfully");
                                output.println("END_OF_LIST");  
                            } else {
                                output.println("Failed to create directory");
                                output.println("END_OF_LIST");  
                            }
                        } else {
                            output.println("Usage: mkdir <directory>");
                            output.println("END_OF_LIST");  
                        }
                        break;
                    case "quit":
                        output.println("Quitting server...");
                        output.println("END_OF_LIST");  
                        break;
                    default:
                        output.println("Invalid command");
                        output.println("END_OF_LIST");  
                        break;
                }
            }
            
            if (clientS != null && !clientS.isClosed()) {       //disconnecting client
                input.close();              //data streams closed
                output.close();
                clientS.close();
            }

        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String lsCommand() {
        File currentDir = new File(System.getProperty("user.dir"));
        File[] files = currentDir.listFiles();

        String fileList = "";
        for (File file : files) {
            fileList += file.getName() + "\n";                          //gets list of files
        }
        fileList += "END_OF_LIST";  // fixed!! > changed from ----------- to be more specific
        return fileList;
    }

    public void getCommand_sendFile(String fileName, PrintWriter fileStatus, OutputStream fileData) {
        File file = new File(System.getProperty("user.dir"), fileName);
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];         //buffer size changed
            int fileReadBytes;
            
            while ((fileReadBytes = fis.read(buffer)) > 0) {
                fileData.write(buffer, 0, fileReadBytes);
            }
            fileData.flush();
            output.println("END_OF_LIST");

        } catch (IOException e) {
            fileStatus.println("Error: Could not send file.");
            fileStatus.println("END_OF_LIST");
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException e) {
                System.out.println("Error closing file stream: " + e.getMessage());
            }
        }
    }
    
    // receivnig from clietns end
    private void putCommand_receiveFile(String fileName, InputStream inputStream) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(System.getProperty("user.dir"), fileName));
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
            }
            
            output.println("File received successfully");
            output.println("END_OF_LIST");
        } catch (IOException e) {
            output.println("Error receiving file: " + e.getMessage());
            output.println("END_OF_LIST");
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (IOException e) {
                System.out.println("Error closing file: " + e.getMessage());
            }
        }
    }

    //main
    public static void main(String[] args) {
        int inputPort = 8000; // default
        inputPort = Integer.parseInt(args[0]);              //user input

        if (args.length == 1) {
            myftpserver server = new myftpserver(inputPort);
            server.run();
        }
        else {
            System.out.println("Usage: java myftpserver <portNumber>");
            System.exit(1);
        }
    }
}
