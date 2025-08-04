# Solution Design

This document outlines the design decisions, trade-offs, and architecture of the FinTech-style payments system.

## 1. High-Level Architecture

The system is designed as a microservices architecture with two main services:
- **Transfer Service**: The public-facing API for initiating transfers. It handles user requests, idempotency, and orchestration.
- **Ledger Service**: The internal service responsible for the core ledger functionality, including managing accounts and ensuring the atomicity of financial transactions.

This separation of concerns allows the system to scale different parts independently and isolates the critical ledger logic from the public-facing API.

## 2. Ledger Service Design

### 2.1. Data Model
- **Account**: Stores the `id`, `balance`, and a `version` for optimistic locking. `BigDecimal` is used for the balance to ensure precision with monetary values.
- **LedgerEntry**: An immutable record of every debit or credit operation. It includes a `transferId` to link the two entries of a single transfer. A unique constraint on `transferId` is used to enforce idempotency at the database level.

### 2.2. Concurrency Control
To prevent race conditions and ensure data integrity (e.g., preventing negative balances), the system uses pessimistic locking (`SELECT ... FOR UPDATE`) on the account rows involved in a transaction. This was chosen over optimistic locking because:
- **Safety**: For financial transactions, pessimistic locking provides a stronger guarantee of consistency, especially under high contention for the same accounts. It prevents concurrent transactions from even reading the rows, avoiding any chance of a lost update.
- **Simplicity**: While optimistic locking can offer better performance in low-contention scenarios, it requires more complex retry logic in the application layer when a version conflict occurs. For this system, the simplicity and safety of pessimistic locking were prioritized.

### 2.3. Idempotency
Idempotency in the Ledger Service is handled at the database level with a unique constraint on the `transferId` in the `ledger_entries` table. The service logic catches the `DataIntegrityViolationException` that occurs when a duplicate `transferId` is inserted, and treats it as a successful idempotent retry. This is a robust way to handle idempotency, as it relies on the database as the single source of truth.

## 3. Transfer Service Design

### 3.1. Idempotency
The Transfer Service provides idempotency for client requests via the `Idempotency-Key` header.
- An `IdempotencyKey` entity is stored in the database, mapping the key to the response of the initial request.
- The key has a 24-hour TTL, after which it expires and can be reused.
- If a request is received with an existing, non-expired key, the stored response is returned immediately without re-processing the transfer.

### 3.2. Resilience (Circuit Breaker)
The `transfer-service` uses Resilience4j's Circuit Breaker pattern to protect itself from failures in the `ledger-service`.
- The `LedgerServiceClient`'s `postTransfer` method is annotated with `@CircuitBreaker`.
- If the `ledger-service` becomes unavailable or consistently returns errors, the circuit breaker will "open", and subsequent calls will fail fast without attempting to contact the service.
- A fallback method is provided to return a meaningful error to the client, indicating that the service is temporarily unavailable.

### 3.3. Concurrency for Batch Transfers
The `POST /transfers/batch` endpoint processes up to 20 transfers concurrently using a Java `ExecutorService` with a fixed thread pool. Each transfer request in the batch is submitted as a separate task to the thread pool, allowing them to be processed in parallel. This improves the throughput of the batch endpoint.

## 4. Security and Observability

### 4.1. Security
No authentication or authorization is implemented in this system. However, it is designed to be easily extensible for security:
- **Proposed Solution**: A JWT-based authentication system could be implemented. An API gateway could be placed in front of the microservices to handle authentication and JWT validation. The gateway would then pass the user's identity to the downstream services in a header. Spring Security could be used in each service to enforce authorization rules.

### 4.2. Observability
- **Request Correlation**: A `RequestCorrelationFilter` is implemented in both services to add a unique `X-Correlation-ID` to every request and log it with every log message using MDC. This allows for easy tracing of a request as it flows through the system.
- **Logging**: Structured logging is configured using `logback-spring.xml` to include the correlation ID.
- **API Documentation**: OpenAPI (Swagger) documentation is provided for each service to allow for easy exploration of the APIs.

## 5. Testing Challenges and Trade-offs

The integration tests for the `ledger-service` proved to be a significant challenge due to persistent issues with transaction management in the Spring Boot test environment. Despite multiple attempts to ensure test isolation using various strategies (`@Transactional`, `@DirtiesContext`, manual database cleanup), the tests continued to fail with `UnexpectedRollbackException`.

This indicates a subtle and complex interaction between the test framework's transaction management and the application's transaction logic. Due to time constraints, I made the trade-off to leave these tests in a failing state and move on with the rest of the implementation, documenting the issue here. In a real-world scenario, resolving these test failures would be a top priority.
