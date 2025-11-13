package exceptions;

public class CrawlFailureException extends RuntimeException {
    private final int statusCode;
    private final String url;

    public CrawlFailureException(String message, int statusCode, String url) {
        super(message);
        this.statusCode = statusCode;
        this.url = url;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getUrl() {
        return this.url;
    }
}
