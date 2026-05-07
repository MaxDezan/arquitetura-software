package service;

import adapter.DatabaseStorage;
import adapter.PersistInterface;
import domain.EntityInterface;
import domain.Product;
import java.util.ArrayList;
import java.util.UUID;

public class ProductService implements ServiceInterface {
    PersistInterface armazenamento = new DatabaseStorage<>(Product.class);

    @Override
    public void save(EntityInterface entity) {
        this.armazenamento.save(entity);
    }

    @Override
    public void delete(EntityInterface entity) {
        this.armazenamento.delete(entity);
    }

    @Override
    public void listAll() {
        ArrayList<EntityInterface> dados = armazenamento.listAll();
        for (int i = 0; i < dados.size(); i++) {
            System.out.println(dados.get(i));
        }
    }

    @Override
    public EntityInterface getById(UUID id) {
        return armazenamento.findOneById(id);
    }

}
