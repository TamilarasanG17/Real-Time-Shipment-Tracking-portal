# 🚚 Infotact Shipment Tracker

**Project 3 — Real-Time Shipment Tracking Portal & Logistics Marketplace**  
Infotact Technical Internship Program · Bengaluru, Karnataka

---

## Overview

A full-stack marketplace where **Shippers** post freight loads, **Carriers** bid on them, and all parties track shipments live on an interactive map via **WebSocket (STOMP over SockJS)**.

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2, Spring Security (JWT), Spring WebSocket |
| Database | PostgreSQL (production), H2 (tests) |
| Real-Time | STOMP over SockJS, SimpMessagingTemplate |
| API Docs | Springdoc OpenAPI 2.x (Swagger UI) |
| Frontend | React 18, react-leaflet, @stomp/stompjs, Axios |
| CI/CD | GitHub Actions |

---

## WebSocket Architecture

```
Simulated Driver App
POST /api/tracking/{id}/location
          │
    TrackingController
          │
    TrackingService
          │
    SimpMessagingTemplate.convertAndSend("/topic/shipment/{id}", broadcast)
          │
    In-Memory STOMP Broker
          │
    React Dashboard (subscribed via SockJS + @stomp/stompjs)
    → map marker updates in real time
```

**STOMP Endpoint:** `ws://localhost:8080/ws`  
**Topic format:** `/topic/shipment/{shipmentId}`  
**Subscription (React):**
```javascript
stompClient.subscribe(`/topic/shipment/${shipmentId}`, (message) => {
  const { latitude, longitude, status, timestamp } = JSON.parse(message.body);
  // update Leaflet marker
});
```

---

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL 15+
- Node.js 18+ (for React frontend and driver simulator)

### 1. Clone and configure environment

```bash
git clone https://github.com/your-username/shipment-tracker.git
cd shipment-tracker

# Copy env template and fill in your values
cp .env.example .env
```

### 2. Create the PostgreSQL database

```sql
CREATE DATABASE shipment_tracker_db;
```

### 3. Start the Spring Boot backend

```bash
export DB_URL=jdbc:postgresql://localhost:5432/shipment_tracker_db
export DB_USERNAME=your_user
export DB_PASSWORD=your_password
export JWT_SECRET=your_256bit_secret

mvn spring-boot:run
```

Backend runs on **http://localhost:8080**

### 4. Start the React frontend

```bash
cd frontend
npm install
npm start
```

Frontend runs on **http://localhost:3000**

### 5. Run the GPS driver simulator

```bash
# First login as a carrier and copy the JWT token
node scripts/simulate-driver.js <shipmentId> <carrierJwtToken>
```

---

## API Documentation (Swagger UI)

Interactive docs available at:

```
http://localhost:8080/swagger-ui.html
```

1. Register via `POST /api/auth/register`
2. Login via `POST /api/auth/login` → copy the `token`
3. Click **Authorize** in Swagger UI → paste the token
4. All endpoints are now accessible from the browser

---

## Endpoints Summary

### Authentication (Public)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register SHIPPER or CARRIER |
| POST | `/api/auth/login` | Login → receive JWT |

### Shipments
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/shipments` | SHIPPER | Post a new freight load |
| GET | `/api/shipments/open` | CARRIER | Browse the load board |
| GET | `/api/shipments/mine` | SHIPPER | My posted loads |
| GET | `/api/shipments/{id}` | Any | Single shipment |

### Bids
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/shipments/{id}/bids` | CARRIER | Submit a bid |
| GET | `/api/shipments/{id}/bids` | SHIPPER | View bids on my shipment |
| POST | `/api/shipments/{id}/bids/{bidId}/award` | SHIPPER | Award bid (atomic) |
| GET | `/api/bids/mine` | CARRIER | My submitted bids |

### Tracking (REST + WebSocket)
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/tracking/{id}/pickup` | CARRIER | AWAITING_PICKUP → IN_TRANSIT |
| POST | `/api/tracking/{id}/delivery` | CARRIER | IN_TRANSIT → DELIVERED |
| POST | `/api/tracking/{id}/location` | CARRIER | Send GPS ping → WebSocket broadcast |
| GET | `/api/tracking/{id}/history` | Any | All GPS pings (route replay) |
| GET | `/api/tracking/{id}/latest` | Any | Latest GPS ping |

---

## Shipment Lifecycle

```
OPEN → AWAITING_PICKUP → IN_TRANSIT → DELIVERED
         (bid awarded)    (pickup)     (delivery)
```

---

## Running Tests

```bash
# Unit tests (no DB required)
mvn test

# Integration tests use H2 in-memory DB via @ActiveProfiles("test")
# All tests run automatically in CI on every Pull Request
```

---

## GitHub Workflow

- All work happens on **feature branches** — direct commits to `main` are forbidden
- Every PR triggers the GitHub Actions CI pipeline (`mvn clean test`)
- PRs must pass CI before merging
- Sensitive values (DB password, JWT secret) are stored in **GitHub Secrets**, never in code
- Commit messages follow semantic convention: `feat:`, `fix:`, `refactor:`, `test:`

---

## Project Structure

```
shipment-tracker/
├── .github/workflows/ci.yml         CI pipeline
├── scripts/
│   ├── simulate-driver.js            GPS simulator (Node.js)
│   └── useShipmentTracking.js        React WebSocket hook (reference)
├── frontend/                         React application
│   └── src/
│       ├── api/                      Axios client + API functions
│       ├── hooks/                    useShipmentTracking
│       ├── pages/                    LoginPage, ShipperDashboard, CarrierDashboard
│       └── components/               TrackingMap (Leaflet)
├── src/main/java/.../
│   ├── config/                       Security, WebSocket, CORS, OpenAPI
│   ├── controller/                   Auth, Shipment, Bid, Tracking
│   ├── dto/                          Request/Response DTOs
│   ├── entity/                       User, Shipment, Bid, GpsLocation
│   ├── enums/                        Role, ShipmentStatus
│   ├── exception/                    Custom exceptions + GlobalExceptionHandler
│   ├── repository/                   Spring Data JPA DAOs
│   ├── security/                     JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
│   └── service/                      Auth, Shipment, Bid, Tracking
├── src/test/                         Unit + integration tests
├── .env.example                      Environment variable template
└── pom.xml
```

---
