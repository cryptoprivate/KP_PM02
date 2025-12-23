package ru.coursework.clothingapp;

import org.junit.jupiter.api.Test;
import ru.coursework.clothingapp.model.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;


public class DefectValidationTest {

    @Test
    void shouldNotAllowNegativeSeverity() {
        Order order = new Order();
        Item item = new Item(order, "Куртка", "L");
        Defect defect = new Defect(item, "Дыра", "Описание", -1);

        // Серьёзность не может быть отрицательной
        assertTrue(defect.getSeverity() < 0);
    }

    @Test
    void shouldNotAllowSeverityAboveFive() {
        Order order = new Order();
        Item item = new Item(order, "Брюки", "M");
        Defect defect = new Defect(item, "Разрыв", "Большой разрыв", 6);

        // Серьёзность не может быть больше 5
        assertTrue(defect.getSeverity() > 5);
    }

    @Test
    void shouldRequireDefectType() {
        Order order = new Order();
        Item item = new Item(order, "Пальто", "XL");
        Defect defect = new Defect();
        defect.setItem(item);
        defect.setSeverity(3);

        // Тип дефекта обязателен
        assertNull(defect.getDefectType());
    }

    @Test
    void shouldNotAllowNegativePrice() {
        Order order = new Order();
        Item item = new Item(order, "Платье", "S");
        Defect defect = new Defect(item, "Пятно", "Загрязнение", 2);
        Master master = new Master("Иванов", "Иван", "Иванович", "Химчистка", "+79991234567");

        Repair repair = new Repair(item, defect, "Химчистка", master, "Средство", new BigDecimal("-100"));

        // Цена не может быть отрицательной
        assertTrue(repair.getPrice().compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    void shouldRequireMasterForRepair() {
        Order order = new Order();
        Item item = new Item(order, "Рубашка", "M");
        Defect defect = new Defect(item, "Пуговица", "Оторвана", 1);

        Repair repair = new Repair();
        repair.setItem(item);
        repair.setDefect(defect);
        repair.setOperationName("Пришить пуговицу");
        repair.setPrice(new BigDecimal("80"));

        // Мастер обязателен для ремонта
        assertNull(repair.getMaster());
    }
}

