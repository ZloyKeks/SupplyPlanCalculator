package by.legan.gt_tss.supplyplancalculator.servvice;

import by.legan.gt_tss.supplyplancalculator.Data.Result;
import by.legan.gt_tss.supplyplancalculator.Data.VisualResult_DTO;
import by.legan.gt_tss.supplyplancalculator.webSocket.StatusProcessMessageDTO;
import by.legan.gt_tss.supplyplancalculator.webSocket.WebSocketEndPointsEnum;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.concurrent.ExecutionException;

/**
 * Класс для расчета и загрузки данных.
 *
 * @author [Имя авторов]
 */
@Service
@Slf4j
public class CalculatorService {

    /**
     * Аутенцируемый метод для получения объекта ExcelSupplyPlanUtils.
     */
    @Autowired
    private ExcelSupplyPlanUtils excelSupplyPlanUtils;

    /**
     * Аутенцируемый метод для получения объекта SupplyPlanCalculatorUtil.
     */
    @Autowired
    private SupplyPlanCalculatorUtil supplyPlanCalculatorUtil;

    /**
     * Аутенцируемый метод для получения объекта SimpMessageSendingOperations.
     */
    @Autowired
    private SimpMessageSendingOperations simpMessageSendingOperations;

    /**
     * Метод для расчета и загрузки данных.
     *
     * @param file        Файл с данными.
     * @param user_key    Ключ пользователя.
     * @param dop_l       Количество дней.
     * @param dop_w       Количество недель.
     * @param dop_h       Количество часов.
     * @param max_h       Максимальное количество часов.
     * @return Результат расчета в виде VisualResult_DTO.
     * @throws IOException           Нарушение доступа к файлу.
     * @throws ExecutionException     Важная ошибка в выполнении.
     * @throws InterruptedException    Остановка Threads.
     */
    public VisualResult_DTO calculateAndGetUploadFile(File file, String user_key, int dop_l, int dop_w, int dop_h, int max_h) throws IOException, ExecutionException, InterruptedException {
        Result result = supplyPlanCalculatorUtil.calculate(new FileInputStream(file), user_key, dop_l, dop_w, dop_h, max_h);
        return getVisualResult_dto(file, user_key, result);
    }

    /**
     * Метод для расчета и загрузки данных с использованием первого метода.
     *
     * @param file        Файл с данными.
     * @param user_key    Ключ пользователя.
     * @param dop_l       Количество дней.
     * @param dop_w       Количество недель.
     * @param dop_h       Количество часов.
     * @param max_h       Максимальное количество часов.
     * @return Результат расчета в виде VisualResult_DTO.
     * @throws IOException           Нарушение доступа к файлу.
     * @throws ExecutionException     Важная ошибка в выполнении.
     * @throws InterruptedException    Остановка Threads.
     */
    public VisualResult_DTO calculateAndGetUploadFileFirst(File file, String user_key, int dop_l, int dop_w, int dop_h, int max_h) throws IOException, ExecutionException, InterruptedException {
        Result result = supplyPlanCalculatorUtil.firstCalculate(new FileInputStream(file), user_key, dop_l, dop_w, dop_h, max_h);
        return getVisualResult_dto(file, user_key, result);
    }

    /**
     * Метод для преобразования результата в VisualResult_DTO.
     *
     * @param file        Файл с данными.
     * @param user_key    Ключ пользователя.
     * @param result      Результат расчета.
     * @return Результат расчета в виде VisualResult_DTO.
     * @throws IOException           Нарушение доступа к файлу.
     */
    private VisualResult_DTO getVisualResult_dto(File file, String user_key, Result result) throws IOException {
        File result_file = File.createTempFile("result", ".xlsx");
        excelSupplyPlanUtils.exportResult(new FileInputStream(file), new FileOutputStream(result_file), result, user_key);
        result.setResultFileName(result_file.getName());
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .excludeFieldsWithModifiers(0x00000080) // STATIC|TRANSIENT в default конфигурации
                .create();
        String json = gson.toJson(result.toVisualResult_DTO(), VisualResult_DTO.class);
        simpMessageSendingOperations.convertAndSend(WebSocketEndPointsEnum.CALC.getEndPoint(),
                new StatusProcessMessageDTO(user_key, "FileWriteFinish", "Finish", json));
        return result.toVisualResult_DTO();
    }

}
