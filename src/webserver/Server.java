package webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class Server implements Runnable {

    private static final int TIMEOUT = 5;
    private static final String ROOT = "www/html";

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

            while (true) {
                Socket connectionSocket;

                try {
                    connectionSocket = socketServer.accept();
                    System.out.println("New connection from " + connectionSocket);

                    if (!this.running) {
                        this.finish();
                        break;
                    }

                    this.workersPool.execute(new Connection(connectionSocket, Server.ROOT));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            System.err.println("Port unavailable");
        }
    }


    private void finish() {
        try {
            this.socketServer.close();
        } catch (IOException e) {
            System.err.println("Could not close server's socket.");
        }

        this.workersPool.shutdown();

        while (true) {
            try {
                if (this.workersPool.awaitTermination(Server.TIMEOUT, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                System.err.println("Error while waiting for the connections to finish.");
            }
            System.out.println("Waiting for the connections to finish");
        }
    }


    void shutdown(String server, int port) {
        Socket mockSocket;

        this.running = false;

        try {
            mockSocket = new Socket(server, port);
            mockSocket.close();
        } catch (IOException e) {
            System.err.println("Could not create a mock socket while shutting down the server.");
        }
    }
}
