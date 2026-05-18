package service;

import adapter.PersistInterface;
import domain.EntityInterface;
import java.util.ArrayList;
import java.util.UUID;

public abstract class BaseService implements ServiceInterface {
    protected PersistInterface storage;

    @Override
    public void save(EntityInterface entity) {
        storage.save(entity);
    }

    @Override
    public void edit(EntityInterface entity) {
        storage.update(entity);
    }

    @Override
    public void delete(EntityInterface entity) {
        storage.delete(entity);
    }

    @Override
    public void listAll() {
        ArrayList<EntityInterface> data = storage.listAll();
        for (int i = 0; i < data.size(); i++) {
            System.out.println(data.get(i));
        }
    }

    @Override
    public EntityInterface getById(UUID id) {
        return storage.findOneById(id);
    }
}
