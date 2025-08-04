# FinTech-style Payments System

This project is a minimal FinTech-style payments system composed of two Spring Boot microservices. The domain is simple internal wallet transfers: move money between two accounts and keep an accurate ledger even under failures and heavy concurrency.

## Services

- **Ledger Service**: Stores accounts and immutable ledger entries. Runs on port 8081.
- **Transfer Service**: Public API for initiating transfers. Runs on port 8080.

## How to Build and Run

### Prerequisites
- Java 17
- Maven 3.8+
- Docker & Docker Compose

### Build
To build the project and run the tests, execute the following command from the root directory:
```bash
mvn clean install
```

### Run with Maven
You can run each service individually using the Spring Boot Maven plugin.

**Ledger Service:**
```bash
mvn spring-boot:run -pl ledger-service
```

**Transfer Service:**
```bash
mvn spring-boot:run -pl transfer-service
```

### Run with Docker Compose
To build and run the entire system using Docker Compose, execute the following command from the root directory:
```bash
docker-compose up --build
```
The services will be available at:
- **Ledger Service**: `http://localhost:8081`
- **Transfer Service**: `http://localhost:8080`

## API Documentation
Once the services are running, you can access the Swagger UI for each service to explore the APIs:
- **Ledger Service Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **Transfer Service Swagger UI**: `http://localhost:8080/swagger-ui.html`
