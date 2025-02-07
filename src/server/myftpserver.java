// Server side
import java.io.*;
import java.net.*;

public class myftpserver {
    int portNum;                            // Port number
    ServerSocket serverS;
    Socket clientS;

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
            
            
            }

        } catch (IOException e) {
            System.out.println("Error: Could not listen to port: " + portNum);
            e.printStackTrace();
            
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
