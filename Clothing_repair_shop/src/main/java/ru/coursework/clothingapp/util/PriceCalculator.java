package ru.coursework.clothingapp.util;

import ru.coursework.clothingapp.model.Defect;
import ru.coursework.clothingapp.model.Item;
import ru.coursework.clothingapp.model.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Утилита для расчёта стоимости ремонта.
 * Формула: Базовая_цена × (1 + 0.2 × (Серьёзность - 1))
 */
public class PriceCalculator {

    // Базовые цены операций (из скрипта БД)
    private static final Map<String, BigDecimal> OPERATION_PRICES = Map.ofEntries(
        Map.entry("Зашить дыру", new BigDecimal("250.00")),
        Map.entry("Восстановить подкладку", new BigDecimal("650.00")),
        Map.entry("Заменить молнию", new BigDecimal("650.00")),
        Map.entry("Перешить шов", new BigDecimal("350.00")),
        Map.entry("Пришить пуговицу", new BigDecimal("80.00")),
        Map.entry("Удалить пятно", new BigDecimal("180.00")),
        Map.entry("Перешить карман", new BigDecimal("300.00")),
        Map.entry("Укрепить локти", new BigDecimal("400.00")),
        Map.entry("Восстановить вышивку", new BigDecimal("500.00")),
        Map.entry("Восстановить воротник", new BigDecimal("350.00")),
        Map.entry("Заменить петлю", new BigDecimal("100.00")),
        Map.entry("Пропарить залом", new BigDecimal("200.00")),
        Map.entry("Заменить подкладку частично", new BigDecimal("450.00")),
        Map.entry("Пришить помпон", new BigDecimal("120.00")),
        Map.entry("Восстановить бахрому", new BigDecimal("250.00")),
        Map.entry("Заменить пряжку", new BigDecimal("150.00")),
        Map.entry("Пришить мех", new BigDecimal("800.00")),
        Map.entry("Заменить резинку", new BigDecimal("100.00")),
        Map.entry("Тонирование ткани", new BigDecimal("400.00")),
        Map.entry("Заделать трещину", new BigDecimal("600.00")),
        Map.entry("Пропарить подол", new BigDecimal("200.00")),
        Map.entry("Пришить бусину", new BigDecimal("90.00"))
    );

    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("100.00");

    /**
     * Рассчитывает предварительную смету заказа на основе дефектов
     */
    public static BigDecimal calculateEstimate(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : order.getItems()) {
            for (Defect defect : item.getDefects()) {
                total = total.add(calculateDefectCost(defect));
            }
        }
        return total;
    }

    /**
     * Рассчитывает стоимость устранения дефекта по его типу
     */
    public static BigDecimal calculateDefectCost(Defect defect) {
        BigDecimal basePrice = getBasePriceForDefectType(defect.getDefectType());
        return applyMultiplier(basePrice, defect.getSeverity());
    }

    /**
     * Рассчитывает стоимость операции ремонта
     */
    public static BigDecimal calculateOperationCost(String operationName, Defect defect) {
        BigDecimal basePrice = OPERATION_PRICES.getOrDefault(operationName, DEFAULT_PRICE);
        return applyMultiplier(basePrice, defect.getSeverity());
    }

    /**
     * Применяет множитель серьёзности: каждая единица добавляет 20%
     */
    private static BigDecimal applyMultiplier(BigDecimal basePrice, int severity) {
        BigDecimal multiplier = BigDecimal.valueOf(1 + 0.2 * (severity - 1));
        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Определяет базовую цену по типу дефекта
     */
    private static BigDecimal getBasePriceForDefectType(String defectType) {
        String type = defectType.toLowerCase();
        if (type.contains("дыр") || type.contains("прорыв")) return OPERATION_PRICES.get("Зашить дыру");
        if (type.contains("подкладк")) return OPERATION_PRICES.get("Восстановить подкладку");
        if (type.contains("молни")) return OPERATION_PRICES.get("Заменить молнию");
        if (type.contains("шов")) return OPERATION_PRICES.get("Перешить шов");
        if (type.contains("пугов")) return OPERATION_PRICES.get("Пришить пуговицу");
        if (type.contains("пятн")) return OPERATION_PRICES.get("Удалить пятно");
        if (type.contains("карман")) return OPERATION_PRICES.get("Перешить карман");
        if (type.contains("локт")) return OPERATION_PRICES.get("Укрепить локти");
        if (type.contains("вышивк")) return OPERATION_PRICES.get("Восстановить вышивку");
        if (type.contains("воротник")) return OPERATION_PRICES.get("Восстановить воротник");
        if (type.contains("петл")) return OPERATION_PRICES.get("Заменить петлю");
        if (type.contains("залом")) return OPERATION_PRICES.get("Пропарить залом");
        if (type.contains("помпон")) return OPERATION_PRICES.get("Пришить помпон");
        if (type.contains("бахром")) return OPERATION_PRICES.get("Восстановить бахрому");
        if (type.contains("пряжк")) return OPERATION_PRICES.get("Заменить пряжку");
        if (type.contains("мех")) return OPERATION_PRICES.get("Пришить мех");
        if (type.contains("резинк")) return OPERATION_PRICES.get("Заменить резинку");
        if (type.contains("выцвет") || type.contains("тонир")) return OPERATION_PRICES.get("Тонирование ткани");
        if (type.contains("трещин")) return OPERATION_PRICES.get("Заделать трещину");
        if (type.contains("подол")) return OPERATION_PRICES.get("Пропарить подол");
        if (type.contains("бусин")) return OPERATION_PRICES.get("Пришить бусину");
        return DEFAULT_PRICE;
    }
}
