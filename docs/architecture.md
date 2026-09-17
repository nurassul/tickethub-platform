# 🏗️ System Architecture

This diagram illustrates the high-level architecture of the Tickethub platform.

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

## Key Architectural Decisions

1. **API Gateway:** Acts as the single entry point, routing requests and validating Keycloak JWT tokens.
2. **Polyglot Services:** Core business logic is in Java, but Payment processing uses Go for high performance and low memory footprint.
3. **Database per Service:** Strict microservice boundaries. No service can directly query another service's database.
4. **gRPC:** Used for synchronous, low-latency communication between Booking and Payment services.
5. **Event-Driven & Outbox Pattern:** Services communicate state changes asynchronously via Kafka. The Transactional Outbox pattern ensures local DB commits and Kafka events are strictly atomically consistent.
