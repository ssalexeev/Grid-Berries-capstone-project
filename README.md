
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
