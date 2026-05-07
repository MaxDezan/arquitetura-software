import domain.Product;
import service.ProductService;

void main() {
    ProductService productService = new ProductService();

    Product produto = new Product("SKU-001", "Notebook", 2000f);
    productService.create(produto);

    Product produto2 = new Product("SKU-002", "Tablet", 1000f);
    productService.create(produto2);

    produto2.setPrice(1899.99f);
    productService.create(produto2);

    produto.setPrice(5000.99f);
    productService.create(produto);

    System.out.println("--- Products ---");
    productService.listAll();
}
