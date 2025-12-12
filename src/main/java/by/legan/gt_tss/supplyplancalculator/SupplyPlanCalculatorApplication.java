package by.legan.gt_tss.supplyplancalculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Главный класс Spring Boot приложения для калькулятора планов поставок.
 * Приложение рассчитывает оптимальные планы поставок и стратегии загрузки контейнеров.
 */
@SpringBootApplication
public class SupplyPlanCalculatorApplication extends SpringBootServletInitializer {

    /**
     * Настраивает Spring application builder для развертывания в виде сервлета.
     *
     * @param application Spring application builder
     * @return настроенный Spring application builder
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SupplyPlanCalculatorApplication.class);
    }

    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(SupplyPlanCalculatorApplication.class, args);
    }

}
