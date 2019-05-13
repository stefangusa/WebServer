WebServer - a minimal web server implementation

To run the server, run in the project root directory:
  java -cp WebServer.jar webserver.WebServer <port> <no of workers>
  
The implementation consists of the 'webserver' package and the following classes:

i. WebServer - the entry point, where the 'main' method is situated. It starts the server thread 
               and also ends it gracefully when the input "exit" is introduced at System.in.
               
ii. Server - which implements Runnable, is a thread running the server logic, opening a socket that 
             accepts connection from clients and instantiates them on a new thread in idle state
             from the thread pool.
             
iii. Connection - also implements Runnable and runs the connection logic. Inside it, the input stream 
                  is sent to be parsed, the operations resulted are sent to be applied and the 
                  response is sent to be parsed also and then written to the output stream.
                  
iv. HTTPParser - is the class where the request is parsed and relevant data (for the minimal 
                 implementation) is stored in a Map and the Map which stores the response data is 
                 parsed to a string and then converted to a byte array.
                 
v. Helper - is the class where the logic of the operations (creating, reading, writing, deleting files)
            is implemented and also, the response Map of data is filled in.
            
            
The capabilities of the web server implemented by this project are:

1. The connection is closed by the server after sending the response.

2. In case of a File Not Found error, Bad Request error, Forbidden error or Internal Server error, specific
   pages existent in 'www/html/error_pages' are sent as response.
  
3. The HTTP response header consists of the <Protocol> <Code> <Code message> line, date line, server line, 
   connection closed line and optional allow method line and content-type/length lines.
   
4. Method supported are:
  i)    GET - returns the requested resource content
  ii)   HEAD - returns the header of a supposed GET request
  iii)  POST - creates the requested resource or overwrites an existent one and returns its content
  iv)   PUT - overwrites the requested resource and returns its content
  v)    PATCH - append data to the requested resource and returns its content
  vi)   DELETE - deletes the requested resource and returns no content
  vii)  OPTIONS - adds the Allow header and returns no content
  
  Important: POST, PUT, PATCH requests with no data will behave as a GET request.
  
