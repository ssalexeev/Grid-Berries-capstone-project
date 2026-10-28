# Connection pooling advantages and disadvantages

## Advantages 
- Thread Safety Without Socket Conflicts: Unlike sharing a raw JDBC Connection object, Hikari hands out the physical connection to one thread at a time. Subsequent threads queue safely inside Hikari until the previous thread calls conn.close().

- Built-in Timeouts: If a thread holds the single connection for too long, other threads fail fast via HikariPool$PoolBag$TimeoutException instead of hanging indefinitely.

- Automatic Reconnection: If the single underlying database connection drops or resets, Hikari transparently closes the broken connection and opens a new one on the next getConnection() request.

## Disadvantages
- Resource Consumption on the Database Server : 
- Risk of Connection Leaks
- Stale & Broken Connection Overhead
- Shared State Contention & Leaky Contexts
- Added Architectural Complexity & Configuration Tuning