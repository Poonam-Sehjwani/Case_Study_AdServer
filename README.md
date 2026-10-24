# Retail Streaming Analytics Platform

A real-time streaming platform that ingests customer interaction and ad
engagement events from retailer websites, processes them asynchronously, and
exposes aggregated insights through REST APIs for marketers and retailers.

This repository contains the **code-design slice** of a larger architecture:
a single, fully working vertical of the platform (ingest → stream → store →
serve). The broader system design (storage tiers, stream processing at scale,
scaling, cost) is covered in the accompanying design document and diagram.

---

## What it does

- Accepts retail events (`PRODUCT_VIEW`, `ADD_TO_BASKET`, `PURCHASE`,
  `AD_IMPRESSION`, `AD_CLICK`) over a REST endpoint.
- Publishes each event to Kafka and processes it asynchronously.
- Tracks ad-click sessions and attributes basket additions to a preceding
  ad click within a 30-minute window (stateful session tracking).
- Maintains per-campaign aggregate metrics and exposes them via APIs.
- Enforces multi-tenancy: every event and query is scoped to a `retailerId`,
  and a client may only access retailers it is authorized for.

---

## Tech stack

| Concern            | Technology                                   |
|--------------------|----------------------------------------------|
| Language / runtime | Java 21                                      |
| Framework          | Spring Boot 4.1.1 (Web MVC)                  |
| Messaging          | Apache Kafka (Spring for Apache Kafka)       |
| Database           | PostgreSQL                                   |
| Migrations         | Flyway                                       |
| Security           | OAuth2 Resource Server (JWT, Auth0)          |
| Observability      | Spring Boot Actuator + Micrometer/Prometheus |
| Build              | Maven (wrapper included)                      |

---

## Architecture (implemented slice)

```
Client ──POST /api/v1/events──> EventIngestionController
                                      │ (JWT auth + tenant check)
                                      ▼
                            EventIngestionService
                                      │
                                      ▼
                            Kafka topic: retail-events
                                      │
                                      ▼
                            EventConsumerService  (group: retail-event-processors)
                                      │
                                      ▼
                          CampaignProcessingService
                           ├─ idempotency check (retailer + eventId)
                           ├─ click-session tracking (30-min attribution window)
                           └─ campaign metric aggregation (optimistic locking)
                                      │
                                      ▼
                               PostgreSQL
                     (processed_events, click_sessions, campaign_metrics)
                                      ▲
                                      │
Client ──GET /ad/{id}/...──> AdAnalyticsController ─> AdAnalyticsService
```

Failed message processing is retried (3 attempts, 1s backoff) and routed to a
`retail-events.DLT` dead-letter topic on exhaustion.

---

## Prerequisites

- **Java 21** (JDK)
- **Docker** (to run Kafka via the provided `docker-compose.yml`)
- **PostgreSQL** running locally on port `5432`
- An **Auth0** tenant (or any OAuth2 provider) issuing JWTs, to call the
  secured endpoints

> The Maven wrapper (`mvnw` / `mvnw.cmd`) is included, so a separate Maven
> installation is not required.

---

## Setup

### 1. Start Kafka

```bash
docker compose up -d
```

This starts a single-node Kafka broker (KRaft mode) on `localhost:9092` with
3 default partitions.

### 2. Create the PostgreSQL database

The application uses `spring.jpa.hibernate.ddl-auto=validate`, so the database
must exist before startup; Flyway then creates and migrates the tables.

```sql
CREATE DATABASE retail_analytics;
```

Default connection settings (see `src/main/resources/application.properties`):

- URL: `jdbc:postgresql://localhost:5432/retail_analytics`
- Username: `postgres`
- Password: configured in `application.properties`

> **Security note:** the datasource password is currently set in
> `application.properties`. For any shared or public repository, override it
> with an environment variable instead of committing a real password, e.g.:
>
> ```properties
> spring.datasource.password=${DB_PASSWORD}
> ```
>
> then set `DB_PASSWORD` in your environment before running.

### 3. Configure JWT validation

Set the issuer and audience for your OAuth2 provider in
`application.properties`:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=<your-issuer-uri>
spring.security.oauth2.resourceserver.jwt.audiences=<your-api-audience>
```

### 4. Seed tenant authorization

A client can only access a retailer that is linked in the
`client_retailer_access` table. Insert a row mapping your JWT client id
(the `client_id` or `azp` claim) to a retailer:

```sql
INSERT INTO client_retailer_access (client_id, retailer_id, enabled)
VALUES ('<your-client-id>', '<your-retailer-id>', true);
```

---

## Running the application

From the project root:

```bash
# Windows
mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

Flyway applies the migrations in `src/main/resources/db/migration` on startup.
The application starts on **http://localhost:8001**.

### Build a JAR

```bash
mvnw.cmd clean package
java -jar target/retail-platform-0.0.1-SNAPSHOT.jar
```

---

## API reference

All endpoints require:

- `Authorization: Bearer <JWT>`
- `X-Retailer-Id: <retailerId>` — must be a retailer the authenticated client
  is authorized for.

### Event ingestion

```
POST /api/v1/events
Content-Type: application/json

{
  "eventId": "evt-123",
  "eventType": "AD_CLICK",
  "eventTime": "2026-10-24T10:15:30Z",
  "retailerId": "retailer-1",
  "sessionId": "session-abc",
  "anonymousCustomerId": "cust-xyz",
  "productId": "prod-1",
  "campaignId": "camp-1",
  "adId": "ad-1"
}
```

The `retailerId` in the body must match the `X-Retailer-Id` header.

### Ad insights

| Method | Path                               | Returns                                                        |
|--------|------------------------------------|----------------------------------------------------------------|
| GET    | `/ad/{campaignId}/clicks`          | Count of unique customers who clicked the ad                   |
| GET    | `/ad/{campaignId}/impressions`     | Count of unique customers who viewed the ad                    |
| GET    | `/ad/{campaignId}/clickToBasket`   | Count of unique customers who added to basket after a click    |

### Campaign metrics

| Method | Path                                  | Returns                                   |
|--------|---------------------------------------|-------------------------------------------|
| GET    | `/api/v1/campaigns/{campaignId}/metrics` | Aggregated impressions, clicks, basket additions, product views |

### Example

```bash
curl -H "Authorization: Bearer <JWT>" \
     -H "X-Retailer-Id: retailer-1" \
     http://localhost:8001/ad/camp-1/clicks
```

---

## Observability

Actuator endpoints (unauthenticated for health/metrics):

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
- `GET /actuator/prometheus` — Prometheus scrape endpoint

---

## Key design decisions

- **Asynchronous processing via Kafka** decouples ingestion latency from
  processing, so ingestion stays fast under load and processing can scale and
  retry independently.
- **Idempotency** is enforced per retailer (`retailer_id` + `event_id` unique
  constraint), so duplicate deliveries do not double-count.
- **Click-to-basket attribution** is stateful: an `AD_CLICK` is recorded in
  `click_sessions`, and a later `ADD_TO_BASKET` in the same session within a
  30-minute window is attributed to the clicked campaign.
- **Optimistic locking** (`@Version`) on `campaign_metrics` with bounded retry
  handles concurrent updates to the same campaign counter.
- **Multi-tenancy** is enforced at the data layer (all rows scoped by
  `retailer_id`) and the access layer (`client_retailer_access` authorization).
- **Resilience:** consumer retries with backoff and a dead-letter topic
  (`retail-events.DLT`) for poison messages.

> These are prototype-scale choices. The companion design document describes
> how each concern evolves at production scale (polyglot storage tiers,
> stream-processing framework for windowed joins, horizontal scaling and
> partitioning, cost monitoring).

---

## Project structure

```
src/main/java/com/retail/streaming/
├── config/        Kafka error handling + security (JWT) configuration
├── controller/    REST endpoints (ingestion, ad analytics, campaign metrics)
├── dto/           Request/response records and the event type enum
├── entity/        JPA entities (processed_events, click_sessions, campaign_metrics)
├── repository/    Spring Data repositories and analytics queries
├── services/      Ingestion, Kafka producer/consumer, processing, tenant auth
└── Exception/     Security exception handling

src/main/resources/
├── application.properties
└── db/migration/  Flyway SQL migrations (V1–V3)
```
