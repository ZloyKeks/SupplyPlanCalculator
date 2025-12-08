package by.legan.gt_tss.supplyplancalculator.Data;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VisualMountInfo_DTO {
    @Expose
    private String visual3DModelFileName;
    @Expose
    private int containersCount;
}
