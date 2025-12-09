package by.legan.gt_tss.supplyplancalculator.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class MainConfig {
    @Value("${writeVisualisationToDisk:true}")
    private boolean writeVisualisationToDisk = false;
}
