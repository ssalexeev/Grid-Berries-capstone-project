# How can we configure the application context when running tests with @SpringBootTest annotation ?
### Application Context Configuration in `@SpringBootTest`
| Approach | Usage | Example |
| :--- | :--- | :--- |
| **Inline Properties** | Override config properties directly | `@SpringBootTest(properties = {"key=val"})` |
| **Profiles** | Activate environment files (e.g., `application-test.yml`) | `@ActiveProfiles("test")` |
| **Property Files** | Load specific external property files | `@TestPropertySource("classpath:test.properties")` |
| **Web Environment** | Control embedded server lifecycle (`MOCK`, `RANDOM_PORT`, `NONE`) | `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` |
| **Dynamic Properties** | Set runtime values (e.g., Testcontainers) | `@DynamicPropertySource static void props(DynamicPropertyRegistry r)` |
| **Bean Mocking** | Replace beans with Mockito mocks | `@MockitoBean` (Boot 3.4+) or `@MockBean` (older) |
| **Test Beans** | Add/override test-only beans | `@TestConfiguration static class TestConfig { ... }` |
| **Scoped Context** | Restrict configuration classes loaded | `@SpringBootTest(classes = {AppConfig.class})` |
| **Context Initializer** | Customize context programmatically before refresh | `@ContextConfiguration(initializers = MyInitializer.class)` |
| **Exclude Auto-Config**| Disable unwanted auto-configurations | `properties = "spring.autoconfigure.exclude=..."` |

# How can you exclude auto configuration from a test ?
```java
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration"
})
class MyIntegrationTest {
    // ...
}
// or

@SpringBootTest
@EnableAutoConfiguration(exclude = {
        MailSenderAutoConfiguration.class,
        DataSourceAutoConfiguration.class
})
class MyIntegrationTest {
   // ...
}
```
# How many application contexts can be cached when running tests ? What are the side effects if we increase the cache size ? What would be the side effects if there was no caching ?
By default, the Spring TestContext Framework caches up to 32 application contexts. \
The cache uses the Least Recently Used (LRU) eviction policy. When a 33rd distinct context configuration is loaded, the least recently used context is closed and evicted. \

## Increasing the context cache size keeps more distinct contexts active in memory simultaneously.\
1. **High JVM Heap Consumption & OutOfMemoryError (OOM):** Each Spring ApplicationContext holds bean definitions, singletons, reflection metadata, ORM session factories, and class loaders. Holding dozens of full @SpringBootTest contexts simultaneously can easily exhaust the JVM heap.
2. **Leaked External Resources & Port Exhaustion:** Contexts often manage thread pools, database connection pools (HikariCP), embedded brokers, or network sockets. Keeping many alive concurrently can exceed OS file descriptor limits or open connection limits.
3. **Higher Garbage Collection (GC) Pressure:** A large live-object graph across multiple dormant contexts increases old-generation memory occupancy, leading to longer and more frequent GC pauses.
4. **Masked Configuration Fragmentation:** A higher limit allows developers to introduce minor, accidental context differences (like subtle @TestPropertySource or @MockBean variations) without triggering evictions, hiding poor test design behind inflated memory requirements.
## Side Effects If There Was No Caching (Cache Size = 0)
If test context caching were disabled, every test class would bootstrap a brand-new ApplicationContext from scratch and destroy it immediately after execution. \
1. **Massive Increase in Test Execution Time:** Spring context bootstrapping (component scanning, reflection, dependency injection, Hibernate metadata validation, schema migrations) is computationally expensive. Running hundreds of test classes sequentially could turn a 2-minute build into a 30+ minute build.
2. **Repeated Resource Initialization Overhead:** Heavy external integrations—such as running Liquibase/Flyway migrations, spinning up in-memory databases (H2), or building proxy classes—would execute repeatedly for every single test class.
3. **CPU and Thread Churn:** Continuous startup, lifecycle callback execution, and teardown of thread pools (e.g., TaskExecutor, message listeners) causes high CPU utilization throughout the entire build.
4. **Loss of Test Isolation Leak Warnings:** Tools and annotations like @DirtiesContext lose their semantic meaning and diagnostic value because contexts would already be thrown away after every run regardless.

# Can @MockBean be used if the bean is not already defined in the application context ?   
Yes. @MockBean does not require existing bean definition in the application context. \
This makes it useful for satisfying missing dependencies during isolated slice tests (such as @WebMvcTest or @DataJpaTest) or providing implementations for interfaces that have no concrete production bean define