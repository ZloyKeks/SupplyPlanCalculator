package by.legan.gt_tss.supplyplancalculator.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Класс конфигурации для планирования и асинхронного выполнения задач.
 * Настраивает пул потоков для запланированных задач.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulerConfiguration implements SchedulingConfigurer {

    /**
     * Настраивает регистратор запланированных задач с пользовательским исполнителем задач.
     *
     * @param taskRegistrar регистратор запланированных задач для настройки
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }

    /**
     * Создает и настраивает пул потоков для запланированных задач.
     * Исполнитель использует фиксированный пул из 5 потоков.
     *
     * @return настроенный сервис исполнителя
     */
    @Bean(destroyMethod="shutdown")
    public ExecutorService taskExecutor() {
        return Executors.newScheduledThreadPool(5);
    }
}
