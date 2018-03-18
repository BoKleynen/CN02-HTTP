package client;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import http_message.HTTPRequest;
import http_message.HTTPResponse;
import http_message.BufferedInputStream;
import static java.net.InetAddress.getByName;
import static java.lang.Integer.parseInt;


@SuppressWarnings("ResultOfMethodCallIgnored")
public class HTTPClientConnection {

    private Socket socket;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;

    HTTPClientConnection(String host, int port) throws IOException {
        this.socket = new Socket(getByName(host), port);
        this.outToServer = new DataOutputStream(socket.getOutputStream());
//        this.inFromServer = new BufferedInputStream(socket.getInputStream());
        this.inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    }

    public HTTPResponse sendRequest(HTTPRequest request) throws IOException {
        outToServer.writeBytes(request.toString());

        HTTPResponse response = new HTTPResponse();
        String responseLine;

        // status line
        for(;;) {
            if ((responseLine = inFromServer.readLine()).length() != 0) {
                response.setStatusLine(responseLine);
                break;
            }
        }

        System.out.println(response.getStatusLine());

        // headers
        while ((responseLine = inFromServer.readLine()).length() != 0) {
            response.addHeader(responseLine);
        }

        // body
        String transferEncoding = response.getHeader("Transfer-Encoding");
        if (transferEncoding != null && transferEncoding.equals("chunked")) {
            int chunkSize;
            String contentType = response.getHeader("Content-Type");
            if (contentType != null && contentType.contains("image")) {
                //TODO
            }

            else {
                StringBuilder body = new StringBuilder();
                while ((chunkSize = parseInt(inFromServer.readLine())) != 0) {
                    char[] chunk = new char[chunkSize];
                    inFromServer.read(chunk, 0, chunkSize);
                    body.append(new String(chunk));
                    inFromServer.skip(2);
                }
            }
        }

        else {
            String contentType = response.getHeader("Content-Type");
            if (contentType != null && contentType.contains("image")) {
                //TODO
            }

            else {
                int contentLength = response.getContentLength();
                if (contentLength != -1) {
                    char[] body = new char[contentLength];
                    inFromServer.read(body, 0, contentLength);
                    response.setBody(new String(body));
                }

                else {
                    StringBuilder body = new StringBuilder();
                    while ((responseLine = inFromServer.readLine()).length() != 0) {
                        body.append(responseLine);
                    }
                    response.setBody(body.toString());
                }
            }
        }

        return response;
    }

    public void getEmbeddedImages(HTTPResponse response) {
        ArrayList<URI> links = response.getImageLinks();
    }

    public void close() throws IOException {
        socket.close();
    }
}
