package org.capstone.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


// Example of read properties into a String array which are separated with '-'
@Component
public class MetricEventTypeConfig {
    @Value("#{'${metrics.event.type}'.split('-')}")
    String[] eventTypes;
}
