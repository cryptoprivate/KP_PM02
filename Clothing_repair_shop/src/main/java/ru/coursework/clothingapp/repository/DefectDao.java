package ru.coursework.clothingapp.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;
import ru.coursework.clothingapp.model.Defect;

import java.util.List;

public class DefectDao extends BaseDao<Defect> {
    public DefectDao() {
        super(Defect.class);
    }

    public List<String> findDistinctTypes() {
        Session session = getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<String> types = session.createQuery("SELECT DISTINCT d.defectType FROM Defect d ORDER BY d.defectType", String.class).list();
            tx.commit();
            return types;
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }
}
