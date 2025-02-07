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
        clientConnect();                        //estalish connection first
        
        try {
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));        
            String inputCommand = "";
            String prompt = "myftp> ";
            
            String serverResponse;
            while(true) {
                System.out.print(prompt);

                try {
                    inputCommand = userInput.readLine();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                //ADD MORE COMMANDS

                if(inputCommand.equals("quit")) {
                    break;
                }

                output.println(inputCommand);                    //command to server      

                //serverResponse = input.readLine();                //input from server
                while((serverResponse = input.readLine()) != null) {                    //Handling server response    
                    if (serverResponse.equals("-----------")) {
                        break;
                    }
                    
                    System.out.println(serverResponse);
                    //break;       
                }

            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
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