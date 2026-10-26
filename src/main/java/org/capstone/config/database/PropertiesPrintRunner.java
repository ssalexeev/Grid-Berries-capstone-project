package org.capstone.config.database;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
public class PropertiesPrintRunner implements CommandLineRunner {
    private final ApplicationContext applicationContext;
    private final ConfigurableEnvironment environment;

    @Override
    public void run(String... args) throws Exception {
        //printAllBeans();
        //printAllProperties();
    }

    public void printAllBeans() {
        System.out.println("            ALL REGISTERED BEAN NAMES            ");
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        Arrays.sort(beanDefinitionNames);

        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println(" - " + beanDefinitionName);
        }
    }

    public void printAllProperties() {
        System.out.println("             ALL ENVIRONMENT PROPERTIES          ");

        TreeMap<String, Object> properties = new TreeMap<>();

        for(PropertySource<?> source: environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> enumerablePropertySource) {
                for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                    properties.put(propertyName, enumerablePropertySource.getProperty(propertyName));
                }
            }
        }

        properties.forEach((key, value) ->
                System.out.println(" - " + key + ": " + value));

    }
}
