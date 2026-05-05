import domain.Product;
import service.ProductService;

void main() {
    ProductService productService = new ProductService();

    Product produto = new Product("SKU", "asas", 2f);
    produto.setPrice(3f);
    produto.setPrice(4f);
    productService.create(produto);

    productService.listAll();
}
