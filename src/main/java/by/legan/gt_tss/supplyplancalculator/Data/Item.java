package by.legan.gt_tss.supplyplancalculator.Data;

import lombok.Data;

import java.util.List;

@Data
public class Item {
    private String article; // Артикул
    private String name; // Название продукта
    private String seller; // Имя продавцы
    private Double Delivery_time; // Срок поставки в днях
    private Dimension dimensions_container = new Dimension(); // Размеры контейнера в котором они будут грузится
    private Double container_lifting; // Грузоподъемность контейнера, кг.
    private Dimension dimensions_item = new Dimension(); // Размеры груза, mm

    private String type;

    //TODO Временно поставил что бы убрать ошибку но нужно убирать и реагировать на ошибки
    private Double weight = 999.0; // Масса, кг
    //TODO Временно поставил что бы убрать ошибку но нужно убирать и реагировать на ошибки
    private Double price = 0.0; // Цена
    private String currency_type; // Валюта

    // Manual edit field
    private Double currentWarehouseBalance = 0D; // Остатки
    private Double manual_Supply; // Поступления

    private Double firstMountPlanSales_plan_left; // Специфичное для 1го месяца значение
    private List<MountPlan> mountPlans; // План закупок и продаж по месяцам
}
