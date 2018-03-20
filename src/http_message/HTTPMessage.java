package http_message;

import java.util.HashMap;

import static java.lang.Integer.parseInt;

/**
 * This class models an HTTP message.
 */
public abstract class HTTPMessage {

    public static final String CRLF = "\r\n";

    private HashMap<String, String> headers = new HashMap<>();
    private String messageBody = "";
    final String version = "HTTP/1.1";


    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public void addHeader(String header) {
        String args[] = header.split(": ");
        headers.put(args[0], args[1]);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    /**
     * Sets the body of this HTTPMessage to the given string.
     * @param body
     */
    public void setBody(String body) {
        messageBody = body;
    }

    public void addToBody(String str) {
        messageBody += str;
    }

    /**
     * @return this.messageBody
     */
    public String getMessageBody() {
        return messageBody;
    }

    /**
     * @return A string representation of the headers of this HTTPMessage
     */
    public String getHeaderString() {
        StringBuilder headerString = new StringBuilder();
        for (HashMap.Entry<String, String> header : headers.entrySet()) {
            headerString.append(header.getKey()).append(": ").append(header.getValue()).append(CRLF);
        }

        return headerString.toString();
    }

    /**
     * @return  -1 if the Content-Length header is absent or invalid, otherwise
     *          return the length of the body of this response as contained within the header.
     */
    public int getContentLength() {
        try {
            return parseInt(getHeader("Content-Length"));
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }

    @Override
    public abstract String toString();
}
