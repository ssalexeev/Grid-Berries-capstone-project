package org.capstone.config.database.production;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;


@Configuration
@Profile("production")
public class ProductionDataSourceConfig {
    @Value("${datasource.url}")
    private String url;

    @Value("${datasource.username}")
    private String username;

    @Value("${datasource.password}")
    private String password;

    @Value("${datasource.driver}")
    private String driver;

    @Bean
    public DataSource dataSource() {
        int maxPoolSize = 5;
        HikariConfig config = new HikariConfig();

        System.out.println(username);
        System.out.println(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(5000);

        return new HikariDataSource(config);
    }
}
