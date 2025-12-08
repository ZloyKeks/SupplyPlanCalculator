package by.legan.gt_tss.supplyplancalculator.visualization;

/**
 * Визуализация для размещения штабелируемого элемента внутри контейнера.
 * Содержит координаты позиции (x, y, z) и ссылку на штабелируемый элемент.
 */
public class StackPlacementVisualization extends AbstractVisualization {

	private int x;
	private int y;
	private int z;
	
	private StackableVisualization stackable;

	/**
	 * Получает координату X размещения.
	 *
	 * @return координата X
	 */
	public int getX() {
		return x;
	}
	
	/**
	 * Устанавливает координату X размещения.
	 *
	 * @param x координата X
	 */
	public void setX(int x) {
		this.x = x;
	}
	
	/**
	 * Получает координату Y размещения.
	 *
	 * @return координата Y
	 */
	public int getY() {
		return y;
	}
	
	/**
	 * Устанавливает координату Y размещения.
	 *
	 * @param y координата Y
	 */
	public void setY(int y) {
		this.y = y;
	}
	
	/**
	 * Получает координату Z размещения.
	 *
	 * @return координата Z
	 */
	public int getZ() {
		return z;
	}
	
	/**
	 * Устанавливает координату Z размещения.
	 *
	 * @param z координата Z
	 */
	public void setZ(int z) {
		this.z = z;
	}

	/**
	 * Устанавливает штабелируемый элемент визуализации который размещается.
	 *
	 * @param stackable штабелируемая визуализация
	 */
	public void setStackable(StackableVisualization stackable) {
		this.stackable = stackable;
	}

	/**
	 * Получает штабелируемый элемент визуализации который размещается.
	 *
	 * @return штабелируемая визуализация
	 */
	public StackableVisualization getStackable() {
		return stackable;
	}
}
