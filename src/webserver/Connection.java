package webserver;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/*
    Class implementing the connection behaviour
 */
public class Connection implements Runnable {

    private final Socket connectionSocket;
    private final String rootDirectory;

    private BufferedReader inputBuffer;
    private DataOutputStream outputBuffer;

    private Map<String, String> responseData;

    Connection(Socket socket, String dir) {
        this.connectionSocket = socket;
        this.rootDirectory = dir;
        this.responseData = new HashMap<>();
    }

    @Override
    public void run() {
        System.out.println(this.connectionSocket + " has started working.");

        Map<String, String> requestData;

        try {
            // get the input and output stream
            this.inputBuffer = new BufferedReader(new InputStreamReader(this.connectionSocket.getInputStream()));
            this.outputBuffer = new DataOutputStream(this.connectionSocket.getOutputStream());

            // parse the http request
            requestData = HTTPParser.parseRequest(this.inputBuffer);
            // apply the requested operations and filling in the requestData
            new Helper(requestData, this.responseData, this.rootDirectory).applyAction();
            // send the parsed response to the client
            this.outputBuffer.write(HTTPParser.createResponse(this.responseData));
            this.outputBuffer.flush();

            // close the streams and the socket
            this.inputBuffer.close();
            this.outputBuffer.close();
            this.connectionSocket.close();

        } catch (IOException e) {
            try {
                this.inputBuffer.close();
                this.outputBuffer.close();
                this.connectionSocket.close();
            } catch (IOException f) { }

        } finally {
            System.out.println(this.connectionSocket + " has stopped.");
        }
    }
}
