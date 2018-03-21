package server;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import http_message.HTTPRequest;
import http_message.HTTPResponse;
import util.BadRequestException;
import util.CommandNotFoundException;
import javax.imageio.ImageIO;

import static http_message.HTTPMessage.CRLF;
import static org.apache.commons.io.FilenameUtils.getExtension;

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
//    private Path currentRelativePath = Paths.get("");
//    private String s = currentRelativePath.toAbsolutePath().toString() + "\\files\\";

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
            } catch (BadRequestException | CommandNotFoundException | IOException | URISyntaxException e) {
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
                if (response.getHeader("Content-Type").contains("image")) {
                    byte[] body = Base64.getDecoder().decode(request.getBody());
                    outToClient.writeBytes(response.headString());

                    for (int i=0; i<body.length; i++) {
                        outToClient.write(body[i]);
                    }
                }

                else {
                    outToClient.writeBytes(request.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
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
	private HTTPRequest getRequest() throws CommandNotFoundException, IOException, BadRequestException, URISyntaxException {
		String requestLine;
		HTTPRequest request;

		//Method
		for (;;) {
			if ((requestLine = inputFromClient.readLine()) != null) {
			    String[] args = requestLine.split(" ");
			    URI uri = new URI(args[1]);
			    request = new HTTPRequest(args[0], uri);
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
//        // Date header
//        Date date= new Date();
//        SimpleDateFormat dateTemplate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz" , Locale.ENGLISH);
//        dateTemplate.setTimeZone(TimeZone.getTimeZone("GMT"));
//        serverResponse.addHeader("Date", dateTemplate.format(date));

        // Methods
        HTTPResponse response = null;
        switch (clientRequest.getMethod()) {
            case "HEAD":
                response = getHeadResponse(clientRequest);
                break;
            case "GET":
                response = getGetResponse(clientRequest);
                break;
//            case "PUT":
//                methodPUT(clientRequest, serverResponse);
//                break;
//            case "POST":
//                methodPOST(clientRequest, serverResponse);
//                break;
        }

        return response;
    }

    private File getResource(HTTPRequest request) {
        File file;
        if (request.getPath().equals("/") || request.getPath().equals("")) {
            file = new File("files/index.html");
        }

        else {
            file = new File(request.getPath());
            String extension = request.getPath().substring(request.getPath().lastIndexOf(".") + 1);
        }

        return file;
    }

    private HTTPResponse getHeadResponse(HTTPRequest request) {
        File resource = getResource(request);
        HTTPResponse response = new HTTPResponse();
        if (resource.exists()) {
            response.setStatusLine("HTTP/1.1 200 OK");
            response.addHeader("Content-Type: " + getContentType(resource));
            response.addHeader("Content-Length: " + resource.length());
        }

        else {
            response.setStatusLine("HTTP/1.1 404 Page not found");
            addCommonHeaders(response);
        }

        return response;
    }

    private HTTPResponse getGetResponse(HTTPRequest request) throws ParseException, IOException {
	    File resource = getResource(request);
        HTTPResponse response = new HTTPResponse();

        if (resource.exists()) {
            String temp;
            if ((temp = request.getHeader("If-Modified-Since")) != null) {
                SimpleDateFormat parser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
                parser.setTimeZone(TimeZone.getTimeZone("GMT"));
                Date ifModifiedDate = parser.parse(temp);
                Date lastModified = new Date(resource.lastModified());

                if (ifModifiedDate.compareTo(lastModified) > 0) {
                    response.setStatusLine("HTTP/1.1 304 Not Modified");
                }
            }
            String contentType = getContentType(resource);
            response.addHeader("Content-Type", contentType);
            response.addHeader("Content-Length", Long.toString(resource.length()));
            if (contentType.contains("text")) {
                BufferedReader reader = new BufferedReader(new FileReader(resource));
                response.setStatusLine("HTTP/1.1 200 OK");
                String message = org.apache.commons.io.IOUtils.toString(reader);
                response.setBody(message);
                reader.close();
            }

            else {
                byte[] imageBytes = Files.readAllBytes(resource.toPath());
                response.setBody(Base64.getEncoder().encodeToString(imageBytes));
                response.setStatusLine("HTTP/1.1 200 OK");
            }
        }

        else {
            response.setStatusLine("HTTP/1.1 404 Page not found");
            addCommonHeaders(response);
        }

        return response;
    }

//    /**
//     * Processes the PUT method.
//     * Create a new file or overwrite the file, with the given input, on the give path directory.
//     * @param clientRequest
//     *         The request of the client
//     * @param serverResponse
//     *         The half-made response to the request of the client.
//     * @throws FileNotFoundException
//     *          If there is no file in the path directory.
//     */
//    private void methodPUT(HTTPRequest clientRequest, HTTPResponse serverResponse) throws FileNotFoundException {
//        // Create or overwrite file
//        PrintWriter writer = new PrintWriter(s + clientRequest.getPath());
//        writer.print(clientRequest.getBody());
//        writer.close();
//        serverResponse.setStatusLine("HTTP/1.1 200 OK");
//    }
//
//    /**
//     * Processes the POST method
//     * Create a new filen, with the given input, when there is no file on the path directory,
//     * otherwise adds the given input to the bottom of the file.
//     * @param clientRequest
//     *         The request of the client
//     * @param serverResponse
//     *         The half-made response to the request of the client.
//     * @throws IOException
//     */
//    private void methodPOST(HTTPRequest clientRequest, HTTPResponse serverResponse) throws IOException {
//        File file = new File(s + clientRequest.getPath());
//
//        // Look if file exists
//        if (!file.exists()) {
//            file.createNewFile();
//        }
//
//        // Add to file
//        FileWriter writer = new FileWriter(file, true);
//        writer.write(clientRequest.getBody());
//        writer.flush();
//        writer.close();
//        serverResponse.setStatusLine("HTTP/1.1 200 OK");
//    }

    private void addCommonHeaders(HTTPResponse response) {
        response.addHeader("Authors: Bo Kleynen, Maarten Boogaerts");
        Date date = new Date();
        response.addHeader("Date: " + date.toString());
    }

    private String getContentType(File file) {
        String extension = getExtension(file.toString());

        switch (extension) {
            case "txt":
                return "text/plain";
            case "html":
                return "text/html";
            default:
                return "image/" + extension;
        }
    }

}
