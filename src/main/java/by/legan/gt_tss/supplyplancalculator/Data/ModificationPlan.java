package by.legan.gt_tss.supplyplancalculator.Data;

import lombok.Data;

@Data
public class ModificationPlan{
    private Item item;
    private int mount;
    private Double value_New_Supply_plan;
}