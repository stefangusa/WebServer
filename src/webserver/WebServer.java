package webserver;

import java.util.Scanner;


/*
    Class implementing the main method (the entry point)
 */
public class WebServer {

    private static final int MINIMUM_PORT_NUMBER = 1024;
    private static final int MAXIMUM_NO_THREADS = 16;

    public static void main(String[] args) {
        int port;
        int maxNoThreads;
        Server server;
        Scanner scanner = new Scanner(System.in);

        if (args.length < 2) {
            System.out.println("Usage: java webserver.Server <port no> <maximum no of workers>");
            System.exit(0);
        }

        try {
            port = Integer.parseInt(args[0]);
            maxNoThreads = Integer.parseInt(args[1]);

            if (port < WebServer.MINIMUM_PORT_NUMBER) {
                System.out.println("Port number should be greater or equal to 1024");
                System.exit(0);
            }

            if (maxNoThreads > WebServer.MAXIMUM_NO_THREADS) {
                System.out.println("Maximum number of threads should be lower or equal to 16");
                System.exit(0);
            }

            // Start the server thread
            server = new Server(port, maxNoThreads);
            new Thread(server).start();

            // Wait for "Exit", "EXIT", "exit" etc. command in command-line to shutdown the server
            while (true) {
                String line = scanner.nextLine();
                if (line.toLowerCase().contentEquals("exit")) {
                    System.out.println("Shutting down the server");

                    server.shutdown(port);
                    break;
                }
            }

        } catch (NumberFormatException e) {
            System.out.println("Port number and maximum number of threads should be integers.");
        }
    }
}
