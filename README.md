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

1. **`api-gateway` (Port `8080`)**: Intercepts client traffic, performs **JWT Validation**, **Redis Rate Limiting**, and reactively forwards microservice calls using a non-blocking WebClient proxy with a **Custom Distributed Circuit Breaker**.
2. **`user-service` (Port `8084`)**: Manages user profiles, registrations, authentication, and password updates. Integrates with **Redis caching** for high-performance profile retrieval.
3. **`search-service` (Port `8083`)**: Handles train searches, route options, station schedules, and seat availability. Integrated with Redis caching.
4. **`booking-service` (Port `8081`)**: Manages ticket booking lifecycles, passenger information, and seat reservations.
5. **`payment-service` (Port `8082`)**: Simulates railway transactions, payment gateways, invoice generation, and status webhooks.

---

## ⚡ Key Technical Features & Implemented Architecture

This repository showcases advanced, production-grade microservice architecture patterns and local development workflows.

### 🛡️ Stateless JWT Security Offloading
- The **API Gateway** acts as the single security enforcement point. It intercepts all incoming requests, validates the JWT, and extracts user claims (e.g., `userId`, `roles`).
- The Gateway injects these claims as trusted custom HTTP headers (`X-User-Id`, `X-Username`, `X-User-Role`) before forwarding requests downstream.
- Downstream services (like `user-service`) trust these headers completely, allowing us to **comment out local JWT filters** and avoid redundant database lookups for authentication.

### ⚙️ Custom Distributed Redis-Backed Circuit Breaker
- Standard Spring Cloud Gateway filters have been replaced by a custom reactive forwarding proxy (`ProxyFilter` + `ProxyService`) that integrates a **distributed Redis-backed circuit breaker state-machine**.
- Dynamically tracks service health (`CLOSED`, `OPEN`, `HALF_OPEN`) with a 5-failure threshold and a 60-second cooldown retry window.
- Provides immediate **Fail-Fast** `503 Service Unavailable` responses when a downstream service goes down, preventing cascading failures.
- Uptime health is dynamically synced to Redis to share state across multiple gateway instances.

### 🚀 Redis Caching with Java 8 Date/Time support
- The `user-service` integrates a **Cache-Aside / Lazy Loading** Redis caching system for user profiles (`GET /api/v1/user/profile`).
- Yields blazing-fast **8ms database-free cache hits** (down from 200ms DB queries).
- Custom serialized inside `RedisConfig.java` to support Java 8 `LocalDateTime` types using the `JavaTimeModule` module, preventing serialization crashes.
- Automatic cache eviction (`evict`) triggers instantly on profile updates (`PUT /api/v1/user/profile/update`).

### 📬 Kafka-Triggered SMTP Email Delivery
- The `notification-service` acts as a message consumer for async notification events (such as user welcome messages and registration OTPs) published to Apache Kafka.
- Features fully-typed event class deserialization to safely map event properties and dispatch transactional SMTP emails.

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

| Service | Port | Local Run Command | Health Check / Diagnosis Endpoint |
|---|---|---|---|
| **api-gateway** | `8080` | `mvn spring-boot:run` | [http://localhost:8080/api/v1/gateway/health](http://localhost:8080/api/v1/gateway/health) (Public diagnosis) |
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
