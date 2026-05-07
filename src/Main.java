import domain.Price;
import domain.Product;
import service.ProductService;

import java.util.Date;

void main() {
    ProductService productService = new ProductService();

    // Create a product with an initial price directly
    Product produto = new Product("SKU-001", "Notebook", new Price(2000f, new Date()));
    productService.create(produto);

    productService.listAll();
}
