// Client side
import java.io.*;
import java.net.*;

public class myftp {

    int serverPort;
    String machineName;
    Socket clientS;
    BufferedReader input;
    PrintWriter output;

    public myftp(String machineName, int serverPort) {
        this.machineName = machineName;
        this.serverPort = serverPort;
    }

    //to establish client connection
    public void clientConnect() {
        try {
            this.clientS = new Socket(this.machineName, this.serverPort);
            input = new BufferedReader(new InputStreamReader(clientS.getInputStream()));     //input data stream
            output = new PrintWriter(clientS.getOutputStream(), true);                          //output data stream

            System.out.println("Client connection established.");
            System.out.println("- Connected to server: " + clientS.getInetAddress().getHostAddress());
            System.out.println("- Port number: " + clientS.getPort());
        }
        catch (IOException ex) {
            System.out.println("Error: Unable to establish successful client connection.");
        }
    }

    public void clientRun() {
        clientConnect();
        
        try {
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));        
            String inputCommand;
            String prompt = "myftp> ";
            
            while(true) {
                System.out.print(prompt);
                inputCommand = userInput.readLine();
                
                if (inputCommand == null || inputCommand.trim().isEmpty()) {
                    continue;  // Skip empty commands
                }
                
                if (inputCommand.equals("quit")) {
                    output.println("quit");
                    System.out.println("Quitting...");
                    break;
                }

                //splitting
                String[] cmdParts = inputCommand.split(" ");
                String cmd = cmdParts[0].toLowerCase();  // case checkign


                // HANDLE OTHER COMMANDS
                try {
                    if (cmd.equals("get")) {
                        handleGetCommand(cmdParts);
                    }
                    else if (cmd.equals("put")) {
                        handlePutCommand(cmdParts);
                    }
                    else {  
                        output.println(inputCommand);
                        readServerResponses();
                    }
                } catch (IOException e) {
                    System.out.println("Error processing command: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            closeConnections();
        }
    }


    //separate handlers needede for get and put
    private void handleGetCommand(String[] cmdParts) throws IOException {
        if (cmdParts.length != 2) {
            System.out.println("Usage: get <filename>");
            return;
        }
        output.println(String.join(" ", cmdParts));         //sending command to server after joining command parameters
        String response = input.readLine();
        
        if (response.startsWith("SIZE")) {
            getCommand_receiveFile(cmdParts[1]);
        } else {
            System.out.println(response);
            readServerResponses();  // until END_OF_LIST
        }
    }

    // put
    private void handlePutCommand(String[] cmdParts) throws IOException {
        if (cmdParts.length != 2) {
            System.out.println("Usage: put <filename>");
            return;
        }
        File file = new File(cmdParts[1]);
        if (!file.exists()) {
            System.out.println("Local file does not exist");
            return;
        }
        output.println(String.join(" ", cmdParts));
        String response = input.readLine();
        if (response.equals("READY")) {
            putCommand_sendFile(cmdParts[1]);
            readServerResponses();
        }
    }

//reading server responses
    public void readServerResponses() throws IOException {
        String response;
        while ((response = input.readLine()) != null) {
            if (response.equals("END_OF_LIST")) {
                break;
            }
            System.out.println(response);
        }
    }

    //end call
    public void closeConnections() {
        try {
            if (clientS != null && !clientS.isClosed()) {
                input.close();
                output.close();
                clientS.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing connections: " + e.getMessage());
        }
    }

    //receiving files sent from the server
    public void getCommand_receiveFile(String fileName) {
        try {
            String sizeResponse = input.readLine();
            if (!sizeResponse.startsWith("SIZE")) {                     //trying to checking size
                System.out.println("Error: Invalid size response from server");
                return;
            }
            
            long fileSize = Long.parseLong(sizeResponse.split(" ")[1]);
            FileOutputStream fileData = null;

            try {
                fileData = new FileOutputStream(fileName);
                byte[] buffer = new byte[4096];                     //4096
                int fileBytes;
                long fileBytesRead = 0;

                while (fileBytesRead < fileSize && 
                       (fileBytes = clientS.getInputStream().read(buffer, 0, 
                           (int)Math.min(buffer.length, fileSize - fileBytesRead))) > 0) {              //condition changed
                    fileData.write(buffer, 0, fileBytes);
                    fileBytesRead += fileBytes;
                }
                
                System.out.println("File received: " + fileName);
                readServerResponses();  // end with END_OF_LIST marker
            } finally {
                if (fileData != null) {
                    fileData.close();
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid file size received from server");
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }

    // to send files to machine
    public void putCommand_sendFile(String fileName) {
        FileInputStream fis = null;
        try {
            File file = new File(fileName);
            fis = new FileInputStream(file);                    // new file
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) > 0) {
                clientS.getOutputStream().write(buffer, 0, bytesRead);
            }
            
            clientS.getOutputStream().flush();
            readServerResponses();  // read server's confirmation
        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException e) {
                System.out.println("Error closing file: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        //Two CLI parameters
        String machineName = args[0];
        int serverPort = Integer.parseInt(args[1]);

        if (args.length == 2) {
            myftp client = new myftp(machineName, serverPort);
            client.clientRun();
        }
        else {
            System.out.println("Usage: java myftp <serverMachineName> <portNumber>");
            System.exit(1);
        }
    }

}