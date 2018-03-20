package client;

import http_message.HTTPRequest;
import http_message.HTTPResponse;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import static java.lang.Integer.parseInt;

public class ChatClient {
    public static void main(String[] args) {
        boolean keepConnection = true;
        Scanner inputScanner = new Scanner(System.in);

        while (keepConnection) {
            System.out.println("Command: ");
            String inputLine;

            if (inputScanner.hasNext()) {
                inputLine = inputScanner.nextLine();
                args = inputLine.split(" ");
            }

            if (args.length < 2) {
                System.err.println("incorrect HTTP command formatting");
            }

            try {
                URI uri = new URI(args[1]);


                HTTPRequest request = new HTTPRequest(args[0], uri);

                // get headers from user input
                while((inputLine = inputScanner.nextLine()).length() != 0) {
                    request.addHeader(inputLine);
                }

                // get body from user input if the specified command is POST or PUT
                if (request.getMethod().equals("POST") || request.getMethod().equals("PUT")) {
                    StringBuilder body = new StringBuilder();
                    while((inputLine = inputScanner.nextLine()).length() != 0) {
                        body.append(inputLine);
                        body.append("\r\n");
                    }
                    request.setBody(body.toString());
                }

                int port;
                try {
                    port = parseInt(args[2]);
                } catch (NumberFormatException e) {
                    port = 80;
                }

                HTTPClientConnection client = new HTTPClientConnection(uri.getHost(), port);
                HTTPResponse response = client.sendRequest(request);
                System.out.println(request);
                System.out.println();
                response.print();

                if ("close".equals(response.getHeader("Connection")))
                    keepConnection = false;

                for (URI imageLink : response.getImageLinks()) {
                    HTTPRequest imRequest = new HTTPRequest("GET", imageLink);
                    client.sendRequest(imRequest);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
