import adapter.PlaywrightApiScraper;
import domain.Price;
import domain.Product;
import domain.ProductLink;
import service.CrawlerService;
import service.ProductService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

void main() {
    ProductService productService = new ProductService();

    // --- PASSO 1: Cadastro de produtos com links (rode uma vez, depois comente) ---
    // ProductLink linkAmazon = new ProductLink("Amazon", "https://www.amazon.com.br/PlayStation-5/dp/B09DFKP1GJ");
    // ProductLink linkKabum  = new ProductLink("Kabum",  "https://www.kabum.com.br/produto/107461");
    //
    // Product ps5 = new Product(
    //     "SKU-PS5",
    //     "PlayStation 5",
    //     new Price(4000f, new Date()),
    //     new ArrayList<>(List.of(linkAmazon, linkKabum))
    // );
    // productService.save(ps5);
    // System.out.println("Produto cadastrado: " + ps5);

    // --- PASSO 2: Listar todos os produtos cadastrados ---
    System.out.println("=== Produtos cadastrados ===");
    productService.listAll();

    // --- PASSO 3: Executar o Crawler (busca precos em todas as lojas) ---
    // Usa o PlaywrightApiScraper: modo HTTP puro (CURL), sem abrir navegador
    CrawlerService crawler = new CrawlerService(new PlaywrightApiScraper());
    crawler.executar();

    // --- PASSO 4: Exibir produtos atualizados apos o crawler ---
    System.out.println("\n=== Produtos apos execucao do Crawler ===");
    productService.listAll();
}
