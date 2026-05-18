package adapter;

/**
 * Interface defining the contract for fetching product prices from stores.
 * Allows CrawlerService to be tested with mocks without hitting the real internet.
 *
 * Extends AutoCloseable so the scraper can be used in try-with-resources,
 * ensuring resources (e.g. Playwright/Browser) are properly released.
 */
public interface PriceScraperAdapter extends AutoCloseable {

    /**
     * Initializes scraper resources (e.g. opens the browser).
     * Must be called before fetchPrice.
     */
    void open();

    /**
     * Fetches the current price of a product at the given URL.
     *
     * @param url       Product page URL at the store
     * @param storeName Store name (used for logging)
     * @return The price found, or null if it could not be extracted
     */
    Float fetchPrice(String url, String storeName);

    /**
     * Releases scraper resources.
     */
    @Override
    void close();
}
