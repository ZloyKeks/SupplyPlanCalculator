package by.legan.gt_tss.supplyplancalculator.visualization;

/**
 * Визуализация для контейнера с его размерами и стеком элементов.
 */
public class ContainerVisualization extends StackableVisualization {

	private int loadDx;
	private int loadDy;
	private int loadDz;
	
	private StackVisualization stack;
	
	private String type = "container";
	
	/**
	 * Получает загруженный размер в направлении X.
	 *
	 * @return загруженный размер X
	 */
	public int getLoadDx() {
		return loadDx;
	}

	/**
	 * Устанавливает загруженный размер в направлении X.
	 *
	 * @param loadDx загруженный размер X
	 */
	public void setLoadDx(int loadDx) {
		this.loadDx = loadDx;
	}

	/**
	 * Получает загруженный размер в направлении Y.
	 *
	 * @return загруженный размер Y
	 */
	public int getLoadDy() {
		return loadDy;
	}

	/**
	 * Устанавливает загруженный размер в направлении Y.
	 *
	 * @param loadDy загруженный размер Y
	 */
	public void setLoadDy(int loadDy) {
		this.loadDy = loadDy;
	}

	/**
	 * Получает загруженный размер в направлении Z.
	 *
	 * @return загруженный размер Z
	 */
	public int getLoadDz() {
		return loadDz;
	}

	/**
	 * Устанавливает загруженный размер в направлении Z.
	 *
	 * @param loadDz загруженный размер Z
	 */
	public void setLoadDz(int loadDz) {
		this.loadDz = loadDz;
	}

	/**
	 * Получает визуализацию стека содержащую размещенные элементы.
	 *
	 * @return визуализация стека
	 */
	public StackVisualization getStack() {
		return stack;
	}
	
	/**
	 * Устанавливает визуализацию стека содержащую размещенные элементы.
	 *
	 * @param stack визуализация стека
	 */
	public void setStack(StackVisualization stack) {
		this.stack = stack;
	}

	/**
	 * Получает тип этой визуализации.
	 *
	 * @return тип (всегда "container")
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Устанавливает тип этой визуализации.
	 *
	 * @param type тип для установки
	 */
	public void setType(String type) {
		this.type = type;
	}
}
