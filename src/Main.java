import domain.Product;
import service.ProductService;
import utils.GenerateValue;

void main() {
    ProductService productService = new ProductService();

    productService.create(new Product(
            GenerateValue.getUUID(),
            "SKU",
            "celular",
            2
    ));

    productService.listAll();

}
