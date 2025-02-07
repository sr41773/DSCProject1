// Client side
import java.io.*;
import java.net.*;

public class myftp {

    int serverPort;
    String machineName;
    Socket clientS;

    public myftp(String machineName, int serverPort) {
        this.machineName = machineName;
        this.serverPort = serverPort;
    }

    //to establish client connection
    public void clientConnect() {
        try {
            this.clientS = new Socket(machineName, serverPort);
            BufferedReader input = new BufferedReader(new InputStreamReader(clientS.getInputStream()));

            System.out.println("Client connection established.");
            System.out.println("- Connected to server: " + clientS.getInetAddress().getHostAddress());
            System.out.println("- Port number: " + clientS.getPort());
        }
        catch (IOException ex) {
            System.out.println("Error: Unable to establish client connection.");

        }
    }

    public void clientRun() {
        clientConnect();                        //estalish connection first
        
        try {
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(clientS.getOutputStream(), true);                     //output to server
        
            String inputCommand = "";
            String prompt = "myftp> ";

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

                out.println(inputCommand);

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