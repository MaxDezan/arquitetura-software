import domain.Price;
import domain.Product;
import service.ProductService;

import java.util.Date;
import java.util.UUID;

void main() {
    ProductService productService = new ProductService();

    // --- Creation (run once, then comment out to avoid duplicates) ---
    // Product produto = new Product("SKU-001", "Notebook", new Price(4500f, new Date()));
    // productService.save(produto);

    // Product produto2 = new Product("SKU-002", "Tablet", new Price(2500f, new Date()));
    // productService.save(produto2);

    // Product produto3 = new Product("SKU-003", "Mouse", new Price(300f, new Date()));
    // productService.save(produto3);

    // --- Fetch existing products from DB by UUID ---
    Product produto = (Product) productService.getById(UUID.fromString("47d601cf-7290-42ae-8f37-22f4ea03947f"));
    Product produto2 = (Product) productService.getById(UUID.fromString("9a8246bb-f8aa-4a48-961b-5d8d909d4280"));

    // --- Update prices ---
    produto.setPrice(5000.99f);
    productService.save(produto);

    produto2.setPrice(1899.99f);
    productService.save(produto2);

    productService.listAll();
}
