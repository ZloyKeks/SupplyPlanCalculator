package by.legan.gt_tss.supplyplancalculator.visualization;

/**
 * Визуализация для элемента коробки внутри контейнера.
 */
public class BoxVisualization extends StackableVisualization {

	private String type = "box";

	/**
	 * Получает тип этой визуализации.
	 *
	 * @return тип (всегда "box")
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
