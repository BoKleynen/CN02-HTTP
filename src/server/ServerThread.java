package server;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import http_message.HTTPRequest;
import http_message.HTTPResponse;
import org.apache.commons.io.FileUtils;
import util.BadRequestException;
import util.CommandNotFoundException;

import static http_message.HTTPMessage.CRLF;
import static org.apache.commons.io.FilenameUtils.getExtension;

/**
 * This class processes the request of the client's request
 * and give a proper response back.
 */
public class ServerThread extends Thread {
	private Socket socket;
	private BufferedReader inFromClient;
	private DataOutputStream outToClient;

	ServerThread(Socket socket) throws IOException {
	    // Set up streams
		this.socket = socket;
		inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outToClient = new DataOutputStream(socket.getOutputStream());
	}

	@Override
	public void run() {
		Boolean open = true;

		while (open) {
		    HTTPResponse response;
		    HTTPRequest request;

            try {
                request = getRequest();
                System.out.println(request.toString());
            } catch (BadRequestException | CommandNotFoundException | IOException | URISyntaxException e) {
                try {
                    e.printStackTrace();
                    send400();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                break;
            }

            open = !"close".equals(request.getHeader("Connection"));

            try {
                // Define response
                response = getResponse(request);

                // Output to client
                if (response.getHeader("Content-Type") != null && response.getHeader("Content-Type").contains("image")) {
                    byte[] body = Base64.getDecoder().decode(response.getBody());
                    outToClient.writeBytes(response.headString());

                    for (int i=0; i<body.length; i++) {
                        outToClient.write(body[i]);
                        outToClient.flush();
                    }
                }

                else {
                    outToClient.writeBytes(response.toString());
                    outToClient.flush();
                }

                System.out.println(response.toString());

                if (!response.success()) {
                    break;
                }

            } catch (Exception e) {
                try {
                    send500();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                open = false;
                e.printStackTrace();
            }
		}

        try {
            socket.close();
            System.out.println("closed socket: " + socket.toString());
        } catch (IOException e) {
            e.printStackTrace();
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
            requestLine = inFromClient.readLine();
			if (requestLine != null && requestLine.length() != 0) {
			    String[] args = requestLine.split(" ");
			    System.out.println("req: " + requestLine);
			    URI uri = new URI(args[1]);
			    request = new HTTPRequest(args[0], uri);
				break;
			}
		}

		// Headers
		while((requestLine = inFromClient.readLine()).length() != 0) {
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
		            body[i] = (char) inFromClient.read();
                }
                request.setBody(new String(body));
            }
            else {
		        StringBuilder body = new StringBuilder();
		        while((requestLine = inFromClient.readLine()).length() != 0) {
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
        // Methods
        switch (clientRequest.getMethod()) {
            case "HEAD":
                return getHeadResponse(clientRequest);
            case "GET":
                return getGetResponse(clientRequest);
            case "PUT":
                return methodPUT(clientRequest);
            case "POST":
                return methodPOST(clientRequest);
        }

        return null;
    }

    private File getResource(HTTPRequest request) {
        File file;
        if (request.getPath().equals("/") || request.getPath().equals("")) {
            file = new File("files/index.html");
        }

        else {
            file = new File("files/" + request.getPath());
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
            response.addHeader("Connection", "Keep-Alive");
            addCommonHeaders(response);
        }

        else {
            response.setStatusLine("HTTP/1.1 404 Page not found");
            addCommonHeaders(response);
            response.addHeader("Connection", "close");
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
                    addCommonHeaders(response);
                    response.addHeader("Connection", "Keep-Alive");

                }
            }

            String contentType = getContentType(resource);
            response.addHeader("Content-Type", contentType);
            response.addHeader("Content-Length", Long.toString(resource.length()));

            if (contentType.contains("text")) {
                BufferedReader reader = new BufferedReader(new FileReader(resource));
                String message = org.apache.commons.io.IOUtils.toString(reader);
                response.setBody(message);
                reader.close();
                response.setStatusLine("HTTP/1.1 200 OK");
                addCommonHeaders(response);
                response.addHeader("Connection", "Keep-Alive");
            }

            else {
                byte[] imageBytes = Files.readAllBytes(resource.toPath());
                response.setBody(Base64.getEncoder().encodeToString(imageBytes));
                response.setStatusLine("HTTP/1.1 200 OK");
                addCommonHeaders(response);
                response.addHeader("Connection", "Keep-Alive");
            }
        }

        else {
            return error404();
        }

        return response;
    }

    /**
     * Processes the PUT method.
     * Create a new file or overwrite the file, with the given input, on the give path directory.
     * @param request
     *         The request of the client
     * @throws FileNotFoundException
     *          If there is no file in the path directory.
     */
    private HTTPResponse methodPUT(HTTPRequest request) throws IOException {
        File resource = getResource(request);
        HTTPResponse response = new HTTPResponse();

        FileUtils.writeStringToFile(resource, request.getBody());
        response.setStatusLine("HTTP/1.1 200 OK");
        addCommonHeaders(response);
        response.addHeader("Connection", "Keep-Alive");

        return response;
    }

    /**
     * Processes the POST method
     * Create a new filen, with the given input, when there is no file on the path directory,
     * otherwise adds the given input to the bottom of the file.
     * @param request
     *         The request of the client
     * @throws IOException
     */
    private HTTPResponse methodPOST(HTTPRequest request) throws IOException {
        File resource = getResource(request);
        HTTPResponse response = new HTTPResponse();

        // Add to file
        FileUtils.writeStringToFile(resource, request.getBody(), true);
        response.setStatusLine("HTTP/1.1 200 OK");
        addCommonHeaders(response);
        response.addHeader("Connection", "Keep-Alive");

        return response;
    }

    private void addCommonHeaders(HTTPResponse response) {
        response.addHeader("Authors: Bo Kleynen, Maarten Boogaerts");
        Date date = new Date();
        response.addHeader("Date", date.toString());
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

    private void send400() throws IOException {
        HTTPResponse response = new HTTPResponse();
        response.setStatusLine("HTTP/1.1 400 Not Found");
        addCommonHeaders(response);
        response.addHeader("Connection", "close");
        BufferedReader br = new BufferedReader(new FileReader("files/400BadRequest.html"));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            body.append(line);
            body.append(CRLF);
        }
        response.setBody(body.toString());
        response.addHeader("Content-Type", "text/html");
        response.addHeader("Content-Length", Integer.toString(response.getBody().length()));
        outToClient.writeBytes(response.toString());
    }

    private HTTPResponse error404() throws IOException {
        HTTPResponse response = new HTTPResponse();
        response.setStatusLine("HTTP/1.1 404 Not Found");
        addCommonHeaders(response);
        response.addHeader("Connection", "close");
        BufferedReader br = new BufferedReader(new FileReader("files/404NotFound.html"));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            body.append(line);
            body.append(CRLF);
        }
        response.setBody(body.toString());
        response.addHeader("Content-Type", "text/html");
        response.addHeader("Content-Length", Integer.toString(response.getBody().length()));

        return response;
    }

    private void send500() throws IOException {
        HTTPResponse response = new HTTPResponse();
        response.setStatusLine("HTTP/1.1 500 Not Found");
        addCommonHeaders(response);
        response.addHeader("Connection", "close");
        BufferedReader br = new BufferedReader(new FileReader("files/500InternalServerError.html"));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            body.append(line);
            body.append(CRLF);
        }
        response.setBody(body.toString());
        response.addHeader("Content-Type", "text/html");
        response.addHeader("Content-Length", Integer.toString(response.getBody().length()));
        outToClient.writeBytes(response.toString());
    }

}
