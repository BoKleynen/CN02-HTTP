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


public class HTTPResponse extends HTTPMessage{

    private String version;
    private int responseCode;
    private String reasonPhrase;

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

    public void print() {
        System.out.println(getStatusLine());
        System.out.println(getHeaderString());

        if (getMessageBody() != null) {
            System.out.println();
            System.out.println(getMessageBody());
        }
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

    public String toString() {
        String s = version + responseCode + reasonPhrase + CRLF +
                getHeaderString() + CRLF;
        if (getMessageBody() == null)  {
            return s;
        }
        else {
            return s + getMessageBody() + CRLF;
        }
    }
}
