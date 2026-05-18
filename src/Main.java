import adapter.DatabaseStorage;
import adapter.PlaywrightApiScraper;
import domain.Price;
import domain.Product;
import domain.ProductLink;
import service.CrawlerService;
import service.ProductService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

void main() {
    ProductService productService = new ProductService();

    // One-time cleanup: removes zero-value history entries (registration placeholder)
    new DatabaseStorage<>(Product.class).clearZeroValueHistory();

    // --- STEP 1: Register products with store links (run once, then comment out) ---
    // ProductLink linkAmazon = new ProductLink("Amazon", "https://www.amazon.com.br/dp/B0FPGF9J2J");
    // ProductLink linkKabum  = new ProductLink("Kabum",  "https://www.kabum.com.br/produto/989702");
    //
    // Product ps5slim = new Product(
    //     "SKU-PS5-SLIM",
    //     "PlayStation 5 Slim",
    //     new Price(4000f, new Date()),
    //     new ArrayList<>(List.of(linkAmazon, linkKabum))
    // );
    // productService.save(ps5slim);
    // System.out.println("Product registered: " + ps5slim);
    //
    // ProductLink linkAmazonTab = new ProductLink("Amazon", "https://www.amazon.com.br/dp/B0F3LTWYS5");
    // ProductLink linkKabumTab  = new ProductLink("Kabum",  "https://www.kabum.com.br/produto/755274");
    //
    // Product tabS10fe = new Product(
    //     "SKU-TAB-S10-FE",
    //     "Samsung Galaxy Tab S10 FE",
    //     new Price(0f, new Date()),
    //     new ArrayList<>(List.of(linkAmazonTab, linkKabumTab))
    // );
    // productService.save(tabS10fe);
    // System.out.println("Product registered: " + tabS10fe);

    // --- STEP 2: List all registered products ---
    System.out.println("=== Registered products ===");
    productService.listAll();

    // --- STEP 3: Run the Crawler (fetches prices from all stores) ---
    // Uses PlaywrightApiScraper: headless Chromium, no visible window
    CrawlerService crawler = new CrawlerService(new PlaywrightApiScraper());
    crawler.executar();

    // --- STEP 4: Display products after crawler run ---
    System.out.println("\n=== Products after Crawler run ===");
    productService.listAll();
}
