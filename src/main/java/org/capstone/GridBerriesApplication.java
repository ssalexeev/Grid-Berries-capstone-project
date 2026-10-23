package org.capstone;

import org.capstone.config.database.DataSourceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DataSourceProperties.class)
public class GridBerriesApplication {
    public static void main(String[] args) {
        SpringApplication.run(GridBerriesApplication.class, args);
    }
}
