import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TestParseServers {
    public static void main(String[] args) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("episode_watch.html"));
        String html;
        if (bytes.length > 2 && bytes[0] == (byte)0xff && bytes[1] == (byte)0xfe) {
            html = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        } else if (bytes.length > 2 && bytes[1] == 0) {
            html = new String(bytes, StandardCharsets.UTF_16LE);
        } else {
            html = new String(bytes, StandardCharsets.UTF_8);
        }

        Document doc = Jsoup.parse(html);
        List<String[]> servers = new ArrayList<>();

        for (Element el : doc.select("[data-link]")) {
            String link = el.attr("data-link").trim();
            String name = el.attr("data-server-name").trim();
            if (name.isEmpty()) name = el.text().trim();
            if (link.contains("id=") && (link.contains("http") || link.contains("vid"))) {
                String b64 = link.substring(link.indexOf("id=") + 3);
                if (b64.contains("&")) b64 = b64.substring(0, b64.indexOf("&"));
                try {
                    byte[] dec = Base64.getDecoder().decode(b64);
                    String decoded = new String(dec, StandardCharsets.UTF_8).trim();
                    if (decoded.startsWith("http")) link = decoded;
                } catch (Exception e) {}
            }
            if (!link.isEmpty()) {
                servers.add(new String[]{name, link});
            }
        }

        System.out.println("Found " + servers.size() + " servers:");
        for (String[] s : servers) {
            System.out.println("  " + s[0] + " => " + s[1]);
        }
    }
}
