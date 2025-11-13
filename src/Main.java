import exceptions.CrawlFailureException;

import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.concurrent.CompletionException;

public class Main {
    public static void main(String[] args) throws URISyntaxException {
        ConcurrentHashSet<String> visitedLinks = new ConcurrentHashSet<>();
        HttpClient httpClient = HttpClient.newBuilder().build();
        CrawlJob crawlJob = new CrawlJob("https://hypixel.net/", visitedLinks, httpClient);
        try {
            crawlJob.doJob().join();

            System.out.println("Crawl job finished successfully.");

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CrawlFailureException) {
                System.err.println("CRAWL FAILED: " + cause.getMessage());
            } else {
                System.err.println("An unexpected error occurred: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
