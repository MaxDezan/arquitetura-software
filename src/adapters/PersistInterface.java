package adapters;

import domain.EntityInterface;

public interface PersistInterface {
    void save(EntityInterface entity);
    void delete ();
}
