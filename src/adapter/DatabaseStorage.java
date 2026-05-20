package adapter;

import domain.EntityInterface;
import jakarta.persistence.*;
import org.hibernate.Hibernate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class DatabaseStorage<T extends EntityInterface> implements PersistInterface {

    private static final String PERSISTENCE_UNIT = "default";

    private final EntityManagerFactory emf;
    private final Class<T> type;

    public DatabaseStorage(Class<T> type) {
        this.emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
        this.type = type;
    }

    private void initLazyCollections(Object entity) {
        if (entity == null) return;
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (!Collection.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value != null) Hibernate.initialize(value);
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    /**
     * Executes a callback function within a database transaction, managing the EntityManager lifecycle.
     */
    private <R> R executeInTransaction(java.util.function.Function<EntityManager, R> action) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            R result = action.apply(em);
            em.getTransaction().commit();
            return result;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Executes a callback consumer within a database transaction, managing the EntityManager lifecycle.
     */
    private void runInTransaction(java.util.function.Consumer<EntityManager> action) {
        executeInTransaction(em -> {
            action.accept(em);
            return null;
        });
    }

    @Override
    public void save(EntityInterface entity) {
        runInTransaction(em -> em.persist(entity));
    }

    @Override
    public void update(EntityInterface entity) {
        runInTransaction(em -> em.merge(entity));
    }

    @Override
    public void delete(EntityInterface entity) {
        runInTransaction(em -> {
            EntityInterface managed = em.find(entity.getClass(), entity.getUUID());
            if (managed != null) {
                em.remove(managed);
            }
        });
    }

    @Override
    public ArrayList<EntityInterface> listAll() {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = "SELECT e FROM " + type.getSimpleName() + " e";
            List<T> result = em.createQuery(jpql, type).getResultList();
            result.forEach(this::initLazyCollections);
            return new ArrayList<>(result);
        } finally {
            em.close();
        }
    }

    @Override
    public EntityInterface findOneById(UUID id) {
        EntityManager em = emf.createEntityManager();
        try {
            T entity = em.find(type, id);
            initLazyCollections(entity);
            return entity;
        } finally {
            em.close();
        }
    }

    /**
     * Removes history entries with a zero price (registration placeholder).
     * Use this to clean up invalid history created when registering a product with price 0f.
     */
    public void clearZeroValueHistory() {
        runInTransaction(em -> {
            int deleted = em.createQuery(
                "DELETE FROM Price p WHERE p.product IS NOT NULL AND p.price = 0"
            ).executeUpdate();
            if (deleted > 0) {
                System.out.println("[DB] Zero-value history entries removed: " + deleted);
            }
        });
    }

    /**
     * Clears all price history from the database,
     * keeping only the current price for each product.
     */
    public void clearAllHistory() {
        runInTransaction(em -> {
            int deleted = em.createQuery(
                "DELETE FROM Price p WHERE p.product IS NOT NULL"
            ).executeUpdate();
            System.out.println("[DB] Price history cleared: " + deleted + " records removed.");
        });
    }

    /**
     * Finds a product by SKU. Returns null if not found.
     * Useful to check if a product is already registered before inserting.
     */
    public T findBySku(String sku) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = "SELECT e FROM " + type.getSimpleName() + " e WHERE e.sku = :sku";
            List<T> result = em.createQuery(jpql, type).setParameter("sku", sku).getResultList();
            if (result.isEmpty()) return null;
            T entity = result.get(0);
            initLazyCollections(entity);
            return entity;
        } finally {
            em.close();
        }
    }

    public void close() {
        if (emf != null && emf.isOpen()) emf.close();
    }
}
