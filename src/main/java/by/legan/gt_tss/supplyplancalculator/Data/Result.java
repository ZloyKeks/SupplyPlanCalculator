package by.legan.gt_tss.supplyplancalculator.Data;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Представляет результат расчетов плана поставок.
 * Содержит документ, результаты расчетов организованные по поставщикам, и имя файла результата.
 */
@Data
@NoArgsConstructor
public class Result {
    private SupplyPlanDocument document;
    @Expose
    private Map<String, List<ResultCalculate>> resultCalculateMap;
    @Expose
    private String resultFileName;

    /**
     * Преобразует результат в VisualResult_DTO для целей визуализации.
     *
     * @return VisualResult_DTO содержащий данные визуализации организованные по поставщикам
     */
    public VisualResult_DTO toVisualResult_DTO(){
        List<Visual3D_DTO> visual3DList = new ArrayList<>();
        List<String> sellers = new ArrayList<>(resultCalculateMap.keySet());
        for (String seller : sellers) {
            Visual3D_DTO visual3D_dto = new Visual3D_DTO();
            visual3D_dto.setSeller(seller);
            visual3D_dto.setVisualMountInfoList(new ArrayList<>());
            List<ResultCalculate> resultList = resultCalculateMap.get(seller);
            for (ResultCalculate resultCalculate : resultList) {
                if (resultCalculate != null) {
                    visual3D_dto.getVisualMountInfoList().add(new VisualMountInfo_DTO(resultCalculate.getVisual3DModelFileName(), resultCalculate.getContainersCount()));
                }
            }
            visual3DList.add(visual3D_dto);
        }
        return new VisualResult_DTO(visual3DList,resultFileName);
    }
}
