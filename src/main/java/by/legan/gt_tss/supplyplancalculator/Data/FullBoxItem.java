package by.legan.gt_tss.supplyplancalculator.Data;


import com.github.skjolber.packing.api.BoxItem;
import lombok.Data;

@Data
public class FullBoxItem {
    private BoxItem boxItem;
    private Item item;
}