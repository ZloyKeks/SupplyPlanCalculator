package by.legan.gt_tss.supplyplancalculator.webSocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusProcessMessageDTO {
    private String user_key;
    private String type;
    private String message;
    private String jsonData;
}
