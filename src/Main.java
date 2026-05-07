import domain.Price;
import domain.Product;
import service.ProductService;
import utils.GenerateValue;

import java.util.Date;

void main() {
    ProductService productService = new ProductService();

    Product produto = new Product("SKU-001", "Notebook", new Price(4500f, new Date()));
    productService.create(produto);

    Product produto2 = new Product("SKU-002", "Tablet", new Price(2500f, new Date()));
    productService.create(produto2);

    Product produto3 = new Product("SKU-003", "Mouse", new Price(300f, new Date()));
    productService.create(produto3);

    produto.setPrice(5000.99f);
    productService.create(produto);

    produto2.setPrice(1899.99f);
    productService.create(produto2);

    productService.listAll();
}
