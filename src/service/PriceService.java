package service;

import adapter.DatabaseStorage;
import adapter.PersistInterface;
import domain.Price;


public class PriceService extends BaseService {
    public PriceService() {
        storage = new DatabaseStorage<>(Price.class);
    }

}
