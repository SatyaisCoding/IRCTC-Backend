# IRCTC Microservices Backend

A robust, enterprise-grade Spring Boot microservices backend for the Indian Railway Catering and Tourism Corporation (IRCTC) clone. This project is built using Java 21, Spring Boot 3.3.x, and Maven, following modern software architecture patterns, cloud-native principles, and microservice best practices.

---

## 🏗️ Architecture Overview

The application is structured into four independent, decoupled microservices, each fulfilling a specific domain-driven boundary:

```text
                                       ┌──────────────────────┐
                                       │    Web / Client      │
                                       └──────────┬───────────┘
                                                  │
                                                  ▼
                                      ┌───────────────────────┐
                                      │  API Gateway (Port)   │ (Ready for Gateway)
                                      └──────────┬────────────┘
                                                 │
                  ┌──────────────────────────────┼──────────────────────────────┐
                  ▼                              ▼                              ▼
     ┌─────────────────────────┐    ┌─────────────────────────┐    ┌─────────────────────────┐
     │      user-service       │    │     search-service      │    │     booking-service     │
     │      (Port 8084)        │    │       (Port 8083)       │    │       (Port 8081)       │
     └────────────┬────────────┘    └────────────┬────────────┘    └────────────┬────────────┘
                  │                              │                              │
                  ▼                              ▼                              ▼
     ┌─────────────────────────┐    ┌─────────────────────────┐    ┌─────────────────────────┐
     │  PostgreSQL: irctc_user │    │ PostgreSQL: irctc_search│    │PostgreSQL: irctc_booking│
     └─────────────────────────┘    └─────────────────────────┘    └─────────────────────────┘
                                                 ▲                              ▲
                                                 │                              │
                                                 │      (Eventual Sync / HTTP)  │
                                                 │                              │
                                                 │                 ┌────────────┴────────────┐
                                                 │                 │     payment-service     │
                                                 │                 │       (Port 8082)       │
                                                 │                 └────────────┬────────────┘
                                                 │                              │
                                                 │                              ▼
                                                 │                 ┌─────────────────────────┐
                                                 │                 │PostgreSQL: irctc_payment│
                                                 │                 └─────────────────────────┘
                                                 │
                                                 ▼
                                        ┌──────────────────┐
                                        │  Redis (Cache)   │ (Ready for Redis)
                                        └──────────────────┘
```

1. **`user-service` (Port `8084`)**: Manages user profiles, registrations, authentication, and role management. Ready for **JWT integration**.
2. **`search-service` (Port `8083`)**: Handles train searches, route options, station schedules, and seat availability. Integrated with **Redis caching** placeholders.
3. **`booking-service` (Port `8081`)**: Manages ticket booking lifecycles, passenger information, and seat reservations.
4. **`payment-service` (Port `8082`)**: Simulates railway transactions, payment gateways, invoice generation, and status webhooks.

---

## ⚡ Key Technical Features & Ecosystem Readiness

This repository is built and structured to integrate seamlessly into a fully fledged distributed system.

### 🛡️ JWT Authentication Ready
Each microservice is ready to support JWT validation. In production:
- The **API Gateway** intercepts requests, validates the JWT, and extracts user claims (e.g., `userId`, `roles`).
- The gateway passes these claims down to the downstream microservices via custom HTTP headers (e.g., `X-User-Id`, `X-User-Roles`).
- Placeholder filter hooks have been supplied within the `config` module of each microservice.

### 📡 Eureka Service Registry Ready
- Ready to register with Eureka Server. 
- The dependencies for Eureka Discovery Client are pre-added (commented out) in each service's `pom.xml`.
- When you set up a Eureka Server, simply uncomment the `spring-cloud-starter-netflix-eureka-client` dependency and add `@EnableDiscoveryClient` to the Application main classes.

### ⚙️ Spring Cloud Config Server Ready
- Centralized configuration capabilities can be unlocked by adding `spring-cloud-starter-config` (pre-populated and commented in `pom.xml`).
- Supports seamless injection of variables across different environments (dev, test, prod).

### 🚀 Redis Caching Ready
- The cache framework is supported via `spring-boot-starter-data-redis` (commented out in `pom.xml`).
- Search and booking endpoints are set up to use `@Cacheable` and `@CacheEvict` annotations out of the box once the Redis connection parameters in `application.yml` are uncommented.

### 🐳 Containerization & Orchestration
- Every microservice features a production-ready, multi-stage **Dockerfile** based on Eclipse Temurin JDK 21.
- A parent **`docker-compose.yml`** coordinates container deployments, local databases, and a shared Redis node.

---

## 🛠️ Getting Started & Local Development

### Prerequisites
- **Java**: JDK 21 or higher
- **Maven**: 3.8+
- **Docker & Docker Compose** (Optional, for database & container runtimes)
- **PostgreSQL**: Local instance running on standard port `5432` if not using Docker.

### Local Setup (Using Docker Compose for DBs & Redis)
1. **Start Infrastructure (PostgreSQL & Redis)**:
   Navigate to the root `IRCTC-Backend` directory and start the Docker Compose stack:
   ```bash
   docker-compose up -d
   ```
   This will spin up:
   - A PostgreSQL server creating separate databases for each service (`irctc_user`, `irctc_booking`, `irctc_payment`, `irctc_search`).
   - A Redis Cache container.

2. **Build and Run Microservices Individually**:
   Navigate into any microservice directory (e.g., `booking-service`):
   ```bash
   cd booking-service
   mvn clean package -DskipTests
   mvn spring-boot:run
   ```

### Ports and Actuator Health Endpoints

All services are equipped with custom global exception handlers and Spring Actuator health monitoring:

| Service | Port | Local Run Command | Health Check Endpoint |
|---|---|---|---|
| **booking-service** | `8081` | `mvn spring-boot:run` | [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health) |
| **payment-service** | `8082` | `mvn spring-boot:run` | [http://localhost:8082/actuator/health](http://localhost:8082/actuator/health) |
| **search-service** | `8083` | `mvn spring-boot:run` | [http://localhost:8083/actuator/health](http://localhost:8083/actuator/health) |
| **user-service** | `8084` | `mvn spring-boot:run` | [http://localhost:8084/actuator/health](http://localhost:8084/actuator/health) |

---

## 📂 Package Architecture per Microservice

Each service has been engineered using clean coding practices with strict package separation:
- `controller`: REST Endpoints, request mapping, HTTP status code handling.
- `service`: Domain business logic, transactional operations, and service interface patterns.
- `repository`: Spring Data JPA interfaces for database operations.
- `entity`: Database mapping models (JPA) annotated with Hibernate features.
- `dto`: Request and response models validating constraints via `jakarta.validation`.
- `config`: Security contexts, database setups, and component configurations.
- `exception`: Custom domain-specific exceptions and `GlobalExceptionHandler` with `@RestControllerAdvice`.
