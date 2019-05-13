package webserver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


/*
    Helper class for implementing the operations logic
 */
public class Helper {

    private static final String ERROR_DIR = "www/html/error_pages";

    private final String rootDirectory;
    private String path;

    private Map<String, String> requestData;
    private Map<String, String> responseData;

    private Map<String, ReentrantLock> locks;


    public Helper(Map<String, String> requestData, Map<String, String> responseData, String rootDirectory) {
        this.requestData = requestData;
        this.responseData = responseData;
        this.rootDirectory = rootDirectory;
        this.locks = new HashMap<>();
    }


    // method that returns the resource path relative to the project directory
    private void setPath(String resource) {
        this.path = this.rootDirectory + resource;
    }


    //method implementing the HTTP request operations
    void applyAction() {
        // append protocol to the response
        this.responseData.put("Protocol", requestData.getOrDefault("Protocol", "HTTP/1.1"));

        // if Code is present in request data, the request is invalid and BadRequest page is returned
        if (this.requestData.containsKey("Code")) {
            this.readPage(Helper.ERROR_DIR + "/BadRequest.html", true);

        } else if (this.requestData.get("Method").equals("OPTIONS")) {
            // if method is OPTIONS, append the Allow header with all methods accepted by HTTPParser class
            this.responseData.put("Code", ErrorCodes.NO_CONTENT);
            String methods = Arrays.toString(HTTPParser.methods);
            this.responseData.put("Allow", methods.substring(1, methods.length() - 1));

        } else {
            // else set the path and create the file instance
            this.setPath(this.requestData.get("Resource"));
            File file = new File(this.path);

            // if file does not exist and method cannot create a file (is not POST) return File Not Found
            if ((!file.exists() && !this.requestData.get("Method").equals("POST")) || file.isDirectory()) {
                this.fileNotFound();
            } else if (file.getName().startsWith(this.rootDirectory + Helper.ERROR_DIR)) {
                // if the requested resource is a restricted page (error) return Forbidden
                this.responseData.put("Code", ErrorCodes.FORBIDDEN);
                this.readPage(Helper.ERROR_DIR + "/Forbidden.html", true);
            } else {
                // finally call the operation method based on the HTTP method
                switch (this.requestData.get("Method")) {
                    case "GET":
                        this.readPage(this.path, true);
                        break;
                    case "HEAD":
                        this.readPage(this.path, false);
                        break;
                    case "POST":
                    case "PUT":
                        this.writePage(false);
                        break;
                    case "PATCH":
                        this.writePage(true);
                        break;
                    case "DELETE":
                        this.deletePage();
                        break;
                }
            }
        }
    }


    // method implementing a GET-style behaviour
    private void readPage(String filePath, boolean appendBody) {
        boolean removeLock = false;
        File resourceFile = new File(filePath);
        String fileContent;

        // create a lock for the file if does not exist
        if (!this.locks.containsKey(filePath)) {
            this.locks.put(filePath, new ReentrantLock());
        }

        // acquire the file's lock
        this.locks.get(filePath).lock();

        try {
            // Not Found error page returned if the file does not exist
            if (!resourceFile.exists()) {
                this.fileNotFound();
                removeLock = true;
            } else {
                // if the request should have a body (is not HEAD)
                if (appendBody) {
                    fileContent = this.readFile(resourceFile);
                    this.responseData.put("Body", fileContent);
                    this.responseData.put("Content-Length", String.valueOf(fileContent.getBytes().length));
                }
                // append other necessary headers
                this.responseData.put("Content-Type", Files.probeContentType(Path.of(filePath)));

                if (!this.responseData.containsKey("Code")) {
                    this.responseData.put("Code", ErrorCodes.OK);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read the requested file.");
            this.internalServerError();
        } finally {
            // release the lock on the file
            this.locks.get(filePath).unlock();
            // if the file was deleted, removes its lock
            if (removeLock) {
                this.locks.remove(filePath);
            }
        }
    }


    // method implementing a POST-style behaviour
    private void writePage(boolean flag) {
        boolean removeLock = false;
        boolean fileExists = true;
        File resourceFile = new File(this.path);
        String fileContent;

        // create a lock for the file if does not exist
        if (!this.locks.containsKey(this.path)) {
            this.locks.put(this.path, new ReentrantLock());
        }

        // acquire the file's lock
        this.locks.get(this.path).lock();

        // set the fileExists flag to false if file does not exist
        if (!resourceFile.exists()) {
            fileExists = false;
        }

        try {
            // if file does not exist and method is not POST, then return File Not Found page
            if (!fileExists && !this.requestData.get("Method").equals("POST")) {
                this.fileNotFound();
                removeLock = true;

            } else {
                // else write the file with data from body
                if (this.requestData.containsKey("Body")) {
                    FileWriter writer = new FileWriter(resourceFile, flag);
                    writer.write(this.requestData.get("Body"));
                    writer.close();
                }

                fileContent = this.readFile(resourceFile);  // read the file (as it could have been appended to)

                // if the file is not empty, append its content and content related headers to response
                if (!fileContent.isEmpty()) {
                    this.responseData.put("Content-Type", Files.probeContentType(Path.of(path)));
                    this.responseData.put("Body", fileContent);
                    this.responseData.put("Content-Length", String.valueOf(fileContent.getBytes().length));
                }

                // add the response code
                if (!fileExists && this.requestData.get("Method").equals("POST")) {
                    this.responseData.put("Code", ErrorCodes.CREATED);
                } else if (!fileContent.isEmpty()){
                    this.responseData.put("Code", ErrorCodes.OK);
                } else {
                    this.responseData.put("Code", ErrorCodes.NO_CONTENT);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not write in the requested file.");
            this.internalServerError();
        } finally {
            // release the lock on the file
            this.locks.get(this.path).unlock();
            // if the file was deleted, removes its lock
            if (removeLock) {
                this.locks.remove(this.path);
            }
        }
    }


    // method implementing a DELETE-style behaviour
    private void deletePage() {
        boolean removeLock = false;
        File resourceFile = new File(this.rootDirectory + this.requestData.get("Resource"));

        // create a lock for the file if does not exist
        if (!this.locks.containsKey(this.path)) {
            this.locks.put(this.path, new ReentrantLock());
        }

        // acquire the file's lock
        this.locks.get(this.path).lock();

        try {
            // if file does not exist return File Not Found page
            if (!resourceFile.exists()) {
                this.fileNotFound();
                removeLock = true;

            } else if (!resourceFile.delete()) {    // delete the file
                System.err.println("Could not delete the requested file.");
                this.internalServerError();

            } else {                                // if file deleted successfully, append 2xx code
                this.responseData.put("Code", ErrorCodes.NO_CONTENT);
            }
        } finally {
            // release the lock on the file
            this.locks.get(this.path).unlock();
            // if the file was deleted, removes its lock
            if (removeLock) {
                this.locks.remove(this.path);
            }
        }
    }


    // method returning a String instance of a file content
    private String readFile(File resourceFile) {
        StringBuilder fileContent = new StringBuilder();
        String line;
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(resourceFile));
            while ((line = reader.readLine()) != null) {
                fileContent.append(line);
            }
            reader.close();
        } catch (Exception e) {
            return "";
        }

        return fileContent.toString();
    }


    // method appending File not Found headers and body
    private void fileNotFound() {
        this.responseData.put("Code", ErrorCodes.NOT_FOUND);
        this.readPage(Helper.ERROR_DIR + "/NotFound.html", true);
    }


    // method appending Internal Server Error headers and body
    private void internalServerError() {
        this.responseData.put("Code", ErrorCodes.ISE);
        this.readPage(Helper.ERROR_DIR + "/InternalServerError.html", true);
    }
}


class ErrorCodes {
    static final String OK = "200 OK";
    static final String CREATED = "201 Created";
    static final String NO_CONTENT = "204 No Content";
    static final String BAD = "400 Bad Request";
    static final String FORBIDDEN = "403 Forbidden";
    static final String NOT_FOUND = "404 Not Found";
    static final String ISE = "500 Internal Server Error";
}
