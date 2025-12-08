package by.legan.gt_tss.supplyplancalculator.Data;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = {"item"})
public class MountPlan {
    private String name;
    private Double firstMountPlanSales_plan_left = 0D; // Специфичное для 1го месяца значение, может быть не 0 только там
    private Double Supply_plan; // В ручную выставленное значение поставок
    private Double New_Supply_plan; // Посчитываемое значение поставок
    private Double Sales_program; // План продаж
    private Double Planned_balance; // Остаток на текущий месяц
    private MountPlan parent; // Ссылка на Предыдущий месяц
    private Item item;

    /**
     * Рассчитывает и возвращает запланированный баланс для этого месяца.
     * Баланс рассчитывается как: (баланс родителя + план поставок + новый план поставок) - программа продаж
     *
     * @return рассчитанный запланированный баланс
     */
    public Double getPlanned_balance() {
        double parent_balance = 0;
        if (parent != null) parent_balance = parent.getPlanned_balance(); else parent_balance = item.getCurrentWarehouseBalance();
        Double result = (parent_balance + Supply_plan + New_Supply_plan) - Sales_program;
        setPlanned_balance(result);
        return Planned_balance;
    }
}
