package http_message;

import util.CommandNotFoundException;
import java.net.URL;

/**
 * This class models a HTTP request.
 */
public class HTTPRequest extends HTTPMessage {
    private String method;
    private String path;

    public HTTPRequest(String method, URL url) throws CommandNotFoundException {
        setMethod(method);
        setPath(url.getPath());
        addHeader("Host", url.getHost());
    }

    @Override
    public String toString() {
        if (getMessageBody() == null) {
            return method + ' ' + path + ' ' + version + CRLF +
                    getHeaderString();
        }
        else {
            return method + ' ' + path + ' ' + version + CRLF +
                    getHeaderString() +
                    CRLF +
                    getMessageBody();
        }

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

    private void setPath(String path) {
        this.path = path.equals("") ? "/" : path;
    }
}
