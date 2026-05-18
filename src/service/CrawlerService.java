package service;

import adapter.DatabaseStorage;
import adapter.PriceScraperAdapter;
import domain.Price;
import domain.Product;
import domain.ProductLink;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Core crawler service.
 * Iterates over all products, fetches the price from each registered store,
 * compares results and saves the lowest price to history.
 *
 * Uses a single scraper session (open/close) for all products,
 * avoiding opening and closing the browser on every call.
 */
public class CrawlerService {

    private final PriceScraperAdapter scraper;
    private final DatabaseStorage<Product> storage;

    public CrawlerService(PriceScraperAdapter scraper) {
        this.scraper = scraper;
        this.storage = new DatabaseStorage<>(Product.class);
    }

    /**
     * Alternative constructor for testing (allows injecting a mocked or real storage).
     */
    public CrawlerService(PriceScraperAdapter scraper, DatabaseStorage<Product> storage) {
        this.scraper = scraper;
        this.storage = storage;
    }

    /**
     * Runs the crawler for all products registered in the database.
     * Opens the browser once and closes it when done.
     */
    public void executar() {
        System.out.println("=== Starting Crawler ===");

        ArrayList<domain.EntityInterface> all = storage.listAll();

        if (all.isEmpty()) {
            System.out.println("[INFO] No products registered. Exiting.");
            return;
        }

        // Open the browser once for the entire session
        scraper.open();
        try {
            for (domain.EntityInterface entity : all) {
                if (entity instanceof Product product) {
                    processProduct(product);
                }
            }
        } finally {
            // Ensures the browser is closed even if an error occurs
            scraper.close();
        }

        System.out.println("=== Crawler finished ===");
    }

    /**
     * Processes a product: fetches the price from all stores and saves the lowest.
     * Can be called directly in tests by passing a product without a database.
     */
    public void processProduct(Product product) {
        List<ProductLink> links = product.getLinks();

        System.out.println("\nStarting crawler for: " + product.getName());

        if (links == null || links.isEmpty()) {
            System.out.println("[WARNING] Product '" + product.getName() + "' has no store links. Skipping.");
            return;
        }

        Float lowestPrice = null;
        String lowestStore = null;

        for (ProductLink link : links) {
            Float price = scraper.fetchPrice(link.getUrl(), link.getStoreName());

            if (price == null) {
                System.out.println("   [SKIPPED] " + link.getStoreName() + " returned no valid price.");
                continue;
            }

            System.out.println("-> Price found at " + link.getStoreName() + ": R$ " + price);

            if (lowestPrice == null || price < lowestPrice) {
                lowestPrice = price;
                lowestStore = link.getStoreName();
            }
        }

        if (lowestPrice == null) {
            System.out.println("[WARNING] No valid price found for: " + product.getName());
            return;
        }

        System.out.println("==> Best price updated: R$ " + lowestPrice + " (" + lowestStore + ")");

        // Creates the new price with store info and updates the product.
        // setPrice() only adds to history if the price actually changed.
        Price newPrice = new Price(lowestPrice, new Date(), lowestStore);
        product.setPrice(newPrice);

        // Persists the change to the database (only if storage is available)
        if (storage != null) {
            storage.update(product);
        }
    }
}
