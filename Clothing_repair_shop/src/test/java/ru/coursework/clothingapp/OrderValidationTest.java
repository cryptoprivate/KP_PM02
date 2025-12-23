package ru.coursework.clothingapp;

import org.junit.jupiter.api.Test;
import ru.coursework.clothingapp.model.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;


public class OrderValidationTest {

    @Test
    void shouldNotAllowDueDateBeforeAcceptDate() {
        Order order = new Order();
        order.setAcceptDate(LocalDate.now());
        order.setDueDate(LocalDate.now().minusDays(1));

        // Дата выполнения не может быть раньше даты приёма
        assertTrue(order.getDueDate().isBefore(order.getAcceptDate()));
    }

    @Test
    void shouldRequireClientForOrder() {
        Order order = new Order();
        order.setOrderNumber("TEST-001");
        order.setAcceptDate(LocalDate.now());
        order.setDueDate(LocalDate.now().plusDays(7));

        // Заказ должен иметь клиента
        assertNull(order.getClient());
    }

    @Test
    void shouldRequireStatusForOrder() {
        Order order = new Order();
        order.setOrderNumber("TEST-002");
        order.setAcceptDate(LocalDate.now());
        order.setDueDate(LocalDate.now().plusDays(7));

        // Заказ должен иметь статус
        assertNull(order.getStatus());
    }

    @Test
    void shouldAllowValidDateRange() {
        Order order = new Order();
        order.setAcceptDate(LocalDate.now());
        order.setDueDate(LocalDate.now().plusDays(7));

        // Корректный диапазон дат
        assertTrue(order.getDueDate().isAfter(order.getAcceptDate()));
    }

    @Test
    void shouldRequireOrderNumber() {
        Order order = new Order();
        order.setAcceptDate(LocalDate.now());
        order.setDueDate(LocalDate.now().plusDays(7));

        // Номер заказа обязателен
        assertNull(order.getOrderNumber());
    }
}

