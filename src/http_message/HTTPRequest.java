package http_message;

import util.CommandNotFoundException;

import java.net.URI;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;

/**
 * A class modelling an HTTP request as defined in RFC2616
 */
public class HTTPRequest extends HTTPMessage {
    private String method;
    private String path;
    private String extension;

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
        setPath(args[1]);
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
        setPath(uri.getPath());
        addHeader("host", uri.getHost());
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
                    getMessageBody() +
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
        return path;
    }

    /**
     * Sets the relative path to the endpoint.
     * @param path
     *          Relative path to the requested resource. If an
     *          empty string is provided, the root is assumed.
     */
    private void setPath(String path) {
        this.path = path.equals("") ? "/" : path;
        if (this.getPath().equals("/")) {
        	this.setPath("/");
        }
        else {
        	String fileExtension = FilenameUtils.getExtension(this.getPath());
        	this.setExtenstion(fileExtension);
        }
    }
    
    private void setExtenstion(String extension) {
    	this.extension = extension.equals("") ? "/" : path;
    }
    
    public String getExtension() {
    	return this.extension;
    	
    }
}
