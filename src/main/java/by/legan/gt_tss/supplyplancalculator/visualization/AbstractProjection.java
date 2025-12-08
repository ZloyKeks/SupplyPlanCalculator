package by.legan.gt_tss.supplyplancalculator.visualization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Абстрактный класс для обобщения методов проекции данных.
 */
public abstract class AbstractProjection<T> implements Projection<T> {

    /**
     * Проектирование списка данных в формат JSON и вывод его в поток.
     *
     * @param input список данных, которые необходимо проить
     * @param out  поток для записи произведенных данных
     * @throws Exception если возникнут ошибки при выполнении метода
     */
	@Override
	public void project(List<T> input, OutputStream out) throws Exception {
		PackagingVisualization project = project(input);

		out.write(project.toJson().getBytes(StandardCharsets.UTF_8));
	}

    /**
     * Проектирование списка данных в формат JSON и написание его в файл.
     *
     * @param input список данных, которые необходимо проить
     * @param output файл, в который будет записан произведенный données
     * @throws Exception если возникнут ошибки при выполнении метода
     */
	@Override
	public void project(List<T> input, File output) throws Exception {
		FileOutputStream fout = new FileOutputStream(output);
		try {
			project(input,fout);
		} finally {
			fout.close();
		}
	}

}

