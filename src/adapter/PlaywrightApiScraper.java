package adapter;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of PriceScraperAdapter using Playwright (headless browser mode).
 * Shares a single Playwright + Browser instance per crawling session.
 * Call open() before use and close() when done.
 */
public class PlaywrightApiScraper implements PriceScraperAdapter {

    // Regex pattern to capture monetary values in Brazilian format (e.g. R$ 4.179,05)
    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "R\\$\\s*([\\d.]+,[\\d]{2})"
    );

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;

    @Override
    public void open() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of(
                        "--disable-blink-features=AutomationControlled",
                        "--no-sandbox",
                        "--disable-setuid-sandbox",
                        "--window-size=1920,1080"
                )));
        context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .setViewportSize(1920, 1080)
                .setLocale("pt-BR")
                .setTimezoneId("America/Sao_Paulo")
                .setExtraHTTPHeaders(Map.of(
                        "Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7",
                        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                        "sec-ch-ua", "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"",
                        "sec-ch-ua-mobile", "?0",
                        "sec-ch-ua-platform", "\"Windows\"",
                        "sec-fetch-dest", "document",
                        "sec-fetch-mode", "navigate",
                        "sec-fetch-site", "none",
                        "sec-fetch-user", "?1",
                        "Upgrade-Insecure-Requests", "1"
                )));
        
        // Hide webdriver flag from browser fingerprint to bypass bot detection/CAPTCHAs
        context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        
        System.out.println("[Scraper] Browser started.");

        // Warm-up step: visit Amazon homepage once to establish valid session cookies (session-id, etc.)
        try {
            Page page = context.newPage();
            page.navigate("https://www.amazon.com.br", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(3000);
            page.close();
        } catch (Exception ignored) {}
    }

    @Override
    public void close() {
        if (context != null) { try { context.close(); } catch (Exception ignored) {} context = null; }
        if (browser != null) { try { browser.close(); } catch (Exception ignored) {} browser = null; }
        if (playwright != null) { try { playwright.close(); } catch (Exception ignored) {} playwright = null; }
        System.out.println("[Scraper] Browser closed.");
    }

    @Override
    public Float fetchPrice(String url, String storeName) {
        if (context == null) {
            throw new IllegalStateException("Scraper not initialized. Call open() before fetchPrice().");
        }
        Page page = null;
        try {
            page = context.newPage();

            // Amazon uses LOAD (not NETWORKIDLE) because it fires infinite tracking requests
            // that prevent networkidle from ever being reached, causing a 30s timeout
            WaitUntilState waitStrategy = storeName.equalsIgnoreCase("Amazon")
                    ? WaitUntilState.LOAD
                    : WaitUntilState.DOMCONTENTLOADED;

            page.setDefaultTimeout(30000);

            System.out.println("-> Navigating to " + storeName + ": " + url);
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(waitStrategy));

            // Wait for JS rendering (especially important for SPAs like Kabum)
            int waitMs = storeName.equalsIgnoreCase("Kabum") ? 7000 : 4000;
            page.waitForTimeout(waitMs);

            // Self-healing navigation loop for Amazon WAF CAPTCHAs
            if (storeName.equalsIgnoreCase("Amazon")) {
                int attempts = 0;
                boolean isCaptcha = true;
                while (attempts < 3 && isCaptcha) {
                    String title = page.title();
                    String contentLower = page.content().toLowerCase();
                    
                    isCaptcha = title.equals("Amazon.com.br")
                            || contentLower.contains("captcha")
                            || contentLower.contains("api-services-support@amazon.com")
                            || contentLower.contains("correios.donotsend");
                    
                    if (isCaptcha) {
                        attempts++;
                        if (attempts < 3) {
                            System.out.println("Amazon CAPTCHA detected. Retrying...");
                            page.waitForTimeout(4000 + (long)(Math.random() * 3000));
                            page.navigate(url, new Page.NavigateOptions().setWaitUntil(waitStrategy));
                            page.waitForTimeout(waitMs);
                        }
                    }
                }
            }

            Float price = extractPriceByStore(page, storeName);

            if (price == null) {
                // Second attempt after additional wait
                page.waitForTimeout(3000);
                price = extractPriceByStore(page, storeName);
            }

            if (price == null) {
                System.out.println("   [WARNING] Price not found via selectors at " + storeName + ". Trying Regex on HTML...");
                price = extractPriceRegex(page.content());
            }

            return price;

        } catch (Exception e) {
            System.out.println("   [ERROR] Failed to access " + storeName + ": " + e.getMessage().split("\n")[0]);
            return null;
        } finally {
            if (page != null) { try { page.close(); } catch (Exception ignored) {} }
        }
    }

    private Float extractPriceByStore(Page page, String storeName) {
        try {
            if (storeName.equalsIgnoreCase("Amazon")) {
                return extractPriceAmazon(page);
            } else if (storeName.equalsIgnoreCase("Kabum")) {
                return extractPriceKabum(page);
            }
        } catch (Exception e) {
            System.out.println("   [WARNING] Exception extracting price from " + storeName + ": " + e.getMessage().split("\n")[0]);
        }
        return null;
    }

    /**
     * Amazon: uses evaluate() to read textContent from 'a-offscreen' elements that are
     * hidden in the DOM (display:none via CSS) but contain the real price.
     * Uses LOAD strategy because Amazon never reaches networkidle.
     *
     * Selectors in priority order (final purchase price, ignoring crossed-out list price):
     *   1. apexPriceToPay  - main product price (most reliable)
     *   2. priceToPay      - alternative for some products
     *   3. corePrice       - central price container
     *   4. priceblock_dealprice / priceblock_ourprice - legacy fallback
     */
    private Float extractPriceAmazon(Page page) {
        String[] selectors = {
            "span.a-price.apexPriceToPay",
            "span.a-price.priceToPay",
            "#corePriceDisplay_desktop_feature_div span.a-price",
            "#corePrice_feature_div span.a-price",
            "span.a-price", // General fallback for any standard price
            "#priceblock_dealprice",
            "#priceblock_ourprice"
        };

        for (String selector : selectors) {
            try {
                // evaluate() reads textContent even from hidden elements (display:none)
                // which Playwright's isVisible()/innerText() ignores.
                // It handles offscreen, direct elements, or builds from whole + fraction.
                String text = (String) page.evaluate(
                    "sel => {\n" +
                    "    const el = document.querySelector(sel);\n" +
                    "    if (!el) return null;\n" +
                    "    \n" +
                    "    // 1. Check if there is a nested a-offscreen\n" +
                    "    const offscreen = el.querySelector('span.a-offscreen');\n" +
                    "    if (offscreen && offscreen.textContent) {\n" +
                    "        return offscreen.textContent.trim();\n" +
                    "    }\n" +
                    "    \n" +
                    "    // 2. If the element itself is a-offscreen\n" +
                    "    if (el.classList.contains('a-offscreen')) {\n" +
                    "        return el.textContent.trim();\n" +
                    "    }\n" +
                    "    \n" +
                    "    // 3. Build price from whole and fraction components if visible\n" +
                    "    const wholeEl = el.querySelector('.a-price-whole');\n" +
                    "    const fractionEl = el.querySelector('.a-price-fraction');\n" +
                    "    if (wholeEl && wholeEl.textContent) {\n" +
                    "        const wholeText = wholeEl.textContent.replace(/[^\\d]/g, '');\n" +
                    "        const fractionText = fractionEl ? fractionEl.textContent.replace(/[^\\d]/g, '') : '00';\n" +
                    "        return 'R$ ' + wholeText + ',' + fractionText;\n" +
                    "    }\n" +
                    "    \n" +
                    "    return el.textContent ? el.textContent.trim() : null;\n" +
                    "}",
                    selector
                );
                if (text != null && !text.isBlank() && text.contains("R$")) {
                    Float price = parsePrice(text.trim());
                    if (price != null && price > 500.0) {
                        System.out.println("Amazon price: R$ " + price);
                        return price;
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Kabum: uses h4.finalPrice which contains the PIX/cash price.
     *
     * Structure confirmed via live DOM inspection (18/05/2026):
     *   div[class*="priceContainer"]
     *     span.oldPrice          -> original crossed-out price (NOT wanted)
     *     h4.finalPrice          -> PIX cash price (THIS ONE)
     *     ... installments ...
     *
     * The selector [class*="price"] picks oldPrice first — that's why it was wrong before.
     */
    private Float extractPriceKabum(Page page) {
        // Primary selectors: h4 with class finalPrice (confirmed live)
        String[] primarySelectors = {
            "h4.finalPrice",
            "[class*='finalPrice']",
            "h4[class*='PriceCard']"
        };

        for (String selector : primarySelectors) {
            try {
                String text = (String) page.evaluate(
                    "sel => { const el = document.querySelector(sel); return el ? el.innerText : null; }",
                    selector
                );
                if (text != null && text.contains("R$")) {
                    Float price = parsePrice(text.trim());
                    if (price != null && price > 100.0) {
                        System.out.println("Kabum price: R$ " + price);
                        return price;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Fallback: find the h4 that contains "R$" and is not the crossed-out price
        try {
            String text = (String) page.evaluate(
                "() => { " +
                "  const h4s = Array.from(document.querySelectorAll('h4')); " +
                "  const target = h4s.find(el => el.innerText.includes('R$')); " +
                "  return target ? target.innerText : null; " +
                "}"
            );
            if (text != null && text.contains("R$")) {
                Float price = parsePrice(text.trim());
                if (price != null && price > 100.0) {
                    System.out.println("Kabum price: R$ " + price);
                    return price;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Parses a price string (e.g. "R$ 4.179,05") to Float (4179.05).
     */
    private Float parsePrice(String priceText) {
        if (priceText == null) return null;
        try {
            // If multiple lines, take the one containing R$
            if (priceText.contains("\n")) {
                for (String line : priceText.split("\n")) {
                    if (line.contains("R$")) { priceText = line.trim(); break; }
                }
            }
            // Remove everything except digits, dot and comma
            String cleaned = priceText.replaceAll("[^\\d,.]", "").trim();
            if (cleaned.isEmpty()) return null;

            // Brazilian format: "4.179,05" -> remove thousand separator, swap decimal comma
            if (cleaned.contains(",") && cleaned.contains(".")) {
                // dot before comma = thousand separator (e.g. 4.179,05)
                if (cleaned.lastIndexOf(".") < cleaned.lastIndexOf(",")) {
                    cleaned = cleaned.replace(".", "").replace(",", ".");
                } else {
                    // dot after comma = US decimal (e.g. 3,999.00) — rare in Brazil
                    cleaned = cleaned.replace(",", "");
                }
            } else if (cleaned.contains(",")) {
                // Comma only: "4179,05" -> "4179.05"
                cleaned = cleaned.replace(",", ".");
            }
            float val = Float.parseFloat(cleaned);
            return val > 0 ? val : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Last-resort fallback: extracts the first "R$ X.XXX,XX" price from the full HTML.
     */
    private Float extractPriceRegex(String html) {
        Matcher matcher = PRICE_PATTERN.matcher(html);
        while (matcher.find()) {
            String raw = matcher.group(1);
            if (raw != null) {
                try {
                    String clean = raw.replace(".", "").replace(",", ".");
                    float val = Float.parseFloat(clean);
                    if (val > 200.0) return val;
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}
