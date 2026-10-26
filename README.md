
# What is the difference between the @Configuration,@Component and @Service annotations ?

## @Confugiration
@Configuration indicates that a class contains factory methods annotated with @Bean to instantiate and configure Spring beans.

## @Component
@Component annotation allows to register Bean of the class which has this annotation in SpringContext

## @Service
@Service has same point as @Component, but add more specific information about Bean, which is "service" business-tag

# What value will a property have if it is defined in two different profiles both of which are active ?
When a property key is defined in multiple active Spring profiles,
the value defined in the last activated profile in the declaration sequence takes precedence and wins.

# Why would we use factory beans instead of regular beans ?
1. Encapsulate Complex Instantiation: Ideal when creating an object requires extensive setup, complex builder patterns, or external library initializations that would clutter configuration classes.
2. Third-Party Library Integration: Useful for integrating non-Spring frameworks or legacy code into the Spring context (e.g., creating ORM sessions, connection pools, or RPC proxies like ProxyFactoryBean).
3. Dynamic / Runtime Bean Construction: Allows you to decide the exact concrete class or proxy to construct dynamically at runtime based on environment parameters or custom logic.

# How can we override any property defined in any .properties file ?
We can override autoconfigured properties by defining an explicit @Bean method in a configuration class. 
For example, if Spring Boot automatically configures a default DataSource using the spring.datasource.* properties 
from application.properties, we can declare our own @Bean method returning a DataSource.
When Spring detects our custom @Bean definition, it overrides the default autoconfigured bean and uses our custom instance instead.

# Does PostDestruct get called for a prototype scope bean ?
No, Spring does not call the @PreDestroy (or @PostConstruct's counterpart @PreDestroy) callback method for prototype-scoped beans.
Spring manages the complete lifecycle of singleton beans, including their destruction. However, for prototype beans, Spring only handles instantiation, configuration, and initialization.
Once the prototype bean is created and handed over to the client code, Spring drops its reference to the instance to prevent memory leaks and no longer manages its lifecycle.


# 5.3.2
# What is the difference between regular configuration and autoconfiguration ?
Regular configuration fully depends from us. We should define config properties, inject it to config classes and other.
In other hand autoconfiguration gives prepared ways for connecting needed tools, for example database connectivity. 
We just paste certain Db properties to .properties file and Spring automatically analyze this file and with
conditions build Datasource class.

# Would all conditional annotations on bean definitions work in regular configuration classes ? Elaborate.
While environment and classpath-based annotations (like @ConditionalOnProperty and @ConditionalOnClass)
work reliably, bean-presence conditionals (like @ConditionalOnBean and @ConditionalOnMissingBean)
do not work reliably on regular @Configuration classes.\
Because regular configuration classes are processed before auto-configuration and lack a deterministic processing order,
bean-detection conditions often fail due to race conditions. Spring Boot officially recommends using them only inside
auto-configuration classes.
# How can we customize the auto configuration process ?
1. Override via User-Defined Beans (The Standard Path).\
   Most auto-configurations back off if a bean of the same type already exists, using @ConditionalOnMissingBean. \
   Simply declaring your own bean in a regular @Configuration class takes precedence and disables the default bean creation.
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain customFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).build();
    }
}
```
2. Exclude Specific Auto-Configuration Classes
   Disable specific auto-configurations entirely if you do not want their beans or behavior registered.
```java
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class Application { ... }
```
3. Use Customizer Interfaces
   Spring Boot provides dedicated callback interfaces for many core components, allowing you to modify pre-configured builders without replacing the entire configuration.
\
Examples:\

- WebServerFactoryCustomizer<T> (customize embedded Tomcat/Jetty ports, SSL, threads)
- Jackson2ObjectMapperBuilderCustomizer (add serializers/deserializers to ObjectMapper)
- RestClientCustomizer / WebClientCustomizer (configure default headers, timeouts)
```java
Java
@Bean
public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webServerCustomizer() {
return factory -> factory.setPort(8081);
```

# What is the condition that causes a tomcat server to start on port 8080 when the application starts ?
Tomcat starts on port 8080 when four conditions are met:

1. **Web starter is present:** `spring-boot-starter-web` (which bundles embedded Tomcat) is on the classpath.
2. **It's a servlet app:** Spring Boot detects a standard servlet environment rather than a reactive (`WebFlux`) or non-web application.
3. **No custom server bean exists:** You haven't declared your own `ServletWebServerFactory` bean (like Jetty or Undertow).
4. **No custom port is configured:** You haven't set `server.port` in `application.properties`, so it falls back to the hardcoded default of **8080**.
