package by.legan.gt_tss.supplyplancalculator.Data;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.google.gson.annotations.Expose;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Представляет результат расчета для конкретного поставщика и месяца.
 * Содержит упакованные контейнеры, элементы коробок и планы модификации.
 */
@Data
public class ResultCalculate {
    private String seller;
    @Expose
    private String visual3DModelFileName;
    @Expose
    private int containersCount;
    private List<Container> pack;
    private List<BoxItem> boxItems = new ArrayList<>();
    private List<ModificationPlan> modificationPlan = new ArrayList<>();

    /**
     * Находит индекс плана модификации для конкретного товара.
     *
     * @param item товар для поиска
     * @return индекс плана модификации, или -1 если не найден
     */
    public int findByItem(Item item) {
        int index = 0;
        for(ModificationPlan modificationPlan : modificationPlan) {
            if (modificationPlan.getItem().equals(item)) return index;
            index++;
        }
        return -1;
    }

    /**
     * Объединяет планы модификации добавляя значения для существующих товаров или добавляя новые планы.
     *
     * @param modificationPlan список планов модификации для объединения с существующими планами
     */
    public void uniteModificationPlan(List<ModificationPlan>  modificationPlan){
        for (ModificationPlan newPlan : modificationPlan) {
            ModificationPlan oldPlan = findPlan(newPlan);
            if (oldPlan != null) {
                double newSupplyPlan = newPlan.getValue_New_Supply_plan();
                oldPlan.setValue_New_Supply_plan(oldPlan.getValue_New_Supply_plan()+newSupplyPlan);
            } else this.modificationPlan.add(newPlan);
        }
    }

    /**
     * Находит существующий план модификации по имени товара.
     *
     * @param plan план модификации для поиска
     * @return существующий план модификации если найден, null в противном случае
     */
    private ModificationPlan findPlan(ModificationPlan plan){
        for (ModificationPlan oldPlan : this.modificationPlan) {
            if (oldPlan.getItem().getName().equals(plan.getItem().getName())) return oldPlan;
        }
        return null;
    }
}