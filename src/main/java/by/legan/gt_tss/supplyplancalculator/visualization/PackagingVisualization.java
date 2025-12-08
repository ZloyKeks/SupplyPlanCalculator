package by.legan.gt_tss.supplyplancalculator.visualization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Представляет коллекцию визуализаций контейнеров для упаковки.
 */
public class PackagingVisualization {

	private List<ContainerVisualization> containers = new ArrayList<>();

	/**
	 * Получает список визуализаций контейнеров.
	 *
	 * @return список визуализаций контейнеров
	 */
	public List<ContainerVisualization> getContainers() {
		return containers;
	}

	/**
	 * Устанавливает список визуализаций контейнеров.
	 *
	 * @param containers список визуализаций контейнеров
	 */
	public void setContainers(List<ContainerVisualization> containers) {
		this.containers = containers;
	}

	/**
	 * Добавляет визуализацию контейнера в коллекцию.
	 *
	 * @param e визуализация контейнера для добавления
	 * @return true если контейнер был успешно добавлен
	 */
	public boolean add(ContainerVisualization e) {
		return containers.add(e);
	}
	
	/**
	 * Преобразует визуализацию упаковки в формат JSON.
	 *
	 * @return строковое представление визуализации в формате JSON
	 * @throws JsonProcessingException если обработка JSON не удалась
	 */
	public String toJson() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
	}
	
}
