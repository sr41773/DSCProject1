CSCI 6780: Distributed Computing Systems
========================================
Project 1: Simple FTP Client and Server
========================================

(a.)    Mrudang Patel          
        Shriya Rasale

(b.) File structure:
    DSCProject1 -- src
                    -- client
                        -- myftp.java
                    -- server
                        -- myftpserver.java
                -- bin
                    -- myftp.class
                    -- myftpserver.class
                -- README.md
                -- Programming-Project.pdf

> To compile and run client:
cd src\client
javac -d bin src/client/myftp.java
java -cp bin myftp <machine_name> <port_number>

> To compile and run server:
cd src\server
javac -d bin src/server/myftpserver.java
java -cp bin myftp <port_number>
 

