// Server side
import java.io.*;
import java.net.*;

public class FTPserver {
    int portNum;                            // Port number
    ServerSocket serverS;
    Socket clientS;

    public FTPserver(int portNum) {
        this.portNum = portNum;
    }

    public void run() {
        this.sSocket = new ServerSocket(portNum);          
        System.out.println("Server is running..." + 
                            "\nListening to port number: " + portNum);
        
        while(true) {                                                   
            this.clientS = serverS.accept();                //Accepting client connection
            System.out.println("Client connected: " + clientS.getInetAddress().getHostAddress());
        }

    }





    //main
    public static void main(String[] args) {
        int inputPort = 8000; // default
        inputPort = Integer.parseInt(args[0]);              //user input

        if (args.length == 1) {
            FTPserver server = new FTPserver(inputPort);
            server.run();
        }
        else {
            System.out.println("Usage: java FTPserver <portNumber>");
            System.exit(1);
        }
    }
}
