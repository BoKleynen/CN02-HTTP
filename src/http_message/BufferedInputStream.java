package http_message;

import java.io.InputStream;
import java.io.IOException;

public class BufferedInputStream extends java.io.BufferedInputStream {

    /**
     * Constructs a buffered input stream from a given input stream.
     * @param in the given input stream to construct from
     */
    public BufferedInputStream(InputStream in) {
        super(in);
    }

    /**
     * Reads a single line (terminated by CRLF) from the input stream.
     * @return the line which was read
     * @throws IOException if the line could not be read
     */
    public String readLine() throws IOException {
        String output = "";
        do
            output += (char) this.read();
        while(!output.endsWith(HTTPMessage.CRLF));

        return output.substring(0, output.length() - 2);
    }
}