package server;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import http_message.HTTPRequest;
import http_message.HTTPResponse;
import util.BadRequestException;
import util.CommandNotFoundException;

import javax.imageio.ImageIO;

import static http_message.HTTPMessage.CRLF;

/**
 * This class processes the request of the client's request
 * and give a proper response back.
 */
public class ServerThread extends Thread {
	private Socket socket;
	private BufferedReader inputFromClient;
	private DataOutputStream outToClient;
    private BufferedImage imageToGet;
    private OutputStream outToClient2;
    private Boolean flushFile;
    private Path currentRelativePath = Paths.get("");
    private String s = currentRelativePath.toAbsolutePath().toString() + "\\files\\";

	ServerThread(Socket socket) throws IOException {
	    // Set up streams
		this.socket = socket;
		inputFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outToClient = new DataOutputStream(socket.getOutputStream());
        outToClient2 = new DataOutputStream(socket.getOutputStream());
	}

	public void run() {
		Boolean open = true;
		while (open) {
		    Boolean flushFile = false;
		    Boolean badRequest = false;
		    HTTPResponse response = new HTTPResponse();
		    HTTPRequest request = null;
            try {
                // Define request
                request = getRequest();
                System.out.println(request.toString());
            } catch (Exception e) {
                response.setStatusLine("HTTP/1.1 400 Bad Request");
                badRequest = true;
            }

            if(!badRequest) {
                try {
                    // Define response
                    response = getResponse(request);
                    System.out.println(response);
                } catch (Exception e) {
                    response.setStatusLine("HTTP/1.1 500 Server Error");
                }
            }

            try {
                // Output to client
                outToClient.writeBytes(response.toString());
                if (flushFile) {
                    ImageIO.write(imageToGet, "png", socket.getOutputStream());
                }
            } catch (Exception e) {
                System.out.println("ERROR");
            }
		}
	}

    /**
     * Make a proper request with the input from client.
     * Defines the method, the headers and the body of the client's input.
     * @return A defined request.
     * @throws CommandNotFoundException
     *          If the method of the request is not implemented.
     * @throws IOException
     * @throws BadRequestException
     *          If the request has no host-header.
     *
     */
	private HTTPRequest getRequest() throws CommandNotFoundException, IOException, BadRequestException {
		String requestLine;
		HTTPRequest request;

		//Method
		for (;;) {
			if ((requestLine = inputFromClient.readLine()) != null) {
				request = new HTTPRequest(requestLine);
				break;
			}
		}

		// Headers
		while((requestLine = inputFromClient.readLine()).length() != 0) {
		    request.addHeader(requestLine);
        }
        if (request.getHeader("Host") == null) {
            throw new BadRequestException();
        }

        // Body
        String method = request.getMethod();
		if (method.equals("GET") || method.equals("HEAD"))
		    return request;
		else {
		    int contentLength = request.getContentLength();
		    if (contentLength != -1) {
		        char[] body = new char[contentLength];
		        for (int i=0; i < contentLength; i++){
		            body[i] = (char) inputFromClient.read();
                }
                request.setBody(new String(body));
            }
            else {
		        StringBuilder body = new StringBuilder();
		        while((requestLine = inputFromClient.readLine()).length() != 0) {
		            body.append(requestLine);
		            body.append(CRLF);
                }
                request.setBody(body.toString());
            }

            return request;
        }
	}

    /**
     * Makes a proper response to the request of the client.
     * This responses has a status line, headers and a potential body.
     * @param clientRequest
     *         The request from the client
     * @return A response to the request of the client
     * @throws ParseException
     * @throws IOException
     */
	private HTTPResponse getResponse(HTTPRequest clientRequest) throws ParseException, IOException {
	    HTTPResponse serverResponse = new HTTPResponse();

        // Date header
        Date date= new Date();
        SimpleDateFormat dateTemplate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz" , Locale.ENGLISH);
        dateTemplate.setTimeZone(TimeZone.getTimeZone("GMT"));
        serverResponse.addHeader("Date", dateTemplate.format(date));

        // Content-type header
        String path =clientRequest.getPath();
        if (path.equals("") || path.endsWith(".html")) {
            serverResponse.addHeader("Content-Type", "text/html");
        }
        else if (path.endsWith(".txt")) {
            serverResponse.addHeader("Content-Type", "text/plain");
        }
        else {
            serverResponse.addHeader("Content-Type", "image" + "/" + clientRequest.getPath());
        }

        // Methods
        switch (clientRequest.getMethod()) {
            case "HEAD":
                serverResponse.setStatusLine("HTTP/1.1 200 OK");
                break;
            case "GET":
                methodGET(clientRequest, serverResponse);
                break;
            case "PUT":
                methodPUT(clientRequest, serverResponse);
                break;
            case "POST":
                methodPOST(clientRequest, serverResponse);
                break;
        }

        // Content-Length header
        int contentLength;
        if (serverResponse.getBody().equals("")) {
            contentLength = 0;
        }
        else {
            contentLength = serverResponse.getBody().length();
        }
        serverResponse.addHeader("Content-Length", Integer.toString(contentLength));

        return serverResponse;
    }

    /**
     * Processes the GET method.
     * @param clientRequest
     *         The request of the client
     * @param serverResponse
     *         The half-made response to the request of the client.
     * @throws ParseException
     *          If the date is wrongly defined
     * @throws IOException
     */
    private void methodGET(HTTPRequest clientRequest, HTTPResponse serverResponse) throws ParseException, IOException {
        File file = new File(s+ clientRequest.getPath());

        //Check if file exists
        if (!file.exists()) {
            serverResponse.setStatusLine("HTTP/1.1 404 Not Found");
            return;
        }
        //If-Modified-since
        String temp;
        if ((temp = clientRequest.getHeader("If-Modified-Since")) != null) {
            SimpleDateFormat parser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
            parser.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date ifModifiedDate = parser.parse(temp.toString());

            long lastModified = file.lastModified();
            Date dateLastModified = new Date(lastModified);

            if (ifModifiedDate.compareTo(dateLastModified) > 0) {
                serverResponse.setStatusLine("HTTP/1.1 304 Not Modified");
                return;
            }
        }

        String path =clientRequest.getPath();
        // Non-image files
        if (path.equals("") || path.endsWith(".html") || path.endsWith(".txt")) {
            BufferedReader fileToGet = new BufferedReader(new FileReader(s + path));
            serverResponse.setStatusLine("HTTP/1.1 200 OK");
            String message = org.apache.commons.io.IOUtils.toString(fileToGet);
            serverResponse.setBody(message);
            fileToGet.close();
        }
        // Image files
        else{
            BufferedImage imageToGet = ImageIO.read(new File(s + path));
            System.out.println(s + path);
            System.out.println(imageToGet);
//            ImageIO.write(imageToGet, "png", bos );
            flushFile = true;
            serverResponse.setStatusLine("HTTP/1.1 200 OK");
        }


    }

    /**
     * Processes the PUT method.
     * Create a new file or overwrite the file, with the given input, on the give path directory.
     * @param clientRequest
     *         The request of the client
     * @param serverResponse
     *         The half-made response to the request of the client.
     * @throws FileNotFoundException
     *          If there is no file in the path directory.
     */
    private void methodPUT(HTTPRequest clientRequest, HTTPResponse serverResponse) throws FileNotFoundException {
        // Create or overwrite file
        PrintWriter writer = new PrintWriter(s + clientRequest.getPath());
        writer.print(clientRequest.getBody());
        writer.close();
        serverResponse.setStatusLine("HTTP/1.1 200 OK");
    }

    /**
     * Processes the POST method
     * Create a new filen, with the given input, when there is no file on the path directory,
     * otherwise adds the given input to the bottom of the file.
     * @param clientRequest
     *         The request of the client
     * @param serverResponse
     *         The half-made response to the request of the client.
     * @throws IOException
     */
    private void methodPOST(HTTPRequest clientRequest, HTTPResponse serverResponse) throws IOException {
        File file = new File(s + clientRequest.getPath());

        // Look if file exists
        if (!file.exists()) {
            file.createNewFile();
        }

        // Add to file
        FileWriter writer = new FileWriter(file, true);
        writer.write(clientRequest.getBody());
        writer.flush();
        writer.close();
        serverResponse.setStatusLine("HTTP/1.1 200 OK");
    }

}
