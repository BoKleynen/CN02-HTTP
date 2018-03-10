package http_message;

import util.CommandNotFoundException;

/**
 * This class models a HTTP request.
 */
public class HTTPRequest extends HTTPMessage {
    private String method;
    private String path;

    public HTTPRequest(String method, String path) throws CommandNotFoundException {
        setMethod(method);
    }

    /**
     * Builds a string representation of this HTTP request, which can be send to a HTTP server.
     * @return
     */
    @Override
    public String toString() {
        return method + ' ' + path + ' ' + version + "\r\n" +
                getHeaderString() + "\r\n\r\n" +
                ((getMessageBody() == null) ? "" : getMessageBody());
    }

    public void setMethod(String method) throws CommandNotFoundException {
        method = method.toUpperCase();

        switch (method) {
            case "GET": {
                this.method = "GET";
                break;
            }
            case "HEAD": {
                this.method = "HEAD";
                break;
            }
            case "POST": {
                this.method = "POST";
                break;
            }
            case "PUT": {
                this.method = "PUT";
                break;
            }
            default:
                throw new CommandNotFoundException();

        }
    }

    public String getMethod() {
        return this.method;
    }
}
