package client;

import http_message.HTTPRequest;
import http_message.HTTPResponse;
import java.io.File;
import java.net.URI;
import java.util.Base64;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;

@SuppressWarnings("all")
public class TestClient {
    public static void main(String[] args) throws Exception {
        URI uri = new URI("//www.tcpipguide.com");

        HTTPClientConnection client = new HTTPClientConnection(uri.getHost(), 80);
        HTTPRequest request = new HTTPRequest("GET", uri);
        System.out.println(request);
        HTTPResponse response = client.sendRequest(request);
        response.print();
        File file = new File("websites/" + uri.getAuthority() + request.getPath() + "a.html");
        writeStringToFile(file, response.getBody());

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

        client.close();
    }
}
