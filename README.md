# Product Price Monitor

A Java application that tracks product prices across multiple online stores, saves price history, and identifies where a product is cheapest.

---

## Technologies

- **Java 21+** (with `--enable-preview` for unnamed main class)
- **Hibernate 6 + JPA** — persistence with SQLite database
- **Playwright (headless Chromium)** — real browser scraping, no visible window
- **JUnit 5 + Mockito** — automated unit tests

---

## Prerequisites

- Java 21 or later installed and on `PATH`
- Maven available on `PATH` (or use the IntelliJ bundled Maven)

---

## How to run

Single command — compiles and runs (using the Maven Wrapper if Maven is not installed on your system):

```powershell
.\mvnw.cmd clean compile -q; .\run_app.ps1
```

---

## How to run tests

```powershell
.\mvnw.cmd test
```

Expected result:

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The 6 test scenarios are:
1. Identifies the lowest price between Amazon and Kabum
2. Works when Amazon is cheaper
3. Accumulates history after multiple crawler runs
4. Does not fail when a product has no links
5. Ignores a broken link and uses the valid store
6. Does not change the price if all stores return an error

---

## How to use the Crawler

### Step 1 — Register a product with store links (do this once)

In `src/Main.java`, uncomment the **STEP 1** block:

```java
ProductLink linkAmazon = new ProductLink("Amazon", "https://www.amazon.com.br/dp/...");
ProductLink linkKabum  = new ProductLink("Kabum",  "https://www.kabum.com.br/produto/...");

Product ps5slim = new Product(
    "SKU-PS5-SLIM",
    "PlayStation 5 Slim",
    new Price(4000f, new Date()),
    new ArrayList<>(List.of(linkAmazon, linkKabum))
);
productService.save(ps5slim);
```

Run the app. After saving, **comment the block out again** to avoid duplicating the product on the next run.

### Step 2 — Run the Crawler

With the product already registered, **STEP 3** in `Main.java` is always active:

```java
CrawlerService crawler = new CrawlerService(new PlaywrightApiScraper());
crawler.executar();
```

The crawler will:
1. Open a headless browser (Chromium) — no window appears
2. Navigate to each store URL and extract the current price
3. Compare all prices found
4. Save the **lowest price** to history, along with the store name

### Expected console output

```
=== Registered products ===
Product { sku='SKU-PS5-SLIM', name='PlayStation 5 Slim', ... }

=== Starting Crawler ===

Starting crawler for: PlayStation 5 Slim
-> Navigating to Amazon: https://www.amazon.com.br/...
Amazon price: R$ 3900.8
-> Price found at Amazon: R$ 3900.8
-> Navigating to Kabum: https://www.kabum.com.br/...
Kabum price: R$ 4179.05
-> Price found at Kabum: R$ 4179.05
==> Best price updated: R$ 3900.8 (Amazon)

=== Crawler finished ===

=== Products after Crawler run ===
Product { sku='SKU-PS5-SLIM', name='PlayStation 5 Slim', price=3900.8 @ ... (Amazon), ... }
```

---

## Project structure

```
src/
├── Main.java                        # Entry point
├── domain/
│   ├── Product.java                 # Product entity
│   ├── Price.java                   # Price entity (with store name)
│   ├── ProductLink.java             # Store link entity
│   └── EntityInterface.java
├── service/
│   ├── CrawlerService.java          # Core crawler logic
│   ├── ProductService.java
│   ├── PriceService.java
│   └── BaseService.java
├── adapter/
│   ├── PriceScraperAdapter.java     # Scraper interface (testable with mocks)
│   ├── PlaywrightApiScraper.java    # Playwright headless Chromium implementation
│   └── DatabaseStorage.java
└── test/
    └── CrawlerServiceTest.java      # 6 unit tests with Mockito
```