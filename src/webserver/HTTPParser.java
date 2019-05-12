package webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HTTPParser {

    static final String[] methods = {"GET", "HEAD", "PATCH", "POST", "DELETE", "PUT", "OPTIONS"};

    static Map<String, String> parseRequest(BufferedReader request) throws IOException {
        Map<String, String> data = new HashMap<>();
        String line;
        String[] lineContents;

        try {
            line = request.readLine();
            lineContents = line.split("\\s");

            if (lineContents.length != 3) {
                return badRequest();
            }

            if (!Arrays.asList(HTTPParser.methods).contains(lineContents[0])) {
                return badRequest();
            }
            data.put("Method", lineContents[0]);

            if (lineContents[1].equals("/")) {
                data.put("Resource", "/index.html");
            } else {
                data.put("Resource", lineContents[1]);
            }

            if (!lineContents[2].equals("HTTP/1.1") && !lineContents[2].equals("HTTP/1.0")) {
                return badRequest();
            }
            data.put("Protocol", lineContents[2]);

            while ((line = request.readLine()) != null && !(line.equals(""))) {
                if (!line.contains(": ")) {
                    return badRequest();

                } else {
                    lineContents = line.split(": ");
                    if (lineContents.length < 2) {
                        return badRequest();
                    }

                    if (lineContents[0].equals("Content-Length")) {
                        data.put(lineContents[0], lineContents[1]);
                    }
                }
            }

            if (data.containsKey("Content-Length")) {
                int length = Integer.parseInt(data.get("Content-Length"));
                char[] requestBody = new char[length];

                if (request.read(requestBody, 0, length) > 0) {
                    data.put("Body", new String(requestBody));
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read from the input stream.");
            throw new IOException();
        }

        return data;
    }


    private static Map<String, String> badRequest() {
        Map <String, String> data = new HashMap<>();
        data.put("Code", ErrorCodes.BAD);
        return data;
    }


    static byte[] createResponse(Map<String, String> responseData) {
        StringBuilder responseString = new StringBuilder();

        responseString.append(responseData.get("Protocol"))
                .append(" ")
                .append(responseData.get("Code"));

        if (responseData.containsKey("Allow")) {
            responseString.append("\r\nAllow: ")
                    .append(responseData.get("Allow"));
        }

        responseString.append("\r\nDate: ")
                .append(new Date().toString())
                .append("\r\nServer: webserver.WebServer")
                .append("\r\nConnection: Closed");

        if (responseData.containsKey("Body")) {
            responseString.append("\r\nContent-Length: ")
                    .append(responseData.get("Content-Length"))
                    .append("\r\nContent-Type: ")
                    .append(responseData.get("Content-Type"))
                    .append("\r\n\r\n")
                    .append(responseData.get("Body"));

        } else {
            responseString.append("\r\n\r\n");
        }

        return responseString.toString().getBytes();
    }

}
