<div align="center">
  <h1>🎟️ Tickethub Platform</h1>
  <p><b>A modern, scalable, and fully observable microservices platform for event management and ticket booking.</b></p>

  <!-- Tech Stack Badges -->
  <img src="https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/Go_1.21-00ADD8?style=for-the-badge&logo=go&logoColor=white" alt="Go" />
  <img src="https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring" />
  <img src="https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white" alt="Kafka" />
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
  <br/>
  <img src="https://img.shields.io/badge/gRPC-244c5a?style=for-the-badge&logo=grpc&logoColor=white" alt="gRPC" />
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white" alt="Grafana" />
  <img src="https://img.shields.io/badge/Jaeger-60C0A8?style=for-the-badge&logo=jaeger&logoColor=white" alt="Jaeger" />
</div>

<br/>

## 🏗️ System Architecture

The platform is designed around strict microservice boundaries. Each service owns its data, and they communicate via synchronous **gRPC/REST** for critical paths, and asynchronous **Kafka** events for eventual consistency.

```mermaid
graph TD
    %% Define Styles
    classDef client fill:#2a9d8f,stroke:#21867a,stroke-width:2px,color:#fff,font-weight:bold
    classDef gateway fill:#e9c46a,stroke:#d4a373,stroke-width:2px,color:#333,font-weight:bold
    classDef javaService fill:#e76f51,stroke:#d00000,stroke-width:2px,color:#fff
    classDef goService fill:#4ea8de,stroke:#0077b6,stroke-width:2px,color:#fff
    classDef db fill:#457b9d,stroke:#1d3557,stroke-width:2px,color:#fff,shape:cylinder
    classDef infra fill:#f4a261,stroke:#e76f51,stroke-width:2px,color:#fff

    %% Nodes
    Client([📱 Client / Web App]):::client
    Gateway[🚪 API Gateway<br/>Spring Cloud Gateway]:::gateway
    Keycloak[🔐 Keycloak<br/>Auth & IAM]:::infra

    subgraph Microservices
        Event[📅 Event Service<br/>Java / Spring]:::javaService
        Ticket[🎟️ Ticket Service<br/>Java / Spring]:::javaService
        Booking[🛒 Booking Service<br/>Java / Spring]:::javaService
        Payment[💳 Payment Service<br/>Go / Gin]:::goService
    end

    subgraph Data Layer
        EventDB[(Event DB<br/>Postgres)]:::db
        TicketDB[(Ticket DB<br/>Postgres)]:::db
        BookingDB[(Booking DB<br/>Postgres)]:::db
        PaymentDB[(Payment DB<br/>Postgres)]:::db
        Redis[(Redis<br/>Seat Locks)]:::db
    end

    Kafka((🔥 Apache Kafka<br/>Event Bus)):::infra

    %% Edges (Traffic)
    Client -->|REST| Gateway
    Client -.->|OAuth2 Token| Keycloak
    Gateway -->|Verify Token| Keycloak

    Gateway -->|REST| Event
    Gateway -->|REST| Ticket
    Gateway -->|REST| Booking

    Booking -- gRPC --> Payment
    Booking -->|REST / Feign| Event

    %% Edges (Database)
    Event --> EventDB
    Ticket --> TicketDB
    Booking --> BookingDB
    Payment --> PaymentDB
    Booking <--> Redis

    %% Edges (Async Events)
    Booking -.->|Outbox Pattern| Kafka
    Payment -.->|Outbox Pattern| Kafka
    Kafka -.->|Consume| Ticket
    Kafka -.->|Consume| Booking
```

---

## 🔄 Core Business Flow (Booking & Payment)

The most complex part of the system is guaranteeing that a user doesn't double-book a seat, and ensuring that a successful payment *always* generates a ticket. We achieve this using **Distributed Redis Locks** and the **Transactional Outbox Pattern**.

```mermaid
sequenceDiagram
    autonumber
    
    actor User
    participant Gateway as API Gateway
    participant Booking as Booking Service
    participant Event as Event Service
    participant Redis as Redis (Locks)
    participant Payment as Payment Service (Go)
    participant Kafka as Kafka Event Bus
    participant Ticket as Ticket Service

    User->>Gateway: POST /bookings (eventId, seatIds)
    Gateway->>Booking: Route Request
    
    %% Validation & Locking
    rect rgb(240, 248, 255)
        Note right of Booking: 1. Validate & Distributed Lock
        Booking->>Event: GET /events/{id}/seats (Validate availability)
        Event-->>Booking: Seats Validated
        Booking->>Redis: TryHoldSeats(seatIds, TTL: 15m)
        alt Seats already held
            Redis-->>Booking: Lock Failed
            Booking-->>User: 409 Conflict (Seats taken)
        else Lock Acquired
            Redis-->>Booking: Success
        end
    end

    %% Booking Creation
    rect rgb(245, 255, 245)
        Note right of Booking: 2. Save Booking & gRPC Call
        Booking->>Booking DB: Save Booking (Status: PENDING)
        Booking->>Payment: gRPC: CreatePayment(Amount, IdempotencyKey)
        Payment->>Payment DB: Save Payment (Status: PENDING)
        Payment-->>Booking: Return Payment URL & ID
        Booking-->>User: 201 Created (Payment URL)
    end

    %% Payment Processing (Async)
    User->>Payment: Redirect to Payment URL (Mock Checkout)
    Payment->>Payment DB: Mark Payment as SUCCEEDED
    
    rect rgb(255, 245, 245)
        Note right of Payment: 3. Outbox Pattern (Guarantee Delivery)
        Payment->>Payment DB: Insert into outbox_events (PaymentSucceeded)
        Note over Payment,Kafka: Async Relay picks up Outbox event
        Payment-)Kafka: Publish tickethub.payment.events.v1
    end

    %% Async Resolution
    Kafka-)Booking: Consume PaymentSucceeded Event
    Booking->>Booking DB: Update Booking (Status: CONFIRMED)
    Booking->>Redis: MarkSeatsSold(seatIds)
    Booking->>Booking DB: Insert into outbox_events (BookingConfirmed)
    Booking-)Kafka: Publish tickethub.booking.events.v1

    Kafka-)Ticket: Consume BookingConfirmed Event
    Ticket->>Ticket DB: Generate Tickets (PDFs/QRs)
    Note right of Ticket: Tickets are now ready for the user!
```

---

## ✨ Key Architectural Patterns

* **Polyglot Ecosystem:** Core services are written in **Java (Spring Boot)** for rapid domain modeling, while the `payment-service` is written in **Go** to handle high-throughput, low-latency financial transactions.
* **Transactional Outbox:** Instead of dual-writing to the database and Kafka (which causes race conditions), services write events to an `outbox_events` table in the same ACID transaction as the business entity. A background relay then pushes these events to Kafka ensuring exactly-once/at-least-once delivery.
* **Distributed Locking:** Redis is used via `SeatHoldService` to prevent double-booking of seats across multiple instances of the booking service.
* **Full Observability (PLG + OTel):** 
  * 📈 **Prometheus & Grafana:** Hardware and application metrics (Micrometer/Go metrics).
  * 🕵️ **Jaeger (OpenTelemetry):** Distributed tracing across Java and Go boundaries via gRPC metadata and HTTP headers.
  * 📜 **Loki & Promtail:** Centralized logging with Trace ID injection (Log Correlation).

---

## 🚦 Getting Started

### Prerequisites
* Docker & Docker Compose
* Ports `8080`, `8180`, `3000`, `9091`, and `16686` must be free.

### Running the Platform

1. **Clone the repository:**
   ```bash
   git clone https://github.com/nurassul/tickethub-platform.git
   cd tickethub-platform
   ```

2. **Start the infrastructure and services:**
   ```bash
   # Add --build to compile the latest Java/Go code
   docker-compose up -d --build
   ```

3. **Access the UIs:**
   * **API Gateway:** `http://localhost:8080`
   * **Grafana (Metrics & Logs):** `http://localhost:3000` *(Login: admin / admin)*
   * **Jaeger (Distributed Tracing):** `http://localhost:16686`
   * **Prometheus:** `http://localhost:9091`

## 📁 Directory Structure
```text
tickethub-platform/
├── api-gateway/       # Spring Cloud Gateway (Entrypoint)
├── booking-service/   # Java 21 / Spring Boot (Booking Domain)
├── event-service/     # Java 21 / Spring Boot (Events & Seats Domain)
├── payment-service/   # Go 1.21 / Gin / gRPC (Payment Domain)
├── ticket-service/    # Java 21 / Spring Boot (Ticket Generation)
├── contracts/         # gRPC Protobuf definitions (.proto)
├── db-init/           # PostgreSQL initialization scripts
├── prometheus/        # Observability configurations
└── docker-compose.yml # Main infrastructure file
```
