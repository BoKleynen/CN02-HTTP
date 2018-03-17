package client;

import http_message.HTTPRequest;
import http_message.HTTPResponse;

import java.net.URI;

public class TestClient {
    public static void main(String[] args) throws Exception {
        URI uri = new URI("http://www.tcpipguide.com");

        HTTPClientConnection client = new HTTPClientConnection(uri.getHost(), 80);
        HTTPRequest request = new HTTPRequest("GET", uri);
        System.out.println(request);
        HTTPResponse response = client.sendRequest(request);
        response.print();
    }
}
