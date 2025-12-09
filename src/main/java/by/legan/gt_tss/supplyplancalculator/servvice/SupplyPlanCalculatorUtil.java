package by.legan.gt_tss.supplyplancalculator.servvice;

import by.legan.gt_tss.supplyplancalculator.Data.*;
import by.legan.gt_tss.supplyplancalculator.configuration.MainConfig;
import by.legan.gt_tss.supplyplancalculator.webSocket.StatusProcessMessageDTO;
import by.legan.gt_tss.supplyplancalculator.webSocket.WebSocketEndPointsEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.skjolber.packing.api.*;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import by.legan.gt_tss.supplyplancalculator.visualization.ContainerProjection;
import by.legan.gt_tss.supplyplancalculator.visualization.PackagingVisualization;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SupplyPlanCalculatorUtil {

    @Autowired
    MainConfig config;

    @Autowired
    ExcelSupplyPlanUtils utils;

    @Autowired
    SimpMessageSendingOperations simpMessageSendingOperations;

    /**
     * Распознание начальных данных, и расчёт планов поставки с учётом оптимальной загрузки контейнеров, сроков поставки и других параметров
     * @param document - спаршеный документ
     * @param user_key - ключ для вещаний в UI пользователя через WebSocket
     * @return результат содержащий модифицированный документ и карту (продавец/результат_расчёта) - отражающую все заказы необходимые для выхода на оптимальный результат
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private Result calculateExec(SupplyPlanDocument document, String user_key, int dop_l, int dop_w, int dop_h, int max_h) throws IOException, ExecutionException, InterruptedException {
//        SupplyPlanDocument document = utils.parse(inputStream, user_key, dop_l, dop_w, dop_h, max_h);
        if (document.getItems() == null || document.getItems().size() == 0) return null;
        int mount_count = document.getItems().stream().filter(Objects::nonNull).findAny().get().getMountPlans().size();
        List<String> sellers = getAllSellers(document);
        Map<String, List<ResultCalculate>> resultMapSellersCalculateMount = new HashMap<>();
        List<CompletableFuture<List<ResultCalculate>>> completableFutureList = new ArrayList<>();
        String json = new Gson().toJson(sellers);
        simpMessageSendingOperations.convertAndSend(WebSocketEndPointsEnum.CALC.getEndPoint(),
                new StatusProcessMessageDTO(user_key, "StartCalculate","SellersList", json));
        for (String seller : sellers) {
            Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            CompletableFuture<List<ResultCalculate>> greetingFuture = CompletableFuture.supplyAsync(() -> {
                List<ResultCalculate> resultOfMounts = new ArrayList<>();
                for (int mount = 0; mount <= mount_count-1; mount++) {
                    ResultCalculate resultCalculate = calculateMount(user_key, document,seller, mount);
                    resultOfMounts.add(resultCalculate);
                    double percent = ((double) mount / (double) mount_count) * 100;
                    String percent_json = new Gson().toJson(new PercentProgressDTO(percent));
                    simpMessageSendingOperations.convertAndSend(WebSocketEndPointsEnum.CALC.getEndPoint(),
                            new StatusProcessMessageDTO(user_key,"ProgressCalculate",seller, percent_json));
                }
                log.info("Остатки товаров на последний месяц");
                printPlanned_balance(document, seller, mount_count-1);
                if (config.isWriteVisualisationToDisk()) writeVisualisationToDisk(resultOfMounts, seller);
                return resultOfMounts;
            }, executor);
            completableFutureList.add(greetingFuture);
        }
        // Создаём комбинированный Future, используя allOf()
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                completableFutureList.toArray(new CompletableFuture[0]));
        CompletableFuture<List<Object>> allPageContentsFuture = allFutures.thenApply(v -> completableFutureList.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));

        List<Object> result = allPageContentsFuture.get();
        for (Object resultCalculate : result){
            List<ResultCalculate> resultCalc = (List<ResultCalculate>) resultCalculate;
            String seller_result = "none";
            //TODO Тут может быть null если ваще ни хуя не сформировано
            try {
                if (resultCalc.size() > 0) seller_result = resultCalc.stream().filter(Objects::nonNull).findFirst().get().getSeller();
                resultMapSellersCalculateMount.put(seller_result,resultCalc);
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
        log.info("--------------------------------------------------------------------");
        Result finishResult = new Result();
        finishResult.setDocument(document);
        finishResult.setResultCalculateMap(resultMapSellersCalculateMount);
        return finishResult;
    }

    /**
     * Выполняет первый расчет используя пробный метод парсинга.
     * Это более быстрый метод расчета для первоначального тестирования.
     *
     * @param inputStream входной поток содержащий Excel документ
     * @param user_key ключ пользователя для WebSocket связи
     * @param dop_l дополнительное измерение длины
     * @param dop_w дополнительное измерение ширины
     * @param dop_h дополнительное измерение высоты
     * @param max_h максимальное ограничение высоты
     * @return результат расчета
     * @throws IOException если произошла ошибка ввода-вывода
     * @throws ExecutionException если произошла ошибка при выполнении
     * @throws InterruptedException если операция была прервана
     */
    public Result firstCalculate(InputStream inputStream, String user_key, int dop_l, int dop_w, int dop_h, int max_h) throws IOException, ExecutionException, InterruptedException {
        SupplyPlanDocument document = utils.parseFirst(inputStream, user_key, dop_l, dop_w, dop_h, max_h);
        return calculateExec(document, user_key, dop_l, dop_w, dop_h, max_h);
    }

    /**
     * Выполняет полный расчет плана поставок с оптимальной загрузкой контейнеров.
     *
     * @param inputStream входной поток содержащий Excel документ
     * @param user_key ключ пользователя для WebSocket связи
     * @param dop_l дополнительное измерение длины
     * @param dop_w дополнительное измерение ширины
     * @param dop_h дополнительное измерение высоты
     * @param max_h максимальное ограничение высоты
     * @return результат расчета содержащий оптимизированные планы поставок
     * @throws IOException если произошла ошибка ввода-вывода
     * @throws ExecutionException если произошла ошибка при выполнении
     * @throws InterruptedException если операция была прервана
     */
    public Result calculate(InputStream inputStream, String user_key, int dop_l, int dop_w, int dop_h, int max_h) throws IOException, ExecutionException, InterruptedException {
        SupplyPlanDocument document = utils.parse(inputStream, user_key, dop_l, dop_w, dop_h, max_h);
        return calculateExec(document, user_key, dop_l, dop_w, dop_h, max_h);
    }

    /**
     * Полный расчёт плана на указанный месяц с учётом загруженности контейнеров, габаритов, массы, стоимости
     * @param document - Набор данных полученный от пользователя
     * @param seller - поставщик
     * @param mount_start - месяц, который считаем
     * @return Списки заполненных контейнеров, товаров, и ModificationPlan
     */
    private ResultCalculate calculateMount(String user_key, SupplyPlanDocument document, String seller, int mount_start){
        log.info("Calculate Seller : " + seller + " mount_start " + mount_start);
        ResultCalculate resultCalculate = null;
        try {
            List<Item> items = getItemFromSeller(document,seller); // Получаем все товары поставщика
            List<Item> deficitOfMount = searchDeficitOfMountAnd(items, mount_start); // Получаем товары с дефицитом за указанный месяц
            List<Item> itemRequestAccept = deficitOfMount.stream().filter(item -> checkRequestAccept(item,mount_start)).collect(Collectors.toList()); // Получаем только те н которые можно подать заявку
            Dimension dimensions = items.get(0).getDimensions_container();
            int maxWeight = (int) Math.round(items.get(0).getContainer_lifting());
            resultCalculate = packToContainer(itemRequestAccept, mount_start, getBoxItem(itemRequestAccept, mount_start),dimensions, maxWeight, 0, -1);
            if (resultCalculate == null) return null;
            log.info("Основная загрузка");
            printContainerLoad(resultCalculate);
            // ДОУПАКОВКА
            resultCalculate.setSeller(seller);
            items = applyResultCalculated(items, resultCalculate);
            dopCalculate(user_key, resultCalculate,mount_start,items);
            log.info("Загрузка после дозабивки");
            resultCalculate.setSeller(seller);
            printContainerLoad(resultCalculate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultCalculate;
    }

    /**
     * Выполняет дополнительный расчет для заполнения контейнеров до максимальной вместимости.
     * Учитывает габариты, цену и массу.
     *
     * @param user_key ключ пользователя для WebSocket связи
     * @param resultCalculate результат основного расчета загрузки
     * @param mount_start месяц для расчета
     * @param items набор товаров и поставщиков из таблицы предоставленной пользователем
     * @return обновленный список товаров с учетом дополнительного заполнения; ResultCalculate также обновляется результатами дополнительного заполнения
     */
    private List<Item> dopCalculate(String user_key, ResultCalculate resultCalculate, int mount_start, List<Item> items){
        items = dobCalculateStart(user_key, resultCalculate, mount_start, items);
        return applyResultCalculated(items,resultCalculate);
    }

    /**
     * Запускает процесс дополнительного расчета для заполнения контейнеров.
     *
     * @param user_key ключ пользователя для WebSocket связи
     * @param resultCalculate результат основного расчета
     * @param mount_start начальный индекс месяца
     * @param items список товаров для обработки
     * @return обновленный список товаров после дополнительных расчетов
     */
    private List<Item> dobCalculateStart(String user_key, ResultCalculate resultCalculate, int mount_start, List<Item> items) {
        int mount_end = items.get(0).getMountPlans().size();
        for (int mount = mount_start; mount < mount_end; mount++){
            dopCalculateToMount(user_key,resultCalculate,mount_start,mount,items);
            applyResultCalculated(items,resultCalculate);
        }
        return items;
    }

    /**
     * Выполняет дополнительный расчет для заполнения контейнеров до определенного месяца.
     *
     * @param user_key ключ пользователя для WebSocket связи
     * @param resultCalculate результат основного расчета
     * @param mount_start начальный индекс месяца
     * @param mount_end конечный индекс месяца
     * @param items список товаров для обработки
     * @return обновленный список товаров после дополнительных расчетов
     */
    private List<Item> dopCalculateToMount(String user_key, ResultCalculate resultCalculate, int mount_start,int mount_end, List<Item> items){
        log.info("Доукомплектование, до " + items.get(0).getMountPlans().get(mount_end).getName());
        Dimension dimensions = items.get(0).getDimensions_container();
        int maxWeight = (int) Math.round(items.get(0).getContainer_lifting());
        List<Item> deficitOfMount = searchDeficitOfMountAnd(items, mount_end); // Получаем товары с дефицитом за указанный месяц
        List<Item> itemRequestAccept = new ArrayList<>(deficitOfMount.stream().filter(item -> checkRequestAccept(item, mount_start)).collect(Collectors.toList()));
        if (itemRequestAccept.size() > 0) {
            //TODO Не учитывается количество дефицита, по этому добивается до полного контейнера
            // из-за этого возникают профициты на последний месяц , иногда
            itemRequestAccept.sort(new PriorityComparator());
            List<BoxItem> boxItems = new ArrayList<>();
            boxItems.addAll(resultCalculate.getBoxItems());
            //TODO Тут возможно придётся вращать
            List<FullBoxItem> fullBoxNextMount = createBoxItemAndItemItem(itemRequestAccept, mount_end);
            int starDopBoxCount = fullBoxNextMount.size();
            double prePercent = 0;
            ResultCalculate finish_dop_result = new ResultCalculate();
            setValueFinishResult(finish_dop_result, finish_dop_result, true);
            do {
                double percent = 100 - (((double) fullBoxNextMount.size() / (double) starDopBoxCount) * 100);
                if (percent - prePercent > 1) {
                    log.info("Прогресс дозабивки : " + percent + "%");
                    String percent_json = new Gson().toJson(new PercentProgressDTO(percent));
                    simpMessageSendingOperations.convertAndSend(WebSocketEndPointsEnum.CALC.getEndPoint(),
                            new StatusProcessMessageDTO(user_key, "ProgressDopCalculate", resultCalculate.getSeller(), percent_json));
                    prePercent = percent;
                }
                //TODO Это нужно выносить в настройки, размер пачки влияет на проценты дозабивки
                int dop_pack_size = 1; //Math.round(fullBoxNextMount.size()/50);
//                    if (dop_pack_size <=0) dop_pack_size = 1;
                List<FullBoxItem> dop_fullBox = givePackBox(fullBoxNextMount, dop_pack_size);
                List<BoxItem> dod_box = dop_fullBox.stream().map(FullBoxItem::getBoxItem).collect(Collectors.toList());
                List<BoxItem> temp_boxItem = new ArrayList<>(boxItems);
                temp_boxItem.addAll(dod_box);
                List<Item> addItems = dop_fullBox.stream().map(FullBoxItem::getItem).collect(Collectors.toList());

                ResultCalculate dop_result = packToContainer(addItems, mount_start, temp_boxItem, dimensions, maxWeight, resultCalculate.getContainersCount(), 1);
                if (dop_result != null) {
/*                        System.out.println("------------000000---------");
                    for (Container container : finish_dop_result.getPack()) {
                        // В версии 4.x используем getStack().getVolume() вместо getUsedSpace().getVolume()
                        double dop_percent = ((double) container.getStack().getVolume() / (double) container.getVolume()) * 100;
                        System.out.println("Percent load" + dop_percent);
                    }
                    System.out.println("--------------000000-------");*/
                    setValueFinishResult(finish_dop_result, dop_result, true);
                    boxItems = temp_boxItem;
                    removeFirsPack(fullBoxNextMount, dop_pack_size);
                } else {
                    //TODO Не понятно почему но затирается значение finish_dop_result, приходится пересчитывать с предыдущим значением боксов
                    dop_result = packToContainer(addItems, mount_start, boxItems, dimensions, maxWeight, resultCalculate.getContainersCount(), 1);
                    if (dop_result != null) setValueFinishResult(finish_dop_result, dop_result, false);
                    if (dop_fullBox.size() > 0) {
                        log.info("COUNT " + fullBoxNextMount.size() + " | REMOVE ALL :" + dop_fullBox.get(0).getItem().getName());
                        removeAllBoxItems(fullBoxNextMount, dop_fullBox.get(0).getItem().getName());
                        log.info("AFTER REMOVE COUNT " + fullBoxNextMount.size());
                    }
                }
            } while (itemRequestAccept.size() > 0 && fullBoxNextMount.size() > 0);
            if (finish_dop_result.getContainersCount() > 0) {
                resultCalculate.setPack(finish_dop_result.getPack());
                resultCalculate.setBoxItems(finish_dop_result.getBoxItems());
                resultCalculate.uniteModificationPlan(finish_dop_result.getModificationPlan());
            }
            items = applyResultCalculated(items, resultCalculate);
        }
        return items;
    }

    /**
     * Устанавливает значения финального результата из результата дополнительного расчета.
     *
     * @param finish_dop_result объект результата для обновления
     * @param dop_result результат дополнительного расчета для копирования
     * @param addModificationPlan добавлять ли планы модификации в результат
     */
    private void setValueFinishResult(ResultCalculate finish_dop_result, ResultCalculate dop_result, boolean addModificationPlan) {
        try {
            finish_dop_result.setContainersCount(dop_result.getContainersCount());
            finish_dop_result.setBoxItems(new ArrayList<>());
            finish_dop_result.getBoxItems().addAll(dop_result.getBoxItems());
            finish_dop_result.setPack(new ArrayList<>());
            finish_dop_result.getPack().addAll(dop_result.getPack());
            if (addModificationPlan) finish_dop_result.uniteModificationPlan(dop_result.getModificationPlan());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Пробует упаковать набор BoxItem в контейнер(ы)
     * @param itemRequestAccept - набор данных прошедший фильтрацию на доступность к заказу
     * @param mount - месяц
     * @param boxList - набор 3D боксов
     * @param dimensions - размер контейнера
     * @param maxWeight - максимальная масса
     * @param maxContainers - максимальное количество контейнеров
     * @param set_count - значение New-Supply_Plan которое задать, -1 для задания посчитанного значения
     * @return Списки заполненных контейнеров, товаров, и ModificationPlan
     */
    public ResultCalculate packToContainer(List<Item> itemRequestAccept, int mount, List<BoxItem> boxList, Dimension dimensions, int maxWeight, int maxContainers, int set_count){
        List<Container> pack = packToManyContainers("Container : " + mount, dimensions, maxWeight, boxList, maxContainers);
        ResultCalculate resultCalculate = new ResultCalculate();
        if (pack == null) return null;
        resultCalculate.setContainersCount(pack.size());
        resultCalculate.setPack(pack);
        resultCalculate.setBoxItems(boxList);
        for (Item item : itemRequestAccept){
            MountPlan plan = item.getMountPlans().get(mount);
            ModificationPlan modificationPlan = new ModificationPlan();
            modificationPlan.setItem(item);
            modificationPlan.setMount(mount);
            modificationPlan.setValue_New_Supply_plan((double) set_count);
            if (set_count == -1) modificationPlan.setValue_New_Supply_plan(plan.getPlanned_balance()*-1);
            resultCalculate.getModificationPlan().add(modificationPlan);
        }
        return resultCalculate;
    }

    /**
     * Запись в фалы каждого контейнера в результате, для последующей визуализации
     * @param resultOfMounts
     * @param seller
     */
    public void writeVisualisationToDisk(List<ResultCalculate> resultOfMounts, String seller) {
        resultOfMounts.stream().forEach(resultCalculate -> {
            if (resultCalculate != null && resultCalculate.getModificationPlan() != null) {
                printContainerLoad(resultCalculate);
                try {
                    String json = packagingVisualization(resultCalculate.getPack());
                    String prefix = seller + "_containers_mount_"+resultCalculate.getModificationPlan().get(0).getMount()+"_";
                    prefix = replaceNotSupportChar(prefix, "+", "=", "[","]",":",";","«",",",".","/","?");
                    String suffix = ".json";
                    File file = File.createTempFile(prefix,suffix);
                    resultCalculate.setVisual3DModelFileName(file.getName());
                    FileUtils.writeStringToFile(file, json);
                } catch (JsonProcessingException e) {

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Заменяет неподдерживаемые символы в строке на пустые строки.
     *
     * @param str строка для обработки
     * @param subols переменные аргументы последовательностей символов для удаления
     * @return строка с удаленными указанными символами
     */
    private String replaceNotSupportChar(String str, String ...subols){
        for (String c : subols) {
            str = str.replace(c, "");
        }
        return str;
    }

    /**
     * Возвращает пачку коробок с начала списка, гарантируя что все коробки принадлежат одному товару.
     *
     * @param boxes список полных элементов коробок
     * @param count количество коробок для возврата
     * @return список полных элементов коробок одного товара
     */
    private List<FullBoxItem> givePackBox(List<FullBoxItem> boxes, int count){
        Item firstItem = null;
        if (boxes != null && boxes.size() > 0) firstItem = boxes.get(0).getItem();
        if (boxes.size() < count) count = boxes.size();
        List<FullBoxItem> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (firstItem.equals(boxes.get(0).getItem())) result.add(boxes.get(i)); else return result;
        }
        return result;
    }

    /**
     * Удаляет первую пачку коробок из списка.
     *
     * @param boxes список полных элементов коробок
     * @param count количество коробок для удаления
     * @return измененный список с удаленными коробками
     */
    private List<FullBoxItem> removeFirsPack(List<FullBoxItem> boxes, int count){
        if (boxes == null || boxes.size() == 0) return boxes;
        if (boxes.size() < count) count = boxes.size();
        for (int i = 0; i < count; i++) {
            boxes.remove(0);
        }
        return boxes;
    }

    /**
     * Находит товар в списке по сравнению на равенство.
     *
     * @param items список товаров для поиска
     * @param findItem товар для поиска
     * @return найденный товар, или null если не найден
     */
    private Item findByItem(List<Item> items, Item findItem) {
        return items.stream().filter(item -> item.equals(findItem)).findAny().orElse(null);
    }

    /**
     * Удаляет все элементы коробок из списка, которые соответствуют заданному имени товара.
     *
     * @param list список полных элементов коробок
     * @param name имя товара для удаления
     */
    private void removeAllBoxItems(List<FullBoxItem> list, String name){
        list.removeIf(item -> item.getItem().getName().equals(name));
    }


    /**
     * Создает список элементов коробок из списка товаров для определенного месяца.
     *
     * @param items список товаров
     * @param mount индекс месяца
     * @return список элементов коробок созданных из товаров
     */
    private List<BoxItem> getBoxItem(List<Item> items, int mount){
        List<BoxItem> boxList = createListBoxFomListItem(items, mount);
        return boxList;
    }

    /**
     * Применяет результаты расчета к списку товаров полученных от пользователя.
     * Обновляет значения New_Supply_plan на основе планов модификации.
     *
     * @param items список товаров для обновления
     * @param resultCalculate результат расчета содержащий планы модификации
     * @return обновленный список товаров
     */
    private List<Item> applyResultCalculated(List<Item> items, ResultCalculate resultCalculate){
        if (resultCalculate == null) return items;
        for (ModificationPlan mod_plan : resultCalculate.getModificationPlan()) {
            int resultMount = mod_plan.getMount();
            Item item = findByItem(items,mod_plan.getItem());
            MountPlan previous_mount = item.getMountPlans().get(resultMount-1);
            previous_mount.setNew_Supply_plan(mod_plan.getValue_New_Supply_plan());
        }
        return items;
    }

    /**
     * Логирует запланированный баланс для всех товаров конкретного поставщика за указанный месяц.
     *
     * @param document документ плана поставок
     * @param seller имя поставщика
     * @param mount индекс месяца
     */
    private void printPlanned_balance(SupplyPlanDocument document, String seller, int mount){
        try {
            List<Item> result = getItemFromSeller(document,seller); // Получаем все товары поставщика
            for (Item item : result) {
                log.info(item.getName() + " | count = " + item.getMountPlans().get(mount).getPlanned_balance());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Проверяет присутствует ли строка в списке.
     *
     * @param str строка для проверки
     * @param list список для поиска
     * @return true если строка найдена в списке, false в противном случае
     */
    private boolean checkPreset(String str, List<String> list){
        for (String item : list) if (item.equals(str)) return true;
        return false;
    }

    /**
     * Получает список всех уникальных поставщиков из документа.
     *
     * @param document документ плана поставок
     * @return список уникальных имен поставщиков
     */
    private List<String> getAllSellers(SupplyPlanDocument document){
        List<String> sellers = new ArrayList<>();
        for (Item item : document.getItems()) {
            if (item != null)
                if (!checkPreset(item.getSeller(),sellers)) sellers.add(item.getSeller());
        }
        return sellers;
    }

    /**
     * Получает все товары для конкретного поставщика из документа.
     *
     * @param document документ плана поставок
     * @param seller имя поставщика
     * @return список товаров принадлежащих поставщику
     */
    private List<Item> getItemFromSeller(SupplyPlanDocument document, String seller){
        List<Item> items = new ArrayList<>();
        for (Item item : document.getItems()){
            if (item != null)
                if (item.getSeller().equals(seller)) items.add(item);
        }
        return items;
    }

    /**
     * Компаратор для сортировки товаров по приоритету для загрузки контейнеров.
     * Товары сортируются по соотношению цена-объем, затем по соотношению цена-вес.
     */
    private class PriorityComparator implements Comparator<Item> {
        /**
         * Сравнивает два товара на основе их приоритета для загрузки контейнеров.
         * Приоритет определяется соотношением цена-объем, затем соотношением цена-вес.
         *
         * @param o1 первый товар для сравнения
         * @param o2 второй товар для сравнения
         * @return отрицательное целое число если o1 имеет больший приоритет, ноль если равны, положительное если o2 имеет больший приоритет
         */
        @Override
        public int compare(Item o1, Item o2) {
            int result = 0;
            int volume_o1 = 0;
            int volume_o2 = 0;
            try {
                volume_o1 = (o1.getDimensions_item().getWidth()*o1.getDimensions_item().getHeight())*o1.getDimensions_item().getDepth();
                volume_o2 = (o2.getDimensions_item().getWidth()*o2.getDimensions_item().getHeight())*o2.getDimensions_item().getDepth();
            } catch (Exception e) {
                //TODO На это нужно реагировать
            }
            double sizeToPrice_o1 = 0;
            double sizeToPrice_o2 = 0;
            try {
                sizeToPrice_o1 = o1.getPrice() / volume_o1;
                sizeToPrice_o2 = o2.getPrice() / volume_o2;
            } catch (Exception e) {
                //TODO На это нужно реагировать
            }
            if (sizeToPrice_o1 > sizeToPrice_o2) result = -1;
            if (sizeToPrice_o1 < sizeToPrice_o2) result = 1;
            if (sizeToPrice_o1 == sizeToPrice_o2) {
                double weightToPrice_o1 = o1.getPrice() / o1.getWeight();
                double weightToPrice_o2 = o2.getPrice() / o2.getWeight();
                if (weightToPrice_o1 < weightToPrice_o2) result = 1;
                if (weightToPrice_o1 > weightToPrice_o2) result = -1;
            }
            return result;
        }
    }

    /**
     * Проверяет можно ли подать заявку на товар в конкретном месяце.
     * Учитывает срок поставки плюс один месяц буфера.
     *
     * @param item товар для проверки
     * @param number_mount индекс месяца
     * @return true если можно подать заявку, false в противном случае
     */
    private boolean checkRequestAccept(Item item, int number_mount){
        double result = Math.floor((number_mount) - (Math.floor(item.getDelivery_time() / 30)+1));
        return (result >= 0);
    }

    /**
     * Ищет товары с дефицитом (отрицательный запланированный баланс) в конкретном месяце.
     *
     * @param items список товаров для проверки
     * @param number_mount индекс месяца
     * @return список товаров с дефицитом в указанном месяце
     */
    private List<Item> searchDeficitOfMountAnd(List<Item> items, int number_mount){
        List<Item> result = new ArrayList<>();
        for (Item item : items) if (item.getMountPlans().get(number_mount).getPlanned_balance() < 0) {
            result.add(item);
        }
        return result;
    }

    /**
     * Упаковывает товары в один контейнер используя алгоритм Largest Area Fit First.
     *
     * @param containerName имя контейнера
     * @param dimensions_container размеры контейнера
     * @param maxWeight максимальная грузоподъемность контейнера
     * @param products список элементов коробок для упаковки
     * @return упакованный контейнер, или null если упаковка не удалась
     */
    private Container packToOneContainer(String containerName, Dimension dimensions_container, int maxWeight , List<BoxItem> products) {
        //TODO Максимальную массу и размеры нужно брать из UI
        Container container = Container.newBuilder()
                .withDescription(containerName)
                .withSize(dimensions_container.getWidth(), 
                         dimensions_container.getDepth(), 
                         dimensions_container.getHeight())
                .withEmptyWeight(1)
                .withMaxLoadWeight(maxWeight)
                .build();
        
        List<ContainerItem> containerItems = ContainerItem
                .newListBuilder()
                .withContainer(container)
                .build();
        
        // В старой версии использовались параметры: (containers, false, true, true, 1)
        // false = rotate2D, true = rotate3D, true = ? , 1 = threads
        // В новой версии нужно настроить builder аналогично
        LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager
                .newBuilder()
                .withRotate3D()  // rotate3D = true (второй параметр true)
                // rotate2D = false (первый параметр false) - по умолчанию
                .build();
        
        PackagerResult result = packager
                .newResultBuilder()
                .withContainerItems(containerItems)
                .withBoxItems(products)
                .build();
        
        if (!result.isSuccess()) {
            return null;
        }
        
        return result.get(0);
    }

    /**
     * Упаковывает товары в несколько контейнеров используя алгоритм Largest Area Fit First.
     *
     * @param prefix префикс для имен контейнеров
     * @param dimensions_container размеры контейнеров
     * @param maxWeight максимальная грузоподъемность каждого контейнера
     * @param products список элементов коробок для упаковки
     * @param maxContainers максимальное количество контейнеров для использования (0 означает неограниченно, по умолчанию 100)
     * @return список упакованных контейнеров
     */
    private List<Container> packToManyContainers(String prefix, Dimension dimensions_container, int maxWeight, List<BoxItem> products, int maxContainers){
        //TODO Это нужно вынести в настройки
        if (maxContainers == 0) maxContainers = 100;
        
        ContainerItem.Builder containerItemsBuilder = ContainerItem.newListBuilder();
        for (int i = 0; i < maxContainers; i++)
        {
            Container container = Container.newBuilder()
                    .withDescription(prefix + " " + i)
                    .withSize(dimensions_container.getWidth(), 
                             dimensions_container.getDepth(), 
                             dimensions_container.getHeight())
                    .withEmptyWeight(1)
                    .withMaxLoadWeight(maxWeight)
                    .build();
            containerItemsBuilder.withContainer(container);
        }
        List<ContainerItem> containerItems = containerItemsBuilder.build();
        
        // В старой версии использовались параметры: (containers, false, true, true, 0)
        // false = rotate2D, true = rotate3D, true = ? , 0 = threads
        LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager
                .newBuilder()
                .withRotate3D()  // rotate3D = true
                // rotate2D = false (по умолчанию)
                .build();
        
        PackagerResult result = packager
                .newResultBuilder()
                .withContainerItems(containerItems)
                .withBoxItems(products)
                .build();
        
        if (!result.isSuccess()) {
            return new ArrayList<>();
        }
        
        List<Container> packedContainers = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
            packedContainers.add(result.get(i));
        }
        return packedContainers;
    }

    /**
     * Преобразует список контейнеров в формат JSON для визуализации.
     *
     * @param containerList список контейнеров для визуализации
     * @return строковое представление контейнеров в формате JSON
     * @throws JsonProcessingException если обработка JSON не удалась
     */
    private String packagingVisualization(List<Container> containerList) throws JsonProcessingException {
        ContainerProjection projection = new ContainerProjection();
        PackagingVisualization visualization = projection.project(containerList);
        return visualization.toJson();
    }

    /**
     * Создает список элементов коробок из списка товаров для определенного месяца.
     * Количество коробок определяется запланированным балансом (дефицитом) для этого месяца.
     *
     * @param items список товаров
     * @param number_mount индекс месяца
     * @return список элементов коробок созданных из товаров
     */
    private List<BoxItem> createListBoxFomListItem(List<Item> items, int number_mount){
        List<BoxItem> boxes = new ArrayList<>();
        int all_count = 0;
        for (Item item : items) {
            int count = (int) Math.round(item.getMountPlans().get(number_mount).getPlanned_balance());
            if (count < 0) count = count * -1;
            if (count > 0) {
                // В версии 4.x BoxItem представляет группу одинаковых коробок
                // Создаем один BoxItem с количеством count
                Box box = Box.newBuilder()
                        .withId(item.getName())
                        .withSize(item.getDimensions_item().getWidth(), 
                                 item.getDimensions_item().getDepth(), 
                                 item.getDimensions_item().getHeight())
                        .withWeight((int) Math.round(item.getWeight()))
                        .withRotate3D()
                        .build();
                BoxItem boxItem = new BoxItem(box, count);
                boxes.add(boxItem);
                all_count = all_count + count;
            }
        }
        return boxes;
    }

    /**
     * Создает список полных элементов коробок из одного товара.
     *
     * @param item товар для создания коробок
     * @param count количество коробок для создания
     * @return список полных элементов коробок
     */
    private List<FullBoxItem> createBoxItemAndItem(Item item, int count){
        List<FullBoxItem> result = new ArrayList<>();
        Box box = Box.newBuilder()
                .withId(item.getName())
                .withSize(item.getDimensions_item().getWidth(), 
                         item.getDimensions_item().getDepth(), 
                         item.getDimensions_item().getHeight())
                .withWeight((int) Math.round(item.getWeight()))
                .withRotate3D()
                .build();
        // В версии 4.x BoxItem представляет группу одинаковых коробок
        // Создаем один BoxItem с количеством count для использования в упаковке
        // Но для совместимости со старым кодом создаем отдельные FullBoxItem
        BoxItem boxItem = new BoxItem(box, count);
        for (int i = 0; i < count; i++) {
            FullBoxItem boxItemAndItem = new FullBoxItem();
            boxItemAndItem.setBoxItem(boxItem);
            boxItemAndItem.setItem(item);
            result.add(boxItemAndItem);
        }
        return result;
    }

    /**
     * Создает список полных элементов коробок из нескольких товаров для определенного месяца.
     *
     * @param items список товаров
     * @param number_mount индекс месяца
     * @return список полных элементов коробок созданных из товаров
     */
    private List<FullBoxItem> createBoxItemAndItemItem(List<Item> items, int number_mount){
        List<FullBoxItem> boxes = new ArrayList<>();
        int all_count = 0;
        for (Item item : items) {
            int count = (int) Math.round(item.getMountPlans().get(number_mount).getPlanned_balance());
            if (count < 0) count = count * -1;
            List<FullBoxItem> result = createBoxItemAndItem(item,count);
            boxes.addAll(result);
            all_count = all_count + result.size();
        }
        return boxes;
    }

    /**
     * Логирует информацию о загрузке контейнеров включая процент загрузки и вес.
     *
     * @param resultCalculate результат расчета содержащий информацию о контейнерах
     */
    private void printContainerLoad(ResultCalculate resultCalculate){
        if (resultCalculate != null && resultCalculate.getModificationPlan() != null) {
            log.info("Месяц №" + resultCalculate.getModificationPlan().get(0).getMount());
            log.info("Product Pack " + resultCalculate.getBoxItems().size());
            log.info("ModificationPlan Count : " + resultCalculate.getModificationPlan().size());
            log.info("Containers count : " + resultCalculate.getContainersCount());
            resultCalculate.getPack().stream().forEach(container -> {
                // В версии 4.x используем getStack().getVolume() вместо getUsedSpace().getVolume()
                double percent = ((double) container.getStack().getVolume() / (double) container.getVolume()) * 100;
                DecimalFormat f = new DecimalFormat("##.00");
                // В версии 4.x используем getStack().getWeight() для веса груза
                int cargoWeight = container.getStack().getWeight();
                log.info("Container load : " + f.format(percent) + "%" + " Загрузка контейнера : " + cargoWeight + "кг");
            });
        }
    }

}
