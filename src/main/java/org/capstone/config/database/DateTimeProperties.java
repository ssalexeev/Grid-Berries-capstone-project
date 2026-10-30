package org.capstone.config.database;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Getter
@Setter
@ConfigurationProperties(prefix = "time")
public class DateTimeProperties {
    private Duration timeInDefaultUnit;
    private Duration timeInNano;

    private LocalDate isoDate;
    private LocalDateTime isoDateTime;
    private LocalTime isoTime;

    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate customDate;
}
