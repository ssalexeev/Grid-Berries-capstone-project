package org.capstone.repository.jdbc.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionPoolBenchmarking {

    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/mydatabase";
    private static final String USER = "myuser";
    private static final String PASS = "mypassword";

    private static final int THREAD_COUNT = 5;
    private static final double SLEEP_SECONDS = 1.0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Starting DB Connection Speed Tests ===");

        // Single thread
        DataSource singleConnDs = new SingleConnectionDataSource(JDBC_URL, USER, PASS);
        runBenchmark("1. Custom Single Connection", singleConnDs, true);

        // HikariPool
        HikariDataSource hikariMultiPool = createHikariPool(5);
        try {
            runBenchmark("3. HikariCP Pool (Size = 5)", hikariMultiPool, false);
        } finally {
            hikariMultiPool.close();
        }
    }

    private static void runBenchmark(String testName, DataSource ds, boolean requiresExternalSync) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        long start = System.currentTimeMillis();

        for (int i = 1; i <= THREAD_COUNT; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    Connection conn = ds.getConnection();

                    if (requiresExternalSync) {
                        synchronized (conn) {
                            executeQuery(conn);
                        }
                    } else {
                        try (conn; Statement stmt = conn.createStatement()) {
                            try (ResultSet rs = stmt.executeQuery("SELECT pg_sleep(" + SLEEP_SECONDS + ")")) {
                                rs.next();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[" + testName + "] Worker " + workerId + " error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long duration = System.currentTimeMillis() - start;
        System.out.printf("%-35s Total Time: %.2f seconds%n", testName + ":", duration / 1000.0);
    }

    private static void executeQuery(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT pg_sleep(" + SLEEP_SECONDS + ")")) {
            rs.next();
        }
    }

    private static HikariDataSource createHikariPool(int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(USER);
        config.setPassword(PASS);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(5000);
        return new HikariDataSource(config);
    }
}

