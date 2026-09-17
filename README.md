# 🎟️ Tickethub Platform

Tickethub is a modern, scalable, polyglot microservices platform for event management and ticket booking. It demonstrates advanced distributed systems patterns, including Event-Driven Architecture, Transactional Outbox, Distributed Locks, and comprehensive Observability.

## 🚀 Key Features

* **Microservices Architecture:** Independently deployable services with isolated databases.
* **Polyglot Stack:** Core services in **Java/Spring Boot 3**, with high-performance payment processing in **Go**.
* **Event-Driven:** Asynchronous communication via **Apache Kafka** using the **Transactional Outbox Pattern** to guarantee exactly-once message delivery.
* **Fast Inter-Service Comm:** Synchronous critical paths (like Booking to Payment) utilize **gRPC**.
* **Concurrency Control:** Distributed seat holds and locking via **Redis**.
* **Enterprise Observability:** Fully instrumented PLG/OTel stack (Prometheus, Loki, Grafana, Jaeger) with cross-language distributed tracing and log correlation.
* **Security:** Centralized identity and access management via **Keycloak** (OAuth2/OIDC).

## 🛠️ Technology Stack

| Category | Technologies |
| :--- | :--- |
| **Languages** | Java 21, Kotlin DSL, Go |
| **Frameworks** | Spring Boot 3, Gin, gRPC |
| **Databases & Cache** | PostgreSQL 15, Redis 7 |
| **Message Broker** | Apache Kafka (Confluent) |
| **Observability** | Prometheus, Grafana, Jaeger, Loki, Promtail, OpenTelemetry |
| **Infrastructure** | Docker, Docker Compose |

## 📐 Architecture & Documentation

To keep this README clean, detailed architectural diagrams and flows are separated into their own documents. We use Mermaid.js for version-controlled, beautifully rendered diagrams right here in GitHub.

* 🏗️ **[System Architecture](docs/architecture.md)** — High-level overview of services, databases, and infrastructure.
* 🔄 **[Booking & Payment Flow](docs/booking-flow.md)** — Detailed sequence diagram of the core ticketing process.

## 🚦 Getting Started

### Prerequisites
* Docker & Docker Compose
* Java 21+ & Go 1.21+ (for local development without Docker)

### Running the Platform

1. **Clone the repository:**
   ```bash
   git clone https://github.com/nurassul/tickethub-platform.git
   cd tickethub-platform
   ```

2. **Start the infrastructure and services:**
   ```bash
   docker-compose up -d --build
   ```
   *Note: This will start PostgreSQL, Redis, Kafka, Keycloak, all Microservices, and the Observability stack.*

### Exposing Ports

| Service | Port | Description |
| :--- | :--- | :--- |
| **API Gateway** | `8080` | Main entrypoint for all client requests |
| **Keycloak** | `8180` | IAM and Authentication |
| **Grafana** | `3000` | Observability Dashboards (`admin`/`admin`) |
| **Jaeger UI** | `16686` | Distributed Tracing UI |
| **Prometheus** | `9091` | Metrics storage |

## 🧪 Testing
*(Tests are currently under development. Contributions are welcome!)*

---
*Developed as an advanced educational project to master distributed systems.*
