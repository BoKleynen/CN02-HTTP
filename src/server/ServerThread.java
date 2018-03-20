package server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import http_message.HTTPRequest;
import http_message.HTTPResponse;
import util.BadRequestException;
import util.CommandNotFoundException;
import static http_message.HTTPMessage.CRLF;

public class ServerThread extends Thread {
	private Socket socket;
	private BufferedReader inputFromClient;
	private DataOutputStream outToClient;
    private Path currentRelativePath = Paths.get("");
    private String s = currentRelativePath.toAbsolutePath().toString() + "/files";

	ServerThread(Socket socket) throws IOException {
		this.socket = socket;
		inputFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outToClient = new DataOutputStream(socket.getOutputStream());
	}

	public void run() {
		Boolean open = true;
		while (open) {
		    try{
                HTTPRequest request = getRequest();
                HTTPResponse response = getResponse(request);
                outToClient.writeBytes(response.toString());
                if ("close".equals(request.getHeader("Connection"))){
    				open = false;
    				socket.close();
    			}
            }catch (Exception e){
                HTTPResponse response = new HTTPResponse();
		        if (e instanceof BadRequestException){
                    response.setStatusLine("HTTP\1.1 400 Bad Request");
                }
                else {
		            response.setStatusLine("HTTP\1.1 500 Server Error");
                }
                try {
                    outToClient.writeBytes(response.toString());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
		}
	}

	private HTTPRequest getRequest() throws CommandNotFoundException, IOException, BadRequestException {
		String requestLine;
		HTTPRequest request;
		for (;;) {
			if ((requestLine = inputFromClient.readLine()) != null) {
				request = new HTTPRequest(requestLine);
				break;
			}
		}

		while((requestLine = inputFromClient.readLine()).length() != 0) {
		    request.addHeader(requestLine);
        }
        if (request.getHeader("Host") == null) {
            throw new BadRequestException();
        }

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

	private HTTPResponse getResponse(HTTPRequest clientRequest) throws ParseException, IOException {
	    HTTPResponse serverResponse = new HTTPResponse();

        // DATE HEADER
        Date date= new Date();
        SimpleDateFormat dateTemplate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        serverResponse.addHeader("Date", dateTemplate.format(date));

        // CONTENT-TYPE HEADER
        if (clientRequest.getExtension().equals("") || clientRequest.getExtension().equals("html")) {
            serverResponse.addHeader("Content-Type", "text/html");
        }
        else {
            serverResponse.addHeader("Content-Type", "image/" + clientRequest.getExtension());
        }

        // METHODS
        switch (clientRequest.getMethod()) {
            case "HEAD":
                serverResponse.setStatusLine("HTTP\1.1 200 OK");
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

        // CONTENT-LENGTH
        int contentLength;
        if (clientRequest.getMessageBody().equals("")) {
            contentLength = 0;
        }
        else {
            contentLength = clientRequest.getMessageBody().length();
        }
        serverResponse.addHeader("Content-Length", Integer.toString(contentLength));

        return serverResponse;
    }

    private void methodGET(HTTPRequest clientRequest, HTTPResponse serverResponse) throws ParseException, IOException {
        File file = new File(s+ clientRequest.getPath());
        //Check if file exists
        if (!file.exists()) {
            serverResponse.setStatusLine("HTTP\1.1 404 Not Found");
            return;
        }
        //If-Modified-since
        Date ifModifiedDate;
        String temp;
        if ((temp = clientRequest.getHeader("If-Modified-Since")) != null) {
            ifModifiedDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").parse(temp);

            long lastModified = file.lastModified();
            Date dateLastModified = new Date(lastModified);
            SimpleDateFormat dateTemplate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            temp = dateTemplate.format(dateLastModified);
            Date lastModifiedDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").parse(temp);

            if (ifModifiedDate.compareTo(lastModifiedDate) > 0) {
                serverResponse.setStatusLine("HTTP\1.1 304 Not Modified");
                return;
            }
        }

        try {
			BufferedReader fileToGet = new BufferedReader(new FileReader(s + clientRequest.getPath()));
	        serverResponse.setStatusLine("HTTP\1.1 200 OK");
	        serverResponse.setBody(fileToGet.toString());
	        fileToGet.close();
		} catch (FileNotFoundException e) {
			serverResponse.setStatusLine("HTTP\1.1 404 Not Found");
		} 
    }

    private void methodPUT(HTTPRequest clientRequest, HTTPResponse serverResponse) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(s + clientRequest.getPath());
        writer.print(clientRequest.getMessageBody());
        writer.close();
        serverResponse.setStatusLine("HTTP\1.1 200 OK");
    }

    private void methodPOST(HTTPRequest clientRequest, HTTPResponse serverResponse) throws IOException {
        File file = new File(s + clientRequest.getPath());
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter writer = new FileWriter(file, true);
        writer.write(clientRequest.getMessageBody());
        writer.flush();
        writer.close();
        serverResponse.setStatusLine("HTTP\1.1 200 OK");
    }

}
