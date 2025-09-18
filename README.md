# Coupon Service - Assignment

## Overview
This assignment implements a backend service for managing and applying discount coupons in an e-commerce setting.
It is built using **Java 17**, **Spring Boot**, **H2 (in-memory DB for local/testing)**, **JPA**, **Lombok**, and **JUnit 5 + Mockito** for unit tests.


The service supports:
- CRUD operations for coupons
- Multiple coupon types:
  - **Cart-wise** (threshold, percent/flat discount)
  - **Product-wise** (specific product, percent/flat discount)
  - **BxGy** (Buy X Get Y)
- Endpoints to evaluate and apply coupons:
  - `POST /api/applicable-coupons`
  - `POST /api/apply-coupon/{id}`

---

## Assumptions
- One coupon applied at a time (no stacking).
- Cart items contain `productId`, `quantity`, `price`.
- `details` JSON is stored as a string (validated and parsed per coupon type).
- Currency handled with BigDecimal (scale=6 internal, scale=2 comparisons).
- BxGy behavior: greedy, deterministic. Buy-products aggregated across all buy definitions. Get-products chosen in defined order.
- Expired or inactive coupons are skipped.
- Validation is strict: missing fields in `details` reject the coupon.

---

## Implemented Features
- Coupon CRUD (`/api/coupons`)
- Validation for each coupon type (`CartWiseDetailsDto`, `ProductWiseDetailsDto`, `BxGyDetailsDto`)
- Evaluation logic:
  - Cart-wise: threshold check, discount applied to total.
  - Product-wise: discount applied only to matching product.
  - BxGy: supports repetition limit, ensures available get-products.
- Apply logic:
  - Cart-wise: discount distributed proportionally across items.
  - Product-wise: discount applied per unit/percent.
  - BxGy: eligible get-products marked with free-unit discounts.
- Unit tests (JUnit + Mockito) covering:
  - Controller-level tests for Coupon CRUD APIs.
  - Service-level tests for core coupon evaluation and apply logic.
  - Application startup test to verify Spring Boot context loads.

---

## Not Implemented / Deferred
- Coupon stacking or combining multiple coupons
- Querying inside coupon details JSON at DB level (currently stored as text)
- Integration tests with real DB and controllers (unit tests only as per assignment)
- Admin/auth layer (open endpoints for simplicity)
- Tax/shipping adjustments

---

## API Endpoints

### Coupon CRUD

#### Create Coupon
`POST /api/coupons`  
Request:
```json
{
  "code":"SAVE10",
  "type":"CART",
  "details":"{\"threshold\":100, \"discount\":10}",
  "active":true,
  "expiresAt":"2025-12-31T23:59:59Z"
}
```
Response (`201 Created`):
```json
{
  "id":"abc123",
  "code":"SAVE10",
  "type":"CART",
  "details":"{\"threshold\":100, \"discount\":10}",
  "active":true,
  "expiresAt":"2025-12-31T23:59:59Z"
}
```

#### List Coupons
`GET /api/coupons`  
Response:
```json
[
  {
    "id":"abc123",
    "code":"SAVE10",
    "type":"CART",
    "details":"{\"threshold\":100, \"discount\":10}",
    "active":true,
    "expiresAt":"2025-12-31T23:59:59Z"
  }
]
```

#### Get Coupon by ID
`GET /api/coupons/{id}`

#### Update Coupon
`PUT /api/coupons/{id}`

#### Delete Coupon
`DELETE /api/coupons/{id}`

---

### Applicable Coupons
`POST /api/applicable-coupons`  
Request:
```json
{
  "items": [
    {"productId": 1, "quantity": 6, "price": 50},
    {"productId": 2, "quantity": 3, "price": 30},
    {"productId": 3, "quantity": 2, "price": 25}
  ]
}
```
Response:
```json
{
  "applicable_coupons": [
    {
      "coupon_id": "abc123",
      "code": "CART10",
      "type": "CART",
      "discount": 25.00
    }
  ]
}
```

### Apply Coupon
`POST /api/apply-coupon/{id}`  
Request:
```json
{
  "items": [
    {"productId": 1, "quantity": 2, "price": 100},
    {"productId": 2, "quantity": 1, "price": 50}
  ]
}
```
Response:
```json
{
  "updated_cart": {
    "items": [
      {"productId": 1, "quantity": 2, "price": 100, "totalDiscount": 20},
      {"productId": 2, "quantity": 1, "price": 50, "totalDiscount": 5}
    ],
    "total_price": 250.00,
    "total_discount": 25.00,
    "final_price": 225.00
  }
}
```

---

## How to Run

### Requirements
- Java 17+
- Maven is not required (Maven wrapper `mvnw` is included)
- (No DB required — H2 in-memory is used for local runs and tests)

### Steps
```bash
# clone repo
git clone <repo-url>
cd CouponService

# build & run tests
./mvnw clean test

# run locally
./mvnw spring-boot:run                                      # for Linux
./mvnw spring-boot:run -Dspring-boot.run.profiles=local     # for Mac
```

### Run with H2 (in-memory DB)
This uses `src/main/resources/application-local.properties`.

```bash
java -jar target/CouponService-0.0.1-SNAPSHOT.jar --spring.profiles.active=local

```

Default server port: `8080`. Override with `--server.port=XXXX`.

---

## Unit Tests
Run:
```bash
./mvnw test
```
  Tests include:
- `CouponControllerTest` – controller layer (MockMvc) test for creating a coupon (`POST /api/coupons`).
- `CouponServiceTest` – service-level unit tests for coupon evaluation logic:
    - Cart-wise percent discount
    - Product-wise percent discount
    - Basic BxGy evaluation with repetition limit
- `CouponServiceApplicationTests` – verifies that the Spring Boot application context loads successfully.

These tests use JUnit 5 and Mockito with H2 for in-memory data.


---

### Usage
1. Start the app (e.g. with H2 in-memory DB, see above).
2. Set environment variable `baseUrl` to `http://localhost:8080`.  
3. Run requests in order:  
   - Create coupons (CART, PRODUCT, BXGY)  
   - List coupons  
   - `POST /api/applicable-coupons`  
   - `POST /api/apply-coupon/{id}`

---

## Future Improvements
- Store details as JSONB in Postgres for querying/indexing.
- Add API auth (JWT, roles).
- Support coupon stacking with conflict resolution.
- Add integration tests with H2 and MockMvc.
- Observability: logs, metrics (applied/failed coupons counters).
- More coupon types (category-based, first-order, etc).

---

## Author
**Anuradha Belgaonkar**
