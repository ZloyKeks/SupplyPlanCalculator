package by.legan.gt_tss.supplyplancalculator.visualization;

import java.util.ArrayList;
import java.util.List;

/**
 * Абстрактный базовый класс для объектов визуализации.
 * Предоставляет общие свойства для шага, идентификатора, имени и плагинов.
 */
public class AbstractVisualization {

	private int step;
	private String id;
	private String name;

	private List<VisualizationPlugin> plugins = new ArrayList<>();

	/**
	 * Устанавливает номер шага для этой визуализации.
	 *
	 * @param step номер шага
	 */
	public void setStep(int step) {
		this.step = step;
	}
	
	/**
	 * Получает номер шага для этой визуализации.
	 *
	 * @return номер шага
	 */
	public int getStep() {
		return step;
	}
	
	/**
	 * Устанавливает имя этой визуализации.
	 *
	 * @param name имя
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Получает имя этой визуализации.
	 *
	 * @return имя
	 */
	public String getName() {
		return name;
	}

	/**
	 * Устанавливает идентификатор этой визуализации.
	 *
	 * @param id идентификатор
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Получает идентификатор этой визуализации.
	 *
	 * @return идентификатор
	 */
	public String getId() {
		return id;
	}

	/**
	 * Устанавливает список плагинов визуализации.
	 *
	 * @param plugins список плагинов
	 */
	public void setPlugins(List<VisualizationPlugin> plugins) {
		this.plugins = plugins;
	}
	
	/**
	 * Получает список плагинов визуализации.
	 *
	 * @return список плагинов
	 */
	public List<VisualizationPlugin> getPlugins() {
		return plugins;
	}
}
