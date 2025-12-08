package by.legan.gt_tss.supplyplancalculator.visualization;

/**
 * Интерфейс для плагинов визуализации.
 * Плагины могут расширять функциональность визуализации.
 */
public interface VisualizationPlugin {

	/**
	 * Получает уникальный идентификатор плагина.
	 *
	 * @return идентификатор плагина
	 */
	String getId();
	
}
