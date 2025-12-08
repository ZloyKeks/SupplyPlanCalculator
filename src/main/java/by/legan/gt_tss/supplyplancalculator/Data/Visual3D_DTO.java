package by.legan.gt_tss.supplyplancalculator.Data;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Visual3D_DTO {
    @Expose
    private String seller;
    @Expose
    private List<VisualMountInfo_DTO> visualMountInfoList;
}
