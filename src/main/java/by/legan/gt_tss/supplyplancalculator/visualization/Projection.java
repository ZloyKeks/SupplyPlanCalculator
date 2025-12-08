package by.legan.gt_tss.supplyplancalculator.visualization;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

/**
 * Интерфейс для проецирования данных в формат визуализации.
 *
 * @param <T> тип данных для проецирования
 */
public interface Projection<T> {

	/**
	 * Проецирует список входных данных в визуализацию упаковки.
	 *
	 * @param input список данных для проецирования
	 * @return PackagingVisualization содержащий спроецированные данные
	 */
	PackagingVisualization project(List<T> input);

	/**
	 * Проецирует данные и записывает их в выходной поток.
	 *
	 * @param input список данных для проецирования
	 * @param out выходной поток для записи
	 * @throws Exception если произошла ошибка при проецировании или записи
	 */
	void project(List<T> input, OutputStream out) throws Exception;

	/**
	 * Проецирует данные и записывает их в файл.
	 *
	 * @param input список данных для проецирования
	 * @param output файл для записи
	 * @throws Exception если произошла ошибка при проецировании или записи
	 */
	void project(List<T> input, File output) throws Exception;
}
