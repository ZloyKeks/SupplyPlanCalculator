package by.legan.gt_tss.supplyplancalculator.visualization;

import java.util.ArrayList;
import java.util.List;

/**
 * Визуализация для стека элементов внутри контейнера.
 */
public class StackVisualization extends AbstractVisualization {

	private List<StackPlacementVisualization> placements = new ArrayList<>();

	/**
	 * Добавляет визуализацию размещения в стек.
	 *
	 * @param e визуализация размещения для добавления
	 * @return true если размещение было успешно добавлено
	 */
	public boolean add(StackPlacementVisualization e) {
		return placements.add(e);
	}
	
	/**
	 * Получает список визуализаций размещений в стеке.
	 *
	 * @return список визуализаций размещений
	 */
	public List<StackPlacementVisualization> getPlacements() {
		return placements;
	}
	
	/**
	 * Устанавливает список визуализаций размещений в стеке.
	 *
	 * @param stackable список визуализаций размещений
	 */
	public void setPlacements(List<StackPlacementVisualization> stackable) {
		this.placements = stackable;
	}

	
}
