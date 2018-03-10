package client;

import http_message.HTTPRequest;
import http_message.HTTPResponse;

import java.net.URI;
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

            if (args.length != 3) {
                System.err.println("incorrect HTTP command formatting");
            }

            try {
                URI uri = new URI(args[1]);

                HTTPRequest request = new HTTPRequest(args[0], uri.getPath());

                // get headers from user input
                while((inputLine = inputScanner.nextLine()).length() != 0) {
                    request.addHeader(inputLine);
                }

                // get body from user input if the specified command is POST or PUT
                if (request.getMethod().equals("POST") || request.getMethod().equals("PUT")) {
                    while((inputLine = inputScanner.nextLine()).length() != 0) {
                        request.addToBody(inputLine);
                        request.addToBody("\r\n");
                    }
                }

                int port;
                try {
                    port = parseInt(args[2]);
                } catch (NumberFormatException e) {
                    port = 80;
                }

                HTTPClientConnection client = new HTTPClientConnection(uri.getHost(), port);
                HTTPResponse reponse = client.sendRequest(request);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
