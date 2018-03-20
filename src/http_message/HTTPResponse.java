package http_message;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.copyOfRange;


/**
 * A class modelling HTTP responses as defined in RFC2616.
 */
public class HTTPResponse extends HTTPMessage{

    private String version;
    private int responseCode;
    private String reasonPhrase;

    /**
     * Parses the given string as a HTTP response status line and sets corresponding
     * fields of this HTTPResponse to the parsed values.
     *
     * @param statusLine    String representation of a HTTP response's
     *                      status line (as defined in RFC2616).
     */
    public void setStatusLine(String statusLine) {
        String args[] = statusLine.split(" ");
        version = args[0];
        responseCode = parseInt(args[1]);
        StringBuilder stringBuilder = new StringBuilder();
        for (String str : copyOfRange(args, 2, args.length)) {
            stringBuilder.append(str);
            stringBuilder.append(" ");
        }
        reasonPhrase = stringBuilder.toString();
    }

    public String getStatusLine() {
        return version + " " + responseCode + " " + reasonPhrase;
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

    /**
     * @return  Returns a list of links to objects embedded in the
     *          the body of this response.
     */
    public ArrayList<URI> getImageLinks() {
        ArrayList<URI> linkList = new ArrayList<>();
        Document document = Jsoup.parse(getMessageBody());
        Elements images = document.getElementsByTag("img");
        for (Element image : images) {
            String link = image.attr("src");
            try {
                linkList.add(new URI(link));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return linkList;
    }

    /**
     * Prints a formatted representation (as defined in RFC2616) to the
     * standard system output.
     */
    public void print() {
        System.out.println(this);
    }

    public String toString() {
        String s = version + responseCode + reasonPhrase + CRLF +
                getHeaderString() + CRLF;
        if (hasBody())  {
            return s + getMessageBody() + CRLF;
        }
        else {
            return s;
        }
    }
}
