package service;

import adapter.DatabaseStorage;
import adapter.PersistInterface;
import domain.Product;


public class ProductService extends BaseService {
    public ProductService() {
        storage = new DatabaseStorage<>(Product.class);
    }

}
