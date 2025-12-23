package ru.coursework.clothingapp.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;
import ru.coursework.clothingapp.model.Item;

import java.util.List;

public class ItemDao extends BaseDao<Item> {
    public ItemDao() {
        super(Item.class);
    }

    public List<String> findDistinctTypes() {
        Session session = getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<String> types = session.createQuery("SELECT DISTINCT i.type FROM Item i ORDER BY i.type", String.class).list();
            tx.commit();
            return types;
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }
}
