import domain.Product;

void main() {
    Product product = new Product();

    product.setName("Celular");
    product.setSku("PROD001");
    product.setPrice(3200);
    product.setPrice(3100);

    Product product2 = new Product();
    product2.setName("Headset");
    product2.setSku("PROD002");
    product2.setPrice(370);
    product2.setPrice(450);

    Product product3 = new Product();
    product3.setName("Tablet");
    product3.setSku("PROD003");
    product3.setPrice(2500);
    product3.setPrice(2000);

    System.out.println(product2);
}
