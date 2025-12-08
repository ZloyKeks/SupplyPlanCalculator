package by.legan.gt_tss.supplyplancalculator.Data;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class VisualResult_DTO {
    @Expose
    private List<Visual3D_DTO> visual3DList;
    @Expose
    private String resultFileName;
}
