import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TestFindSeries {
    public static void main(String[] args) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
        
        Request req = new Request.Builder()
            .url("https://m.myseed.tv/find/?find=%D8%A7%D9%84%D8%B2%D9%88%D8%A7%D8%AC+%D8%AC%D9%85%D9%8A%D9%84")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build();
        
        Response resp = client.newCall(req).execute();
        String html = resp.body().string();
        System.out.println("Search HTTP Status: " + resp.code() + " Length: " + html.length());
        Document doc = Jsoup.parse(html, "https://m.myseed.tv/");
        for (Element el : doc.select(".MovieBlock a, .SeriesBlock a, a.BlockItem, .container a[href*='myseed']")) {
            String href = el.absUrl("href");
            if (href.contains("myseed.tv/") && !href.contains("/category/") && !href.contains("/find/")) {
                System.out.println("Item: " + el.text() + " => " + href);
            }
        }
    }
}
