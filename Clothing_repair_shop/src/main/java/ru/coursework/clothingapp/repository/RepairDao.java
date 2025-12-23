package ru.coursework.clothingapp.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;
import ru.coursework.clothingapp.model.Item;
import ru.coursework.clothingapp.model.Repair;

import java.util.Collections;
import java.util.List;

public class RepairDao extends BaseDao<Repair> {

    public RepairDao() {
        super(Repair.class);
    }

    public List<Repair> findByItem(Item item) {
        if (item == null || item.getId() == null) return Collections.emptyList();
        Session session = getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<Repair> repairs = session.createQuery("FROM Repair WHERE item = :item", Repair.class)
                    .setParameter("item", item).list();
            tx.commit();
            return repairs;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Ошибка поиска операций", e);
        }
    }

    public List<String> findDistinctOperations() {
        Session session = getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<String> ops = session.createQuery(
                "SELECT DISTINCT r.operationName FROM Repair r ORDER BY r.operationName", String.class).list();
            tx.commit();
            return ops;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Ошибка загрузки операций", e);
        }
    }
}
