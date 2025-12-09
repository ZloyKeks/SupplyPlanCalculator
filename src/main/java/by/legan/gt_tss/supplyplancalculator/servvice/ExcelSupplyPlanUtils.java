package by.legan.gt_tss.supplyplancalculator.servvice;

import by.legan.gt_tss.supplyplancalculator.Data.*;
import by.legan.gt_tss.supplyplancalculator.configuration.MainConfig;
import by.legan.gt_tss.supplyplancalculator.webSocket.StatusProcessMessageDTO;
import by.legan.gt_tss.supplyplancalculator.webSocket.WebSocketEndPointsEnum;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExcelSupplyPlanUtils {

    @Autowired
    MainConfig config;

    @Autowired
    SimpMessageSendingOperations simpMessageSendingOperations;

    /**
     * Преобразует входные данные из Excel документа в Java Object
     * @param inputStream - стрим содержащий Excel документы
     * @return документ сл списком строк Item
     * @throws IOException
     */
    public SupplyPlanDocument parse(InputStream inputStream,String user_key, int dop_l, int dop_w, int dop_h, int max_h) throws IOException {
        //TODO Валидация не помешала бы
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet sheet = workbook.getSheetAt(0);
        Map<Integer,Map<Integer, XSSFCell>> original_doc = parseOriginal(sheet);
        List<Map<Integer, XSSFCell>> list = new ArrayList<>(original_doc.values());

        List<Item> items = new ArrayList<>();
        // items  = list.stream().map(cells -> parseItem(cells, list.get(0),original_doc.get(1),dop_l, dop_w, dop_h, max_h)).collect(Collectors.toList());

        int current = 0;
        for (Map<Integer, XSSFCell> cells : list) {
            current++;
            items.add(parseItem(cells, list.get(0),original_doc.get(1),dop_l, dop_w, dop_h, max_h));
            double percent = ((double) current / (double) list.size()) * 100;
            String percent_json = new Gson().toJson(new PercentProgressDTO(percent));
            simpMessageSendingOperations.convertAndSend(WebSocketEndPointsEnum.CALC.getEndPoint(),
                    new StatusProcessMessageDTO(user_key,"ProgressParse",user_key, percent_json));
        }

        SupplyPlanDocument document = new SupplyPlanDocument();
        document.setWorkbook(workbook);
        document.setItems(items);
        inputStream.close();
        return document;
    }

    /**
     * Парсит Excel документ для пробного заполнения контейнеров.
     * Это более быстрый метод парсинга используемый для первоначального тестирования.
     *
     * @param inputStream входной поток содержащий Excel документ
     * @param user_key ключ пользователя для WebSocket связи
     * @param dop_l дополнительное измерение длины
     * @param dop_w дополнительное измерение ширины
     * @param dop_h дополнительное измерение высоты
     * @param max_h максимальное ограничение высоты
     * @return распарсенный документ плана поставок
     * @throws IOException если произошла ошибка ввода-вывода
     */
    public SupplyPlanDocument parseFirst(InputStream inputStream, String user_key, int dop_l, int dop_w, int dop_h, int max_h) throws IOException{
        //TODO Валидация не помешала бы
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet sheet = workbook.getSheetAt(0);
        Map<Integer,Map<Integer, XSSFCell>> original_doc = parseOriginal(sheet);
        List<Map<Integer, XSSFCell>> list = new ArrayList<>(original_doc.values());
        List<Item> items = list.stream().map(cells -> firstParseItem(cells, list.get(0),original_doc.get(1),dop_l, dop_w, dop_h, max_h)).collect(Collectors.toList());
        SupplyPlanDocument document = new SupplyPlanDocument();
        document.setWorkbook(workbook);
        document.setItems(items);
        inputStream.close();
        return document;
    }

    /**
     * Парсит один товар из ячеек Excel для пробного расчета.
     *
     * @param cells карта индексов ячеек к ячейкам для текущей строки
     * @param tableHeaderRU карта русских заголовков таблицы
     * @param tableHeaderEN карта английских заголовков таблицы
     * @param dop_l дополнительное измерение длины
     * @param dop_w дополнительное измерение ширины
     * @param dop_h дополнительное измерение высоты
     * @param max_h максимальное ограничение высоты
     * @return распарсенный объект Item, или null если парсинг не удался
     */
    private Item firstParseItem(Map<Integer,XSSFCell> cells, Map<Integer,XSSFCell> tableHeaderRU, Map<Integer,XSSFCell> tableHeaderEN, int dop_l, int dop_w, int dop_h, int max_h){
        try {
            Item item = new Item();
            item.setArticle(getMapValue(cells,0));
            if (item.getArticle().equals("Артикул")) return null;
            item.setName(getMapValue(cells,1));
            item.setSeller("Быстрая проверка загрузки");
            item.setDelivery_time(0D);


            String[] dimensions_original_container = getMapValue(cells,2).replace("×","x").replace("х","x").replace(" ", "").split("x");
            if (dimensions_original_container.length == 3) {
                int length = Integer.parseInt(trunc(dimensions_original_container[0]));
                int width = Integer.parseInt(trunc(dimensions_original_container[1]));
                int height = Integer.parseInt(trunc(dimensions_original_container[2]));
                item.setDimensions_container(new Dimension("Dimensions_container",width,length,height));
            }

            // ДхШхВ длина ширина высота
            item.setContainer_lifting(getNumber(getMapValue(cells,3)));

            String[] dimensions_original = getMapValue(cells,4).replace("×","x").replace("х","x").replace(" ", "").split("x");
            if (dimensions_original.length == 3) {
                int length = Integer.parseInt(trunc(dimensions_original[0]))+dop_l;
                int width = Integer.parseInt(trunc(dimensions_original[1]))+dop_w;
                int height = Integer.parseInt(trunc(dimensions_original[2]))+dop_h;
                if (height >= max_h) height = item.getDimensions_container().getHeight();
                item.setDimensions_item(new Dimension("Dimensions_Item", width,length,height));
            }

            item.setType("");
            item.setWeight(getNumber(getMapValue(cells,5)));

            List<MountPlan> mountPlanList = new ArrayList<>();

            // Create First Mount
            MountPlan firstMountPlan = new MountPlan();
            firstMountPlan.setItem(item);
            firstMountPlan.setName("");
            firstMountPlan.setSupply_plan(0D);
            firstMountPlan.setNew_Supply_plan(0D);
            firstMountPlan.setSales_program(0D);
            firstMountPlan.setPlanned_balance(0D);
            item.setFirstMountPlanSales_plan_left(0D);

            MountPlan testMountPlan = new MountPlan();
            testMountPlan.setItem(item);
            testMountPlan.setName("Тестовый месяц");
            testMountPlan.setSupply_plan(0D);
            testMountPlan.setNew_Supply_plan(0D);
            testMountPlan.setSales_program(getNumber(getMapValue(cells,6)));
            testMountPlan.setPlanned_balance(0D);
            testMountPlan.setParent(firstMountPlan);
            item.setFirstMountPlanSales_plan_left(0D);


            mountPlanList.add(firstMountPlan);
            mountPlanList.add(testMountPlan);


            item.setMountPlans(mountPlanList);
            return item;
        } catch (NumberFormatException e) {
            return null;
        }
    }

        /**
         * Осуществляет экспорт в Excel файл результатов расчётов
         * @param inputStream - Оригинальный файл от пользователя
         * @param outputStream - Выходной файл с результатами
         * @param result - результаты
         * @throws IOException
         */
    public void exportResult(InputStream inputStream, OutputStream outputStream, Result result, String user_key) throws IOException {
        XSSFWorkbook book = new XSSFWorkbook(inputStream);
        SupplyPlanDocument document = result.getDocument();
        writeMainTable(book, document);
        List<InfoOfSeller> orders = calculateAllInfoFromOrders(result);
        writeRequestOfSellersNEW(orders,user_key, book);
        writeConsolidatedStatement(orders,user_key, book);
        book.write(outputStream);
        log.info("File Write finish");
        outputStream.close();
    }

    @Data
    @AllArgsConstructor
    private class ItemsFullInfoCount {
        private String name;
        private double count;
        private Item item;
    }

    @Data
    class ItemInfoOrder {
        private double count;
        private double price;
        private double weight;

        private String article; // Артикул
        private String name; // Название продукта
        private String seller; // Имя продавцы
        private Double Delivery_time; // Срок поставки в днях
        private Dimension dimension;

    }

    @Data
    class Order {
        private Container container;
        private String mount_request;
        private String mount_exist;
        private int number;
        private List<ItemInfoOrder> itemInfoOrders;
        private double totalCount;
        private double totalPrice;
        private double totalWeight;
        private double percent;
    }

    @Data
    class InfoOfSeller {
        private String sellerName;
        private List<Order> orders;
    }

    /**
     * Рассчитывает и возвращает информацию о заказах для каждого поставщика.
     * Обрабатывает результаты расчетов и организует их по поставщикам.
     *
     * @param result результат расчета содержащий информацию об упаковке контейнеров
     * @return список объектов InfoOfSeller содержащих детали заказов для каждого поставщика
     */
    public List<InfoOfSeller> calculateAllInfoFromOrders(Result result){
        List<InfoOfSeller> ordersOfSeller = new ArrayList<>();
        List<String> sellersList = new ArrayList<>(result.getResultCalculateMap().keySet());
        for (String seller : sellersList) {
            List<Order> orders = new ArrayList<>();
            List<ResultCalculate> listResultCalculatesOfMount = result.getResultCalculateMap().get(seller);
            int orderNumber = 1;
            for (ResultCalculate calculateOfMount : listResultCalculatesOfMount) {
                if (calculateOfMount != null && calculateOfMount.getModificationPlan().size() >0) {
                    for (Container container : calculateOfMount.getPack()) {
                        Order order = new Order();
                        List<Item> items = calculateOfMount.getModificationPlan().stream().map(modificationPlan -> modificationPlan.getItem()).collect(Collectors.toList());
                        Map<String, ItemsFullInfoCount> containerItems = getCountItemsFromContainer(container, items);
                        ModificationPlan plan = calculateOfMount.getModificationPlan().stream().findAny().get();
                        MountPlan plan_request = plan.getItem().getMountPlans().get(plan.getMount() - 1);
                        double mount_request = Math.floor((plan.getMount() - 1) - (Math.floor(plan_request.getItem().getDelivery_time() / 30)));
                        String mount_request_name = plan.getItem().getMountPlans().get((int) mount_request).getName();
                        // В версии 4.x используем getStack().getVolume() вместо getUsedSpace().getVolume()
                        double percent = ((double) container.getStack().getVolume() / (double) container.getVolume()) * 100;
                        order.mount_request = mount_request_name;
                        order.mount_exist = plan_request.getName();
                        order.percent = percent;
                        order.number = orderNumber++;
                        order.container = container;
                        double totalCount = 0;
                        double totalPrice = 0;
                        double totalWeight = 0;
                        order.itemInfoOrders = new ArrayList<>();
                        for (String nameItem : containerItems.keySet()) {
                            ItemsFullInfoCount itemsCount = containerItems.get(nameItem);
                            double count = itemsCount.count;
                            double price = itemsCount.getItem().getPrice() * itemsCount.getCount();
                            double weight = itemsCount.getItem().getWeight()* itemsCount.getCount();
                            ItemInfoOrder itemInfoOrder = new ItemInfoOrder();
                            itemInfoOrder.count = count;
                            itemInfoOrder.price = price;
                            itemInfoOrder.weight= weight;
                            itemInfoOrder.article = itemsCount.item.getArticle();
                            itemInfoOrder.name = itemsCount.getName();
                            itemInfoOrder.Delivery_time = itemsCount.item.getDelivery_time();
                            itemInfoOrder.dimension = itemsCount.item.getDimensions_item();

                            order.itemInfoOrders.add(itemInfoOrder);
                            totalCount = totalCount + count;
                            totalPrice = totalPrice + price;
                            totalWeight = totalWeight + weight;
                        }
                        order.totalCount = totalCount;
                        order.totalPrice = totalPrice;
                        order.totalWeight = totalWeight;
                        orders.add(order);
                    }
                }
            }
            InfoOfSeller info = new InfoOfSeller();
            info.sellerName = seller;
            info.orders = orders;
            ordersOfSeller.add(info);
        }
        return ordersOfSeller;
    }

    /**
     * Записывает заявки на заказы для каждого поставщика на отдельные листы в книге.
     *
     * @param infoOfSellers список информации о поставщиках с их заказами
     * @param user_key ключ пользователя для обновлений прогресса через WebSocket
     * @param book книга для записи
     */
    private void writeRequestOfSellersNEW(List<InfoOfSeller> infoOfSellers, String user_key, XSSFWorkbook book) {
        infoOfSellers.stream().forEach(info -> {
            XSSFSheet sheet = book.createSheet(WorkbookUtil.createSafeSheetName(info.sellerName));
            int width = (int) (50 * 1.14388) * 256;
            sheet.setColumnWidth(1, width);
            sheet.setColumnWidth(5, (int) (15 * 1.14388) * 256);
            sheet.setColumnWidth(6, (int) (15 * 1.14388) * 256);
            XSSFRow row = sheet.createRow(0);
            XSSFCell cell = row.createCell(0);
            cell.setCellType(CellType.STRING);
            cell.setCellValue(info.sellerName);
            final int[] rowNumber = {2};
            int request = 1;
            int current = 0;
            String pre_mount_request = null;
            String current_mount_request = null;
            for (Order order : info.orders) {
                current ++;
                current_mount_request = order.mount_exist;
                if (pre_mount_request == null) pre_mount_request = current_mount_request;
                if (!pre_mount_request.contains(current_mount_request)) {
                    pre_mount_request = current_mount_request;
                    createRow(sheet, rowNumber[0]++, "-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
                    double percent = ((double) current / (double) info.orders.size()) * 100;
                    String percent_json = new Gson().toJson(new PercentProgressDTO(percent));
                    simpMessageSendingOperations.convertAndSend(WebSocketEndPointsEnum.CALC.getEndPoint(),
                            new StatusProcessMessageDTO(user_key,"ProgressExportResult",user_key, percent_json));

                }
                if (order.getItemInfoOrders().size() > 0) {
                    createRow(sheet, rowNumber[0]++, "Заказ № " + request++ + " | Дата заказа : " + order.mount_request + "| Дата поступления : " + order.mount_exist);
                    DecimalFormat f = new DecimalFormat("##.00");
                    // В версии 4.x используем getStack().getWeight() для веса груза
                    int cargoWeight = order.container.getStack().getWeight();
                    createRow(sheet, rowNumber[0]++,"Загрузка контейнера : " + f.format(order.percent) + "%" + " Масса товара : " + cargoWeight + "кг");
                    createRow(sheet, rowNumber[0]++, "Артикул","Номенклатура","Количество","FOB", "Масса", "Размер", "Срок поставки");
                    createRow(sheet, rowNumber[0]++, "1.1","1. Product","","Purchase price", "weight");
                    order.itemInfoOrders.stream().forEach(itemInfoOrder -> {
                        createRow(sheet, rowNumber[0]++,
                                itemInfoOrder.getArticle(),
                                itemInfoOrder.getName(),
                                ""+ itemInfoOrder.count,
                                "" + itemInfoOrder.price,
                                "" + itemInfoOrder.weight,
                                itemInfoOrder.getDimension().getDepth()+
                                        "x"+ itemInfoOrder.getDimension().getWidth()+
                                        "x"+ itemInfoOrder.getDimension().getHeight(),
                                ""+ itemInfoOrder.getDelivery_time());
                    });
                    createRow(sheet, rowNumber[0]++, "","Всего :",""+order.totalCount, ""+order.totalPrice, ""+order.totalWeight);
                    current_mount_request = order.mount_exist;
                }

            }
        });
    }

    /**
     * Записывает сводную ведомость суммирующую все заказы от всех поставщиков.
     *
     * @param infoOfSellers список информации о поставщиках с их заказами
     * @param user_key ключ пользователя для WebSocket связи
     * @param book книга для записи
     */
    private void writeConsolidatedStatement(List<InfoOfSeller> infoOfSellers, String user_key, XSSFWorkbook book){
        XSSFSheet sheet = book.createSheet("Сводная ведомость");
        sheet.setColumnWidth(0, (int) (50 * 1.14388) * 256);
        sheet.setColumnWidth(1, (int) (10 * 1.14388) * 256);
        sheet.setColumnWidth(2, (int) (15 * 1.14388) * 256);
        sheet.setColumnWidth(3, (int) (15 * 1.14388) * 256);
        sheet.setColumnWidth(4, (int) (15 * 1.14388) * 256);
        XSSFRow row = sheet.createRow(0);
        XSSFCell cell = row.createCell(0);
        cell.setCellType(CellType.STRING);
        cell.setCellValue("Сводная ведомость");
        int rowNumber = 2;
        createRow(sheet,rowNumber++, "Основной поставщик","№ Заказа","Дата заказа","Дата поступления", "FOB итого");
        for (InfoOfSeller info : infoOfSellers) {
            for (Order order : info.orders) {
                createRow(sheet,rowNumber++, info.sellerName,""+order.number,order.mount_request,order.mount_exist,""+order.totalPrice);
            }
        }
    }


    /**
     * Подсчитывает товары в контейнере и возвращает карту имен товаров к их количествам.
     *
     * @param container контейнер для анализа
     * @param items список всех товаров
     * @return карта имен товаров к объектам ItemsFullInfoCount
     */
    private Map<String, ItemsFullInfoCount> getCountItemsFromContainer(Container container, List<Item> items){
        Map<String, ItemsFullInfoCount> mapItemsCount = new HashMap<>();
        // В версии 4.x Container использует getStack() вместо getLevels()
        Stack stack = container.getStack();
        for (Placement placement : stack) {
                // В версии 4.x Placement использует getBoxItem() вместо getBox()
                BoxItem boxItem = placement.getBoxItem();
                Box box = boxItem.getBox();
                // В версии 4.x Box использует getId() и getDescription()
                String boxName = box.getId() != null ? box.getId() : box.getDescription();
                // В версии 4.x BoxItem может содержать несколько коробок (getCount())
                // В старой версии каждая коробка была отдельным Placement
                int count = boxItem.getCount();
                if (mapItemsCount.containsKey(boxName)) {
                    ItemsFullInfoCount itemsFullInfoCount = mapItemsCount.get(boxName);
                    itemsFullInfoCount.count += count;  // Добавляем количество коробок, а не 1
                    mapItemsCount.put(boxName, itemsFullInfoCount);
                } else {
                    ItemsFullInfoCount itemsFullInfoCount = new ItemsFullInfoCount(boxName, count, getItemFromName(items,boxName));
                    mapItemsCount.put(boxName, itemsFullInfoCount);
                }
            }
        return mapItemsCount;
    }

    /**
     * Находит товар по его имени из списка товаров.
     *
     * @param items список товаров для поиска
     * @param name имя товара для поиска
     * @return товар с соответствующим именем, или null если не найден
     */
    private Item getItemFromName(List<Item> items, String name){
        for (Item item : items) {
            if (item.getName().equals(name)) return item;
        }
        return null;
    }

    /**
     * Создает строку в листе Excel с указанными значениями ячеек.
     * Автоматически определяет числовые значения и устанавливает соответствующие типы ячеек.
     *
     * @param sheet лист Excel для создания строки
     * @param rowNumber номер строки для создания
     * @param cellValue переменные аргументы значений ячеек как строки
     * @return список созданных ячеек
     */
    private List<XSSFCell> createRow(XSSFSheet sheet, int rowNumber, String ... cellValue){
        XSSFRow rowInfo = sheet.createRow(rowNumber);
        List<XSSFCell> cells = new ArrayList<>();
        int cellNumber = 0;
        for (String value : cellValue) {
            XSSFCell cellInfo = rowInfo.createCell(cellNumber);
            try {
                double valueNum= Double.parseDouble(value);
                cellInfo.setCellType(CellType.NUMERIC);
                cellInfo.setCellValue(valueNum);
            } catch (NumberFormatException e) {
                cellInfo.setCellType(CellType.STRING);
                cellInfo.setCellValue(value);
            }
            cellNumber++;
            cells.add(cellInfo);
        }
        return cells;
    }

    /**
     * Записывает результаты расчетов в главную таблицу на первом листе.
     * Обновляет значения New_Supply_plan для каждого товара и месяца.
     *
     * @param book книга содержащая лист
     * @param document документ плана поставок с результатами расчетов
     */
    private void writeMainTable(XSSFWorkbook book, SupplyPlanDocument document) {
        XSSFSheet sheet = book.getSheetAt(0);
        int offset = 19;
        Iterator<Row> rowIter = sheet.rowIterator();
        while (rowIter.hasNext()) {
            XSSFRow row = (XSSFRow) rowIter.next();
            try {
                Item item = document.getItems().get(row.getRowNum());
                for (int mount = 1; mount < item.getMountPlans().size(); mount++) {
                    MountPlan plan = item.getMountPlans().get(mount);
                    int realOffset = ((mount-1)*4)+offset;
                    XSSFCell cell = row.createCell(realOffset);

                    Color COLOR_light_gray  = new Color(255, 255, 9);
                    XSSFCellStyle style = book.createCellStyle();
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    style.setFillForegroundColor(new XSSFColor(COLOR_light_gray, new DefaultIndexedColorMap()));

                    style.setBorderTop(BorderStyle.THIN);
                    style.setBorderRight(BorderStyle.THIN         );
                    style.setBorderBottom(BorderStyle.THIN);
                    style.setBorderLeft(BorderStyle.THIN);

                    cell.setCellStyle(style);

                    cell.setCellType(CellType.NUMERIC);
                    cell.setCellValue(plan.getNew_Supply_plan());
                }
            } catch (Exception e) {
            }
        }
        XSSFFormulaEvaluator.evaluateAllFormulaCells(book);
    }

    /**
     * Парсит оригинальный лист Excel в вложенную структуру карты.
     * Отображает номера строк к индексам столбцов к ячейкам.
     *
     * @param sheet лист Excel для парсинга
     * @return вложенная карта: номер строки -> индекс столбца -> ячейка
     */
    private Map<Integer,Map<Integer, XSSFCell>> parseOriginal(XSSFSheet sheet) {
        Map<Integer,Map<Integer, XSSFCell>> rows = new HashMap<>();
        Iterator<Row> rowIter = sheet.rowIterator();
        while (rowIter.hasNext()) {
            XSSFRow row = (XSSFRow) rowIter.next();
            Map<Integer, XSSFCell> cells = new HashMap<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                XSSFCell cell = (XSSFCell) cellIterator.next();
                cells.put(cell.getColumnIndex(), cell);
            }
            rows.put(row.getRowNum(), cells);
        }
        return rows;
    }

    /**
     * Преобразует строку в Double, обрабатывая распространенные проблемы форматирования.
     * Заменяет запятые на точки и удаляет пробелы.
     *
     * @param str строка для преобразования
     * @return распарсенное значение Double, или 0.0 если парсинг не удался
     */
    private Double getNumber(String str){
        try {
            return Double.parseDouble(str.replace(",",".").replace(" ", ""));
        } catch (NumberFormatException ignored) {}
        return (double) 0;
    }

    /**
     * Получает строковое значение из ячейки Excel.
     * Обрабатывает ячейки с формулами возвращая их числовое значение как строку.
     *
     * @param cell ячейка Excel
     * @return значение ячейки как строка
     */
    private String getCellValue(XSSFCell cell) {
        if (cell.getCellType() == CellType.FORMULA) return "" + cell.getNumericCellValue();
        return cell.toString();
    }

    /**
     * Получает значение ячейки из карты по индексу, с обработкой ошибок.
     *
     * @param cells карта индексов столбцов к ячейкам
     * @param index индекс столбца для получения
     * @return значение ячейки как строка, или пустая строка если не найдено или произошла ошибка
     */
    private String getMapValue(Map<Integer,XSSFCell> cells, int index){
        try {
            return getCellValue(cells.get(index));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Обрезает строку на первой запятой, возвращая часть до нее.
     *
     * @param str строка для обрезки
     * @return обрезанная строка, или пустая строка если входная строка null или пустая
     */
    private String trunc(String str){
        if (str == null || str.length() == 0) return "";
        String[] result = str.split(",");
        return result[0];
    }

    /**
     * Парсит один товар из ячеек Excel.
     * Извлекает все свойства товара включая размеры, цены и планы по месяцам.
     *
     * @param cells карта индексов ячеек к ячейкам для текущей строки
     * @param tableHeaderRU карта русских заголовков таблицы
     * @param tableHeaderEN карта английских заголовков таблицы
     * @param dop_l дополнительное измерение длины
     * @param dop_w дополнительное измерение ширины
     * @param dop_h дополнительное измерение высоты
     * @param max_h максимальное ограничение высоты
     * @return распарсенный объект Item, или null если парсинг не удался или строка является заголовком
     */
    private Item parseItem(Map<Integer,XSSFCell> cells, Map<Integer,XSSFCell> tableHeaderRU, Map<Integer,XSSFCell> tableHeaderEN, int dop_l, int dop_w, int dop_h, int max_h){
        try {
            Item item = new Item();
            item.setArticle(getMapValue(cells,0));
            item.setName(getMapValue(cells,1));

            //TODO Немного кривой чек но всёже пока так

            if (item.getArticle().contains("Артикул") && item.getName().contains("Наименование")) return null;

            item.setSeller(getMapValue(cells,2));
            item.setDelivery_time(getNumber(getMapValue(cells,3)));


            String[] dimensions_original_container = getMapValue(cells,4).replace("×","x").replace("х","x").replace(" ", "").split("x");
            if (dimensions_original_container.length == 3) {
                int length = Integer.parseInt(trunc(dimensions_original_container[0]));
                int width = Integer.parseInt(trunc(dimensions_original_container[1]));
                int height = Integer.parseInt(trunc(dimensions_original_container[2]));
                item.setDimensions_container(new Dimension("Dimensions_container",width,length,height));
            }

            // ДхШхВ длина ширина высота

            item.setContainer_lifting(getNumber(getMapValue(cells,5)));

            String[] dimensions_original = getMapValue(cells,6).replace("×","x").replace("х","x").replace(" ", "").split("x");
            if (dimensions_original.length == 3) {
                int length = Integer.parseInt(trunc(dimensions_original[0]))+dop_l;
                int width = Integer.parseInt(trunc(dimensions_original[1]))+dop_w;
                int height = Integer.parseInt(trunc(dimensions_original[2]))+dop_h;
                if (height >= max_h) height = item.getDimensions_container().getHeight();
                item.setDimensions_item(new Dimension("Dimensions_Item", width,length,height));
            }

            item.setType(getMapValue(cells,7));
            item.setWeight(getNumber(getMapValue(cells,8)));
            item.setPrice(getNumber(getMapValue(cells,9)));
            item.setCurrency_type(getMapValue(cells,10));
            item.setCurrentWarehouseBalance(getNumber(getMapValue(cells,11)));

            item.setManual_Supply(getNumber(getMapValue(cells,12)));

            List<MountPlan> mountPlanList = new ArrayList<>();

            // Create First Mount
            MountPlan firstMountPlan = new MountPlan();
            firstMountPlan.setItem(item);
            firstMountPlan.setName(tableHeaderRU.get(13).toString().replace("Поступление ",""));
            firstMountPlan.setSupply_plan(getNumber(getMapValue(cells,13)));
            firstMountPlan.setNew_Supply_plan(getNumber(getMapValue(cells,14)));
            firstMountPlan.setSales_program(getNumber(getMapValue(cells,15)));
            item.setFirstMountPlanSales_plan_left(getNumber(getMapValue(cells,16))); // Изза этого говна приходитс япарсить отдельно
            firstMountPlan.setFirstMountPlanSales_plan_left(item.getFirstMountPlanSales_plan_left());
            firstMountPlan.setPlanned_balance(getNumber(getCellValue(cells.get(17))));

            mountPlanList.add(firstMountPlan);

            for (int i = 18; i < cells.size(); i = i + 4) {
                MountPlan mountPlan = new MountPlan();
                mountPlan.setItem(item);
                mountPlan.setName(tableHeaderRU.get(i).toString().replace("Поступление ",""));
                mountPlan.setParent(mountPlanList.get(mountPlanList.size()-1));
                mountPlan.setSupply_plan(getNumber(cells.get(i).toString()));
                mountPlan.setNew_Supply_plan(getNumber(cells.get(i+1).toString()));
                mountPlan.setSales_program(getNumber(cells.get(i+2).toString()));
                mountPlanList.add(mountPlan);
            }
            item.setMountPlans(mountPlanList);
            return item;
        } catch (Exception e) {
            return null;
        }
    }

}
