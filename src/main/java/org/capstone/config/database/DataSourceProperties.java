package org.capstone.config.database;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "datasource")
public record DataSourceProperties (String url, String username, String password, String driver) {
}
