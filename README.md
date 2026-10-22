# Questions

## What is the ApplicationContext?
The ApplicationContext is the central IoC (Inversion of Control) container in Spring. 
Instead of manually instantiating and managing objects, the ApplicationContext creates, 
configures, and injects beans wherever dependencies are required. It holds bean definitions
and manages their entire lifecycle (creation, initialization, and destruction) according to their defined scopes.

## What are the tradeoffs of different approaches to injecting beans ?

### Constructor Injection
Constructor Injection is most preferred way of  bean injection.
However, it can bring a cycle dependency, when a->b->c and c depends on a.
Spring try to create beans when it needs, but in this situation spring doesn't understand what it should create in first way "a" or "c".

### Setter Injection
The primary drawback of **setter injection** is that it prevents fields from being declared `final`,
making objects mutable and compromising thread safety. It also creates a risk of `NullPointerExceptions` 
because an object can exist in an incomplete, partially initialized state if a developer or test forgets 
to invoke the setter before calling business methods. Additionally, setter injection makes unit testing more
verbose by requiring multiple method calls after instantiation, and it masks bad class design by allowing dependencies 
to grow excessively without triggering an obvious code smell like a bloated constructor signature would.

### Field injection
The main issue with field injection is that it tightly couples your classes to the dependency injection framework 
by relying on annotations directly on private fields. Because dependencies are set using reflection behind the scenes,
you cannot declare fields as final, which loses immutability and thread safety. It also makes unit testing significantly 
harder because you cannot easily pass mock objects through a constructor or setter—forcing you to either launch a full
framework context or use reflection utilities to populate private variables. Additionally, field injection hides design flaws,
making it far too easy to accumulate dozens of unnoticeable dependencies and violate the Single Responsibility Principle without 
realizing the class has become bloated.

Field injection is widely considered an anti-pattern in modern development and should generally be avoided outside of quick prototypes or test classes.

## Why do we need to use @Qualifier when multiple of the same type are defined ?
We need @Qualifier because Spring resolves dependencies primarily by type. 
When multiple beans of the same type exist in the context, 
Spring cannot determine which one to inject on its own and throws a NoUniqueBeanDefinitionException.
Adding @Qualifier("beanName") specifies the exact bean name or qualifier ID to inject, eliminating the ambiguity.

## How to avoid loading of heavy beans (like caches or other beans with heavy init logic) on startup and decrease startup time?
Set up @Lazy annotation on heavy beans initialization.

## What are Spring lifecycle stages and methods?
### Spring Bean Lifecycle Stages & Methods

The lifecycle of a Spring Bean spans four primary phases: **Instantiation**, **Populate Properties**, **Initialization**, and **Destruction**.

---

#### Lifecycle Flow

```text
  1. Instantiation  ──►  2. Populate Properties  ──►  3. Aware Interfaces
                                                             │
  5. Destruction   ◄──   4. Ready for Use    ◄──  Initialization
```
```java
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class DatabaseConnectionBean implements InitializingBean, DisposableBean {

    public DatabaseConnectionBean() {
        System.out.println("1. Instantiation: Constructor called");
    }

    // --- Initialization Methods ---

    @PostConstruct
    public void postConstruct() {
        System.out.println("4a. Initialization: @PostConstruct executed");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("4b. Initialization: InitializingBean.afterPropertiesSet executed");
    }

    public void customInit() {
        System.out.println("4c. Initialization: Custom init-method executed");
    }

    // --- Destruction Methods ---

    @PreDestroy
    public void preDestroy() {
        System.out.println("6a. Destruction: @PreDestroy executed");
    }

    @Override
    public void destroy() {
        System.out.println("6b. Destruction: DisposableBean.destroy executed");
    }

    public void customDestroy() {
        System.out.println("6c. Destruction: Custom destroy-method executed");
    }
}
```

## How can we customize the component scanning process ?

We can customize the component scanning process using five primary methods in `@ComponentScan`:

1. `basePackages` / `basePackageClasses`: Specify target packages or type-safe marker classes to scan.
2. `includeFilters` & `excludeFilters`: Whitelist or blacklist components using annotations, regex, class types (`ASSIGNABLE_TYPE`), or AspectJ expressions.
3. `useDefaultFilters = false`: Disable default automatic scanning for standard stereotypes (`@Component`, `@Service`, `@Repository`, `@Controller`).
4. Custom `TypeFilter`: Implement Spring’s `TypeFilter` interface to write custom programmatic matching rules based on class metadata.
5. `ClassPathBeanDefinitionScanner`: Programmatically trigger and configure scanning logic at runtime.