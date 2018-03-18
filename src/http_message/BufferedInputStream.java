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
        StringBuilder s = new StringBuilder();
        char c = 0;

        if (!skipLF || (c = (char) read()) != '\n')
            s.append(c);
        skipLF = false;

        s.append(c);
        for(;;) {
            c = (char) read();
            if (c == '\r') {
                skipLF = true;
                break;
            }
            else if (c == '\n') {
                break;
            }
            s.append(c);
        }

        return s.toString();
    }

    private boolean skipLF = false;
}