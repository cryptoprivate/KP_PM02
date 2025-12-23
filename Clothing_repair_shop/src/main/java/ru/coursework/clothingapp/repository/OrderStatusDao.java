package ru.coursework.clothingapp.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;
import ru.coursework.clothingapp.model.OrderStatus;

import java.util.List;

public class OrderStatusDao extends BaseDao<OrderStatus> {

    public OrderStatusDao() {
        super(OrderStatus.class);
    }

    public OrderStatus findByName(String name) {
        Session session = getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            OrderStatus result = session.createQuery("FROM OrderStatus WHERE name = :name", OrderStatus.class)
                    .setParameter("name", name).uniqueResult();
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Ошибка поиска статуса", e);
        }
    }

    public List<OrderStatus> findAllUnique() {
        Session session = getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<OrderStatus> result = session.createQuery(
                "SELECT os FROM OrderStatus os WHERE os.id IN " +
                "(SELECT MIN(os2.id) FROM OrderStatus os2 GROUP BY os2.name) ORDER BY os.name",
                OrderStatus.class).getResultList();
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Ошибка загрузки статусов", e);
        }
    }
}
