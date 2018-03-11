package http_message;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static java.lang.Integer.parseInt;


public class HTTPResponse extends HTTPMessage{

    private String version;
    private int responseCode;
    private String reasonPhrase;

    public void setStatusLine(String statusLine) {
        String args[] = statusLine.split(" ");
        version = args[0];
        responseCode = parseInt(args[1]);
        reasonPhrase = args[2];
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
        System.out.println();
        System.out.println(getMessageBody());
    }

    public void getEmbeddedImages() {

    }
}
