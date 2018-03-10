package http_message;

import java.util.HashMap;

import static java.lang.Integer.parseInt;

public class HTTPResponse extends HTTPMessage{

    private String version;
    private int responseCode;
    private String reasonPhrase;

    public void setStatusLine(String statusLine) {
        String args[] = statusLine.split(" ");
        version = args[0];
        responseCode = parseInt(args[1]);
        reasonPhrase = args[2];
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public String getVersion() {
        return version;
    }

    public int getResponseCode() {
        return responseCode;
    }


    @Override
    public String toString() {
        return null;
    }
}
