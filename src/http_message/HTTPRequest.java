package http_message;

import util.CommandNotFoundException;

import java.net.URI;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;

/**
 * This class models a HTTP request.
 */
public class HTTPRequest extends HTTPMessage {
    private String method;
    private String path;
    private String extension;

    public HTTPRequest(String initialLine) throws CommandNotFoundException {
        String args[] = initialLine.split(" ");
        setMethod(args[0]);
        setPath(args[1]);
    }

    public HTTPRequest(String method, URI uri) throws CommandNotFoundException {
        setMethod(method);
        setPath(uri.getPath());
        addHeader("host", uri.getHost());
    }

    @Override
    public String toString() {
        if (getMessageBody().equals("")) {
            return method + ' ' + path + ' ' + version + CRLF +
                    getHeaderString()
                    + CRLF;
        }
        else {
            return method + ' ' + path + ' ' + version + CRLF +
                    getHeaderString() +
                    CRLF +
                    getMessageBody()
                    + CRLF;
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
        if (this.getPath().equals("/")) {
        	this.setPath("/");
        }
        else {
        	String fileExtension = FilenameUtils.getExtension(this.getPath());
        	this.setExtenstion(fileExtension);
        }
    }
    
    public String getPath() {
    	return this.path;
    }
    
    private void setExtenstion(String extension) {
    	this.extension = extension.equals("") ? "/" : path;
    }
    
    public String getExtension() {
    	return this.extension;
    	
    }
}
