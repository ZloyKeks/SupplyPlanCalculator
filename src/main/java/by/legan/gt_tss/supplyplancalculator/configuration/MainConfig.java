package by.legan.gt_tss.supplyplancalculator.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@Data
public class MainConfig {
    @Value("${writeVisualisationToDisk}")
    private boolean writeVisualisationToDisk = false;
}
