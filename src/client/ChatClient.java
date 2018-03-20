package client;

import http_message.HTTPRequest;
import http_message.HTTPResponse;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;

import static java.lang.Integer.parseInt;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.io.FileUtils.writeStringToFile;

@SuppressWarnings("all")
public class ChatClient {
    public static void main(String[] args) throws Exception{
        boolean keepConnection = true;
        Scanner inputScanner = new Scanner(System.in);
        HTTPClientConnection client = null;

        while (keepConnection) {
            System.out.println("Enter command. E for exit");
            String inputLine;

            if (inputScanner.hasNext()) {
                inputLine = inputScanner.nextLine();
                if (inputLine.equals("E")) {
                    if (client != null)
                        client.close();
                    break;
                }

                else
                    args = inputLine.split(" ");
            }

            if (args.length < 3) {
                System.err.println("incorrect HTTP command formatting");
            }

            int port;
            try {
                port = parseInt(args[2]);
            } catch (NumberFormatException e) {
                port = 80;
                System.out.println("INFO: invalid port, reverting to default HTTP port (port 80)!");
            }

            URI uri = new URI(args[1]);
            HTTPRequest request = new HTTPRequest(args[0], uri);

            // get headers from user input
            System.out.println("Enter headers followed by a blank line.");
            while((inputLine = inputScanner.nextLine()).length() != 0) {
                request.addHeader(inputLine);
            }

            // get body from user input if the specified command is POST or PUT
            if (request.getMethod().equals("POST") || request.getMethod().equals("PUT")) {
                StringBuilder body = new StringBuilder();
                System.out.println("Enter body followed by a blank line.");
                while((inputLine = inputScanner.nextLine()).length() != 0) {
                    body.append(inputLine);
                    body.append("\r\n");
                }
                request.setBody(body.toString());
            }

            client = new HTTPClientConnection(uri.getHost(), port);
            HTTPResponse response = client.sendRequest(request);
            System.out.println(request);
            System.out.println();
            response.print();
            File file = new File("websites/" + uri.getAuthority() + request.getPath() + "a.html");
            writeStringToFile(file, response.getBody());

            if ("close".equals(response.getHeader("Connection")))
                keepConnection = false;

            HTTPRequest imgRequest;
            HTTPResponse imgResponse;
            for (URI imgUri : response.getImageLinks()) {
                try {
                    imgRequest = new HTTPRequest("GET", imgUri);
                    System.out.println(imgRequest);
                    imgResponse = client.sendRequest(imgRequest);
                    imgResponse.print();
                    if (imgResponse.success()) {
                        file = new File("websites/" + uri.getAuthority() + imgRequest.getPath());
                        writeByteArrayToFile(file, Base64.getDecoder().decode(imgResponse.getBody()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
