import exceptions.CrawlFailureException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class CrawlJob {
    private final URI uri;
    private final ConcurrentHashSet<String> visitedLinks;
    private final HttpClient httpClient;
    private final HttpRequest httpRequest;
    private final String host;

    public CrawlJob(String uri, ConcurrentHashSet<String> visitedLinks, HttpClient httpClient) throws URISyntaxException {
        String host1;
        this.visitedLinks = visitedLinks;
        this.httpClient = httpClient;

        this.uri = new URI(uri);
        host1 = this.uri.getHost();
        if (host1 != null && host1.startsWith("www.")) {
            host1 = host1.substring(4);
        }

        this.host = host1;
        this.httpRequest = HttpRequest.newBuilder()
                .uri(this.uri)
                .GET()
                .header("User-Agent", "J-Crawler")
                .build();
    }

    public CompletableFuture<CrawlJob[]> doJob() {

        CompletableFuture<String> htmlFuture = this.fetchHTML();
        CompletableFuture<Document> documentFuture = this.parseHTML(htmlFuture);
        CompletableFuture<ConcurrentHashSet<String>> linkFuture = this.extractLinks(documentFuture);
        CompletableFuture<ConcurrentHashSet<String>> unseenLinks = this.getUnseenLinks(linkFuture);
        return unseenLinks.thenApply(links -> {
            CrawlJob[] newCrawlJobs = new CrawlJob[links.size()];

            System.out.println("--- Found " + links.size() + " Links ---");

            int i = 0;

            for (String link : links) {
                try {
                    newCrawlJobs[i] = new CrawlJob(link, this.visitedLinks, this.httpClient);
                } catch (URISyntaxException e) {
                    System.out.println("Failed to create Crawl Job for " + link);
                }
                i++;
            }

            return newCrawlJobs;
        });
    }

    private CompletableFuture<String> fetchHTML() {
        return this.httpClient.sendAsync(this.httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int statusCode = response.statusCode();
                    String url = response.uri().toString();

                    if (statusCode >= 200 && statusCode < 300) {
                        return response.body();
                    } else {
                        String errorMessage = "Failed to crawl URL: " + url + "\nWith status code" + statusCode;
                        throw new CrawlFailureException(errorMessage, statusCode, url);
                    }
                });
    }

    private CompletableFuture<Document> parseHTML(CompletableFuture<String> htmlFuture) {
        return htmlFuture.thenApply(htmlBody -> Jsoup.parse(htmlBody, this.uri.toString()));
    }

    private CompletableFuture<ConcurrentHashSet<String>> extractLinks(CompletableFuture<Document> documentFuture) {
        return documentFuture.thenApply(parsedHTMLBody -> {
            ConcurrentHashSet<String> extractedLinks = new ConcurrentHashSet<>();
            Elements links = parsedHTMLBody.select("a[href]");

            for (Element element : links) {
                String absUrl = element.absUrl("href");

                if (absUrl.isEmpty() || absUrl.startsWith("#")) {
                    continue;
                }

                try {

                    URI linkURI = new URI(absUrl);

                    String linkHost = linkURI.getHost();

                    if (linkHost == null) {
                        System.out.println("Skipping non-http link: " + absUrl);
                        continue;
                    }

                    URI strippedUri = new URI(linkURI.getScheme(), linkURI.getAuthority(), linkURI.getPath(), null, null);

                    if (linkHost.startsWith("www.")) {
                        linkHost = linkHost.substring(4);
                    }
                    if (linkHost.equals(this.host) || linkHost.endsWith("." + this.host)) {
                        extractedLinks.add(strippedUri.toString());
                    }
                } catch (URISyntaxException e) {
                    System.out.println("Failed to convert " + element.absUrl("href") + " to a url.");
                }
            }
            return extractedLinks;
        });
    }

    private CompletableFuture<ConcurrentHashSet<String>> getUnseenLinks(CompletableFuture<ConcurrentHashSet<String>> linkFuture) {
        return linkFuture.thenApply(links -> {
            ConcurrentHashSet<String> newLinks = new ConcurrentHashSet<>();

            links.forEach(link -> {
                if (this.visitedLinks.add(link)) {
                    newLinks.add(link);
                }
            });

            return newLinks;
        });
    }
}
