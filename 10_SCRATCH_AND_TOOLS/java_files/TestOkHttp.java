import okhttp3.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

public class TestOkHttp {
    public static void main(String[] args) {
        String watchUrl = "https://m.myseed.tv/%d9%81%d9%8a%d9%84%d9%85-%d8%b9%d8%b3%d9%84-%d8%a3%d8%b3%d9%88%d8%af-2010-%d8%a7%d9%88%d9%86%d9%84%d8%a7%d9%8a%d9%86/watch/";
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

            // 1. Fetch fresh watch page
            Request req0 = new Request.Builder()
                .url(watchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build();

            Response resp0 = client.newCall(req0).execute();
            String html0 = resp0.body() != null ? resp0.body().string() : "";
            Matcher m0 = Pattern.compile("vids\\.php\\?t=([a-f0-9]+)").matcher(html0);
            if (!m0.find()) {
                System.out.println("No token in watch page!");
                return;
            }
            String token = m0.group(1);
            System.out.println("Got fresh token: " + token);

            String vidsUrl = "https://m.myseed.tv/vids.php?t=" + token;

            // 2. Fetch vids.php
            Request req1 = new Request.Builder()
                .url(vidsUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Referer", watchUrl)
                .build();

            Response resp1 = client.newCall(req1).execute();
            System.out.println("vids.php resp: " + resp1.code());
            String body1 = resp1.body() != null ? resp1.body().string() : "";

            Matcher m1 = Pattern.compile("src=[\"']([^\"']+player-gateway[^\"']+)[\"']").matcher(body1);
            if (m1.find()) {
                String gwUrl = m1.group(1).replace("&amp;", "&");
                System.out.println("Gateway URL: " + gwUrl);

                Request req2 = new Request.Builder()
                    .url(gwUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Referer", vidsUrl)
                    .build();

                Response resp2 = client.newCall(req2).execute();
                System.out.println("Gateway resp: " + resp2.code());
                String body2 = resp2.body() != null ? resp2.body().string() : "";

                Matcher vm = Pattern.compile("<source\\s+[^>]*src=[\"']([^\"']+\\.(?:mp4|m3u8)(?:\\?[^\"']*)?)[\"']", Pattern.CASE_INSENSITIVE).matcher(body2);
                if (vm.find()) {
                    System.out.println("FOUND DIRECT STREAM: " + vm.group(1));
                } else {
                    System.out.println("NO STREAM FOUND in body! Body snippet:\n" + body2.substring(0, Math.min(500, body2.length())));
                }
            } else {
                System.out.println("No player-gateway iframe found in vids.php!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
