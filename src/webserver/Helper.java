package webserver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

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


    private void setPath(String resource) {
        this.path = this.rootDirectory + resource;
    }


    void applyAction() {
        this.responseData.put("Protocol", requestData.getOrDefault("Protocol", "HTTP/1.1"));

        if (this.requestData.containsKey("Code")) {
            this.readPage(Helper.ERROR_DIR + "/BadRequest.html", true);

        } else if (this.requestData.get("Method").equals("OPTIONS")) {
            this.responseData.put("Code", ErrorCodes.NO_CONTENT);

            String methods = Arrays.toString(HTTPParser.methods);
            this.responseData.put("Allow", methods.substring(1, methods.length() - 1));

        } else {
            this.setPath(this.requestData.get("Resource"));

            File file = new File(this.path);

            if ((!file.exists() && !this.requestData.get("Method").equals("POST")) || file.isDirectory()) {
                this.responseData.put("Code", ErrorCodes.NOT_FOUND);
                this.readPage(Helper.ERROR_DIR + "/NotFound.html", true);

            } else if (file.getName().startsWith(this.rootDirectory + Helper.ERROR_DIR)) {
                this.responseData.put("Code", ErrorCodes.FORBIDDEN);
                this.readPage(Helper.ERROR_DIR + "/Forbidden.html", true);

            } else {
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


    private void readPage(String filePath, boolean appendBody) {
        boolean removeLock = false;
        File resourceFile = new File(filePath);
        String fileContent;

        if (!this.locks.containsKey(filePath)) {
            this.locks.put(filePath, new ReentrantLock());
        }

        this.locks.get(filePath).lock();

        try {
            if (!resourceFile.exists()) {
                this.responseData.put("Code", ErrorCodes.NOT_FOUND);
                this.readPage(Helper.ERROR_DIR + "/NotFound.html", true);
                removeLock = true;

            } else {
                if (appendBody) {
                    fileContent = this.readFile(resourceFile);
                    this.responseData.put("Body", fileContent);
                    this.responseData.put("Content-Length", String.valueOf(fileContent.getBytes().length));
                }

                this.responseData.put("Content-Type", Files.probeContentType(Path.of(filePath)));

                if (!this.responseData.containsKey("Code")) {
                    this.responseData.put("Code", ErrorCodes.OK);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read the requested file.");
            this.responseData.put("Code", ErrorCodes.ISE);
            this.responseData.put("Resource", Helper.ERROR_DIR + "/InternalServerError.html");

        } finally {
            this.locks.get(filePath).unlock();
            if (removeLock) {
                this.locks.remove(filePath);
            }
        }
    }


    private void writePage(boolean flag) {
        boolean removeLock = false;
        boolean fileExists = true;
        File resourceFile = new File(this.path);
        String fileContent;

        if (!this.locks.containsKey(this.path)) {
            this.locks.put(this.path, new ReentrantLock());
        }

        this.locks.get(this.path).lock();

        if (!resourceFile.exists()) {
            fileExists = false;
        }

        try {
            if (!fileExists && !this.requestData.get("Method").equals("POST")) {
                this.responseData.put("Code", ErrorCodes.NOT_FOUND);
                this.readPage(Helper.ERROR_DIR + "/NotFound.html", true);
                removeLock = true;

            } else {
                FileWriter writer = new FileWriter(resourceFile, flag);
                writer.write(this.requestData.get("Body"));
                writer.close();

                fileContent = this.readFile(resourceFile);

                this.responseData.put("Content-Type", Files.probeContentType(Path.of(path)));
                this.responseData.put("Body", fileContent);
                this.responseData.put("Content-Length", String.valueOf(fileContent.getBytes().length));

                if (!fileExists && this.requestData.get("Method").equals("POST")) {
                    this.responseData.put("Code", ErrorCodes.CREATED);
                } else {
                    this.responseData.put("Code", ErrorCodes.OK);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not write in the requested file.");
            this.responseData.put("Code", ErrorCodes.ISE);
            this.responseData.put("Resource", Helper.ERROR_DIR + "/InternalServerError.html");

        } finally {
            this.locks.get(this.path).unlock();
            if (removeLock) {
                this.locks.remove(this.path);
            }
        }
    }

    private void deletePage() {
        boolean removeLock = false;
        File resourceFile = new File(this.rootDirectory + this.requestData.get("Resource"));

        if (!this.locks.containsKey(this.path)) {
            this.locks.put(this.path, new ReentrantLock());
        }

        this.locks.get(this.path).lock();

        try {
            if (!resourceFile.exists()) {
                this.responseData.put("Code", ErrorCodes.NOT_FOUND);
                this.readPage(Helper.ERROR_DIR + "/NotFound.html", true);
                removeLock = true;

            } else if (!resourceFile.delete()) {
                System.err.println("Could not delete the requested file.");
                this.responseData.put("Code", ErrorCodes.ISE);
                this.responseData.put("Resource", Helper.ERROR_DIR + "/InternalServerError.html");

            } else {
                this.responseData.put("Code", ErrorCodes.OK);
            }
        } finally {
            this.locks.get(this.path).unlock();
            if (removeLock) {
                this.locks.remove(this.path);
            }
        }
    }

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
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            return "";
        }

        return fileContent.toString();
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
