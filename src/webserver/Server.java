package webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;


/*
    Class implementing the server behaviour
 */
public class Server implements Runnable {

    private static final int TIMEOUT = 5;
    private static final String ROOT = "www/html";
    private static final String SERVER = "127.0.0.1";

    private final int runningPort;
    private final int noWorkers;

    private boolean running;

    private ServerSocket socketServer;
    private ExecutorService workersPool;

    Server(int port, int maxNoThreads) {
        this.runningPort = port;
        this.noWorkers = maxNoThreads;
        this.running = false;
    }

    @Override
    public void run() {
        try {
            this.socketServer = new ServerSocket(this.runningPort);
            this.workersPool = Executors.newFixedThreadPool(this.noWorkers);
            this.running = true;

            System.out.println("Server has started.");

            // Server logic
            while (true) {
                Socket connectionSocket;

                try {
                    connectionSocket = socketServer.accept();   // waiting for connections

                    // if the running flag is changed to false, the server stops execution
                    if (!this.running) {
                        this.finish();
                        break;
                    }
                    System.out.println("New connection from " + connectionSocket);

                    // execute connection's logic on a new worker thread in the workers pool
                    this.workersPool.execute(new Connection(connectionSocket, Server.ROOT));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            System.err.println("Port unavailable");
        }
    }


    /*
        Method where the server ends its execution
     */
    private void finish() {
        try {
            this.socketServer.close();  // closing the server's socket
        } catch (IOException e) {
            System.err.println("Could not close server's socket.");
        }

        this.workersPool.shutdown();

        while (true) {
            try {
                // waits until all the threads in the workers pool finish their execution
                if (this.workersPool.awaitTermination(Server.TIMEOUT, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                System.err.println("Error while waiting for the connections to finish.");
            }
            System.out.println("Waiting for the connections to finish");
        }
    }


    /*
        Method which sends shutdown signal to the server thread
     */
    void shutdown(int port) {
        Socket mockSocket;

        this.running = false;   // puts the running flag on false

        // creates a mock connection to move the thread from the socket waiting state (accept)
        try {
            mockSocket = new Socket(Server.SERVER, port);
            mockSocket.close();
        } catch (IOException e) {
            System.err.println("Could not create a mock socket while shutting down the server.");
        }
    }
}
