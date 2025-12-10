package by.legan.gt_tss.supplyplancalculator.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Конфигурация для обслуживания статических ресурсов.
 * Добавляет файловую систему как источник статических файлов для разработки.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Добавляем файловую систему как источник статических ресурсов
        // Это позволяет Spring Boot видеть файлы из src/main/resources/static при разработке
        String staticPath = new File("src/main/resources/static").getAbsolutePath();
        
        registry.addResourceHandler("/**")
                .addResourceLocations(
                        "file:" + staticPath + "/",
                        "classpath:/static/",
                        "classpath:/META-INF/resources/"
                )
                .setCachePeriod(0); // Отключаем кэширование для разработки
    }
}

