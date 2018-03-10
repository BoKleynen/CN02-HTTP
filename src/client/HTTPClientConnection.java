package client;

import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;

import http_message.HTTPRequest;
import http_message.HTTPResponse;

import org.jsoup.Jsoup;

import static java.net.InetAddress.getByName;

public class HTTPClientConnection {

    private Socket socket;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;
    private String host;

    HTTPClientConnection(String host, int port) throws IOException {
        this.host = host;
        this.socket = new Socket(getByName(host), port);
        this.outToServer = new DataOutputStream(socket.getOutputStream());
        this.inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    }

    public HTTPResponse sendRequest(HTTPRequest request) throws IOException {
        outToServer.writeBytes(request.toString());

        HTTPResponse response = new HTTPResponse();
        String responseLine;

        if ((responseLine = inFromServer.readLine()).length() != 0) {
            response.setStatusLine(responseLine);
        }

        // headers
        while (((responseLine = inFromServer.readLine()).length() != 0)) {
            response.addHeader(responseLine);
        }

        StringBuilder responseBody = new StringBuilder();
        while (((responseLine = inFromServer.readLine()).length() != 0)) {
            responseBody.append(responseLine);
            responseBody.append("\r\n");
        }

        String contentType = response.getHeaders().get("content-type");
        if (contentType != null && contentType.startsWith("text/html")) {
            PrintWriter htmlWriter = new PrintWriter("../../out/output.html");
            htmlWriter.print(response.getMessageBody());
            htmlWriter.close();
        }
        response.setBody(responseBody.toString());

        return response;
    }

    public void close() throws IOException {
        socket.close();
    }

    private void writeToFile(String fileName, String data) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("out/" + fileName));
        writer.write(data);

        writer.close();
    }
}
