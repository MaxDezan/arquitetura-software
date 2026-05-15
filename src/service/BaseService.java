package service;

import adapter.PersistInterface;
import domain.EntityInterface;
import java.util.ArrayList;
import java.util.UUID;

public abstract class BaseService implements ServiceInterface {
    protected PersistInterface armazenamento;

    @Override
    public void save(EntityInterface entity) {
        armazenamento.save(entity);
    }

    @Override
    public void edit(EntityInterface entity) {
        armazenamento.update(entity);
    }

    @Override
    public void delete(EntityInterface entity) {
        armazenamento.delete(entity);
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
