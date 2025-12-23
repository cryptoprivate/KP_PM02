package ru.coursework.clothingapp.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;
import ru.coursework.clothingapp.util.HibernateSessionFactoryUtil;

import java.util.List;

public abstract class BaseDao<T> {
    private final Class<T> clazz;

    public BaseDao(Class<T> clazz) {
        this.clazz = clazz;
    }

    protected Session getCurrentSession() {
        return HibernateSessionFactoryUtil.getSessionFactory().getCurrentSession();
    }

    public void save(T entity) {
        executeInTransaction(session -> session.persist(entity));
    }

    public void update(T entity) {
        executeInTransaction(session -> session.merge(entity));
    }

    public void delete(T entity) {
        executeInTransaction(session -> session.remove(entity));
    }

    public T findOne(Integer id) {
        Session session = getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            T result = session.get(clazz, id);
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException("Ошибка поиска " + clazz.getSimpleName(), e);
        }
    }

    public List<T> findAll() {
        Session session = getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<T> result = session.createQuery("FROM " + clazz.getSimpleName(), clazz).list();
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException("Ошибка загрузки " + clazz.getSimpleName(), e);
        }
    }

    private void executeInTransaction(SessionAction action) {
        Session session = getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            action.execute(session);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException("Ошибка операции " + clazz.getSimpleName(), e);
        }
    }

    @FunctionalInterface
    private interface SessionAction {
        void execute(Session session);
    }
}
