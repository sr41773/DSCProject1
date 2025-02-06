import java.io.*;
import java.net.*;

public class FTPclient {

    int serverPort;
    String machineName;
    Socket clientS;

    public FTPclient(String machineName, int serverPort) {
        this.machineName = machineName;
        this.serverPort = serverPort;
    }

    public void clientConnect() {
        try {
            this.clientS = new Socket(machineName, serverPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientS.getInputStream()));

            System.out.println("Client connection established.");
            System.out.println("- Connected to server: " + clientS.getInetAddress().getHostAddress());
            System.out.println("- Port number: " + clientS.getPort());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientRun() {
       
        clientConnect();                        //estalish connection first
        BufferedReader userInput;
        String inputCommand = "";
        String prompt = "myftp> ";

        try {}
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
            FTPclient client = new FTPclient(serverName, serverPort);
            client.clientRun();
        }
        else {
            System.out.println("Usage: java FTPclient <serverMachineName> <portNumber>");
            System.exit(1);
        }
    }

}