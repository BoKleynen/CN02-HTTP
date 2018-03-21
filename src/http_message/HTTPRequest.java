package http_message;

import util.CommandNotFoundException;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;

/**
 * A class modelling an HTTP request as defined in RFC2616
 */
public class HTTPRequest extends HTTPMessage {
    private String method;
    private final String path;

    /**
     *
     * @param initialLine
     * @throws CommandNotFoundException
     *          If the given command is not a valid HTTP command
     *          or it is not supported. At the moment only GET, HEAD
     *          PUT and POST are supported.
     * @throws ArrayIndexOutOfBoundsException
     *          If the given string does not meet the expected formatting.
     */

    public HTTPRequest(String initialLine) throws CommandNotFoundException, ArrayIndexOutOfBoundsException {
        String args[] = initialLine.split(" ");
        setMethod(args[0]);
        path = "".equals(args[1]) ? "/" : args[1];
    }

    /**
     *
     * @param method
     *          Supported methods are GET, HEAD, PUT and POST
     * @param uri
     *          The endpoint URI for this request.
     * @throws CommandNotFoundException
     *          If the given command is not a valid HTTP command
     *          or it is not supported.
     */
    public HTTPRequest(String method, URI uri) throws CommandNotFoundException {
        setMethod(method);
        path = "".equals(uri.getPath()) ? "/" : uri.getPath();
        addHeader("Host", uri.getHost());
    }

    /**
     * @return A formatted string representation of this HTTP request.
     */
    @Override
    public String toString() {
        String s = method + ' ' + path + ' ' + version + CRLF +
                getHeaderString()
                + CRLF;
        if (hasBody()) {
            return s +
                    getBody() +
                    CRLF;
        }
        else {
            return s;
        }
    }

    /**
     * Sets the method for this HTTP request
     * @param method
     *          Supported methods are GET, HEAD, PUT and POST
     * @throws CommandNotFoundException
     *          If the given method is not a valid HTTP method or
     *          it is not supported.
     */
    private void setMethod(String method) throws CommandNotFoundException {
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

    /**
     * @return  The method of this HTTP request.
     */
    public String getMethod() {
        return this.method;
    }

    /**
     * @return  The relative path to the requested resource.
     */
    public String getPath() {
        return path.substring(1);
    }
}
