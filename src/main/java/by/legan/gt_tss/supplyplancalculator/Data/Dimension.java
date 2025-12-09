package by.legan.gt_tss.supplyplancalculator.Data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс для представления размеров (ширина, глубина, высота).
 * Используется для совместимости со старым кодом, так как в версии 4.x библиотеки
 * 3d-bin-container-packing класс Dimension отсутствует.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dimension {
    private String name;
    private int width;
    private int depth;
    private int height;
    
    public Dimension(int width, int depth, int height) {
        this(null, width, depth, height);
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public int getHeight() {
        return height;
    }
}

