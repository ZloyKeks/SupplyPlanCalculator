package by.legan.gt_tss.supplyplancalculator.webSocket;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum with webSocket end points information
 */
@Getter
@AllArgsConstructor
public enum WebSocketEndPointsEnum {
    CALC(1L, "/topic/calc"),
    CHAT(2L, "/topic/chat");

    /**
     * id
     */
    private Long id;
    /**
     * end point
     */
    private String endPoint;
}
