package client;

import java.io.*;
import java.net.Socket;
import java.util.Base64;
import http_message.BufferedInputStream;
import http_message.HTTPRequest;
import http_message.HTTPResponse;

import static java.net.InetAddress.getByName;
import static java.lang.Integer.parseInt;
import static http_message.HTTPMessage.CRLF;


/**
 * This class models and HTTP connection between the use of this
 * class and a remote host.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class HTTPClientConnection {

    private Socket socket;
    private DataOutputStream outToServer;
    private BufferedInputStream inFromServer;

    HTTPClientConnection(String host, int port) throws IOException {
        this.socket = new Socket(getByName(host), port);
        this.outToServer = new DataOutputStream(socket.getOutputStream());
        this.inFromServer = new BufferedInputStream(socket.getInputStream());

    }

    /**
     * Sends the given HTTP request to the foreign host.
     *
     * @param request
     *          The request to be sent.
     * @return
     * @throws IOException
     */
    public HTTPResponse sendRequest(HTTPRequest request) throws IOException {
        outToServer.writeBytes(request.toString());

        HTTPResponse response = new HTTPResponse(request.getHeader("Host"));
        String responseLine;

        // status line
        for(;;) {
            if ((responseLine = inFromServer.readLine()) != null) {
                response.setStatusLine(responseLine);
                break;
            }
        }

        // headers
        while ((responseLine = inFromServer.readLine()).length() != 0) {
            response.addHeader(responseLine);
        }

        if (!request.getMethod().equals("HEAD")) {
            // body
            String transferEncoding = response.getHeader("Transfer-Encoding");
            if ("chunked".equals(transferEncoding)) {
                int chunkSize;
                String contentType = response.getHeader("Content-Type");
                if (contentType != null && contentType.contains("image")) {
                    response.setBody(readChunkedImageBody());
                }

                else {
                    response.setBody(readChunkedBody());
                }
            }

            else {
                String contentType = response.getHeader("Content-Type");
                if (contentType != null && contentType.contains("image")) {
                    int contentLength = response.getContentLength();
                    if (contentLength != -1) {
                        String imgString = readImageBody(contentLength);
                        response.setBody(imgString);

                    }
                }

                else {
                    int contentLength = response.getContentLength();
                    if (contentLength != -1) {
                        response.setBody(readTextBody(contentLength));
                    }

                    else {
                        response.setBody(readTextBody());
                    }
                }
            }
        }

        if ("close".equals(response.getHeader("Connection")))
            socket.close();

        return response;
    }

    /**
     *
     * @throws IOException
     */
    public void close() throws IOException {
        socket.close();
    }

    private String readTextBody() throws IOException {
        StringBuilder body = new StringBuilder();
        String responseLine;
        while ((responseLine = inFromServer.readLine()) != null) {
            body.append(responseLine);
            body.append(CRLF);
        }

        return body.toString();
    }

    private String readTextBody(int contentLength) throws IOException {
        char[] body = new char[contentLength];
        for (int i=0; i<contentLength; i++) {
            body[i] = (char) inFromServer.read();
        }
        return new String(body);
    }

    private String readImageBody(int contentLength) throws IOException {
        byte[] body = new byte[contentLength];
        for (int i=0; i<contentLength; i++) {
            body[i] = (byte) inFromServer.read();
        }

        return Base64.getEncoder().encodeToString(body);

    }

    private String readChunkedImageBody() throws IOException {
        int chunkSize;
        StringBuilder body = new StringBuilder();
        while ((chunkSize = parseInt(inFromServer.readLine())) != 0) {
            byte[] chunk = new byte[chunkSize];
            body.append(Base64.getEncoder().encodeToString(chunk));
        }

        return body.toString();
    }

    private String readChunkedBody() throws IOException {
        int chunkSize;
        StringBuilder body = new StringBuilder();
        while ((chunkSize = parseInt(inFromServer.readLine())) != 0) {
            byte[] chunk = new byte[chunkSize];
            inFromServer.read(chunk, 0, chunkSize);
            body.append(new String(chunk));
            inFromServer.skip(2);
        }

        return body.toString();
    }
}
