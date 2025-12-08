package by.legan.gt_tss.supplyplancalculator.Data;

import lombok.Data;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;

@Data
public class SupplyPlanDocument {
    private XSSFWorkbook workbook;
    private List<Item> items;
}
