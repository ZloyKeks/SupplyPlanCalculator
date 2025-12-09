package by.legan.gt_tss.supplyplancalculator.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Adds a filesystem template resolver so Thymeleaf can render pages
 * when running from the project directory (mvn spring-boot:run),
 * even if classpath resources are not picked up for some reason.
 * The default classpath resolver still works for packaged JARs.
 */
@Configuration
public class ThymeleafConfig {

    @Bean
    public SpringResourceTemplateResolver fileSystemTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("file:src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setCheckExistence(true);
        resolver.setOrder(1); // try filesystem before default classpath resolver
        return resolver;
    }
}

