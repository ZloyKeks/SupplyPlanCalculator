package by.legan.gt_tss.supplyplancalculator.controllers;


import by.legan.gt_tss.supplyplancalculator.Data.Result;
import by.legan.gt_tss.supplyplancalculator.Data.VisualResult_DTO;
import by.legan.gt_tss.supplyplancalculator.service.CalculatorService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

/**
 * REST контроллер для обработки загрузки файлов, расчетов и скачивания файлов.
 * Предоставляет конечные точки для загрузки Excel файлов, расчета планов поставок и скачивания результатов.
 */
@RestController
@RequestMapping("/rest")
public class MainRestController {

    @Autowired
    CalculatorService calculatorService;

    /**
     * Обрабатывает запросы на загрузку файлов.
     * Сохраняет загруженный файл во временное место и возвращает имя файла.
     *
     * @param file multipart файл для загрузки
     * @return ResponseEntity содержащий временное имя файла при успехе, или BAD_REQUEST при ошибке
     * @throws IOException если произошла ошибка ввода-вывода при операциях с файлом
     * @throws ExecutionException если произошла ошибка при выполнении
     * @throws InterruptedException если операция была прервана
     */
    @RequestMapping(value={"/upload"}, method = RequestMethod.POST)
    public ResponseEntity uploadFile(@RequestParam("file") MultipartFile file) throws IOException, ExecutionException, InterruptedException {
        if (file.getOriginalFilename() != null && file.getOriginalFilename().length() > 0) {
            File tempFile = File.createTempFile(file.getOriginalFilename(), ".xlsx");
            IOUtils.copy(file.getInputStream(), new FileOutputStream(tempFile));
            return ResponseEntity.status(HttpStatus.OK).body(tempFile.getName());
        } else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    /**
     * Рассчитывает план поставок на основе загруженного файла и параметров.
     * Возвращает результаты расчета в формате JSON.
     *
     * @param name имя загруженного файла
     * @param user_key ключ пользователя для WebSocket связи
     * @param dop_l дополнительное измерение длины
     * @param dop_w дополнительное измерение ширины
     * @param dop_h дополнительное измерение высоты
     * @param max_h максимальное ограничение высоты
     * @return ResponseEntity содержащий результаты расчета в формате JSON
     * @throws IOException если произошла ошибка ввода-вывода
     * @throws ExecutionException если произошла ошибка при выполнении
     * @throws InterruptedException если операция была прервана
     */
    @GetMapping(value={"/calculate/{name}/{user_key}/{dop_l}/{dop_w}/{dop_h}/{max_h}"})
    public ResponseEntity calculate(@PathVariable("name") String name,
                                    @PathVariable("user_key") String user_key,
                                    @PathVariable("dop_l") int dop_l,
                                    @PathVariable("dop_w") int dop_w,
                                    @PathVariable("dop_h") int dop_h,
                                    @PathVariable("max_h") int max_h) throws IOException, ExecutionException, InterruptedException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        File file = new File(tmpdir+"/"+name);
        VisualResult_DTO result = calculatorService.calculateAndGetUploadFile(file, user_key, dop_l, dop_w, dop_h, max_h);
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .excludeFieldsWithModifiers(0x00000080) // STATIC|TRANSIENT in the default configuration
                .create();
        String json = gson.toJson(result);
        return ResponseEntity.status(HttpStatus.OK).body(json);
    }

    /**
     * Рассчитывает план поставок используя первый метод расчета.
     * Это более быстрый метод расчета для первоначального тестирования.
     *
     * @param name имя загруженного файла
     * @param user_key ключ пользователя для WebSocket связи
     * @param dop_l дополнительное измерение длины
     * @param dop_w дополнительное измерение ширины
     * @param dop_h дополнительное измерение высоты
     * @param max_h максимальное ограничение высоты
     * @return ResponseEntity содержащий результаты расчета в формате JSON
     * @throws IOException если произошла ошибка ввода-вывода
     * @throws ExecutionException если произошла ошибка при выполнении
     * @throws InterruptedException если операция была прервана
     */
    @GetMapping(value={"/calculateFirst/{name}/{user_key}/{dop_l}/{dop_w}/{dop_h}/{max_h}"})
    public ResponseEntity calculateFirst(@PathVariable("name") String name,
                                    @PathVariable("user_key") String user_key,
                                    @PathVariable("dop_l") int dop_l,
                                    @PathVariable("dop_w") int dop_w,
                                    @PathVariable("dop_h") int dop_h,
                                    @PathVariable("max_h") int max_h) throws IOException, ExecutionException, InterruptedException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        File file = new File(tmpdir+"/"+name);
        VisualResult_DTO result = calculatorService.calculateAndGetUploadFileFirst(file, user_key, dop_l, dop_w, dop_h, max_h);
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .excludeFieldsWithModifiers(0x00000080) // STATIC|TRANSIENT in the default configuration
                .create();
        String json = gson.toJson(result);
        return ResponseEntity.status(HttpStatus.OK).body(json);
    }

    /**
     * Скачивает файл по имени из временной директории.
     * Возвращает файл как потоковое тело ответа.
     *
     * @param name имя файла для скачивания
     * @return ResponseEntity содержащий файл как потоковое тело ответа
     */
    @GetMapping(value = "/download/{name}")
    public @ResponseBody
    ResponseEntity<StreamingResponseBody> getFile(@PathVariable("name") String name) {
            StreamingResponseBody streamingResponseBody = outputStream -> {
            String tmpdir = System.getProperty("java.io.tmpdir");
            File file = new File(tmpdir+"/"+name);
            IOUtils.copy(new FileInputStream(file), outputStream);
        };
        String encodedFilename = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        // Используем только filename* для поддержки UTF-8, чтобы избежать проблем с кириллицей в Tomcat
        String contentDisposition = String.format("attachment; filename*=UTF-8''%s", encodedFilename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(streamingResponseBody);
    }

}
