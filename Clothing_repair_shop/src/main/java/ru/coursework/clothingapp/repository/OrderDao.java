package ru.coursework.clothingapp.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;
import ru.coursework.clothingapp.model.Order;

public class OrderDao extends BaseDao<Order> {

    public OrderDao() {
        super(Order.class);
    }

    @Override
    public void delete(Order order) {
        Session session = getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.createNativeQuery("DELETE FROM orders WHERE id = :id", Order.class)
                    .setParameter("id", order.getId()).executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Ошибка удаления заказа", e);
        }
    }
}
