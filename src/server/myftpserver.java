// Server side
import java.io.*;
import java.net.*;

public class myftpserver {
    int portNum;                            // Port number
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
                this.clientS = serverS.accept();                //Accepting client connection
                System.out.println("Client connected: " + clientS.getInetAddress().getHostAddress());
                
                clientHandler(this.clientS);                          //Handling client
            
            }

        } catch (IOException e) {
            System.out.println("Error: Could not listen to port: " + portNum);
            e.printStackTrace();
        }
    }

    private void clientHandler(Socket clientS) {                        //processing commands on the server
        try {
            input = new BufferedReader(new InputStreamReader(clientS.getInputStream()));            //input data stream
            output = new PrintWriter(clientS.getOutputStream(), true);                                 //output data stream

            String inputCommand = "";                       //command at the terminal
            inputCommand = input.readLine();

            while (inputCommand != null) { 
                if (inputCommand.equals("pwd")) {
                    output.println("Current Directory: " + System.getProperty("user.dir"));
                    output.flush();
                }
                else if (inputCommand.equals("ls")) {
                    //System.out.println("printing: " + lsCommand());
                    output.println(lsCommand());
                    output.flush();
                }
                else if (inputCommand.equals("quit")) {
                    output.println("Quitting server...");
                    output.flush();
                    break;
                }
                else {
                    output.println("Invalid command");
                    output.flush();
                }
            }

        } catch (Exception e) {
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
        fileList += "-----------";
        return fileList;
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
