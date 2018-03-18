package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import http_message.HTTPRequest;
import http_message.HTTPResponse;
import util.CommandNotFoundException;

public class HTTPHandler_1 {
	public static void httpHandler(BufferedReader clientInput) throws IOException, CommandNotFoundException, ParseException {
		DataOutputStream outputStream;
		HTTPRequest clientRequest = DefineRequest(clientInput);
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
		
		// no host header
		if (clientRequest.getHeader("Host") == null) {
			serverResponse.setStatusLine("HTTP\1.1 400 Bad Request");
			serverResponse.setBody("");
			}
		
		// METHODS
		if (clientRequest.getMethod() == "HEAD") {
			serverResponse.setStatusLine("HTTP\1.1 200 OK");
			serverResponse.setBody("");
		}
		else if(clientRequest.getMethod() == "GET") {
			methodGET(clientRequest, serverResponse);	
		}
		else if(clientRequest.getMethod() == "PUT") {
			methodPUT(clientRequest, serverResponse);	
		}
		else if(clientRequest.getMethod() == "POST") {
			methodPOST(clientRequest, serverResponse);			
		}
		
		
		
		// CONTENT-LENGTH TODO
		
		
		
	}
	
	
	private static HTTPRequest DefineRequest(BufferedReader clientInput) throws IOException, CommandNotFoundException {
		HTTPRequest request = new HTTPRequest(clientInput.readLine());
		String inputHeader;
		while((inputHeader = clientInput.readLine()).length() != 0 || clientInput.readLine() != null) {
			request.addHeader(inputHeader);			
		}
		String inputBody;
		while((inputBody = clientInput.readLine()) != null) {
			request.addToBody(inputBody);
		}
		return request;
	}
	
	private static void methodGET(HTTPRequest clientRequest, HTTPResponse serverResponse) throws ParseException {
		//If-Modified-since
		Date ifModifiedDate;
		String temp;
		if ((temp = clientRequest.getHeader("If-Modified-Since")) != null) {
			ifModifiedDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").parse(temp);
			
			File file = new File(clientRequest.getPath());
			long lastModified = file.lastModified();
			Date dateLastModified = new Date(lastModified);
			SimpleDateFormat dateTemplate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
			temp = dateTemplate.format(dateLastModified);
			Date lastModifiedDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").parse(temp);
			
			if (ifModifiedDate.compareTo(lastModifiedDate) > 0) {
				serverResponse.setStatusLine("HTTP\1.1 304 Not Modified");
				serverResponse.setBody("");
				return;
			}
		}
		
		// Files binnenhalen TODO
		
	}
	
	private static void methodPUT(HTTPRequest clientRequest, HTTPResponse serverResponse) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(clientRequest.getPath());
		writer.print(clientRequest.getMessageBody());
		writer.close();
		serverResponse.setStatusLine("HTTP\1.1 200 OK");
		serverResponse.setBody("");	
	}
	
	private static void methodPOST(HTTPRequest clientRequest, HTTPResponse serverResponse) throws IOException {
		File file = new File(clientRequest.getPath());
		if (!file.exists()) {
			file.createNewFile();
		}
		FileWriter writer = new FileWriter(file, true);
		writer.write(clientRequest.getMessageBody());
		writer.flush();
		writer.close();
		serverResponse.setStatusLine("HTTP\1.1 200 OK");
		serverResponse.setBody("");
	}


}

// TODO Error 404,500
