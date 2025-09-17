# Coupon Service - Assignment

## Overview
This project implements a backend service for managing and applying discount coupons in an e-commerce setting.  
It was built using **Java 17, Spring Boot, PostgreSQL (or H2 for testing), JPA, Lombok**, and **JUnit 5 + Mockito** for unit tests.

The service supports:
- CRUD operations for coupons.
- Multiple coupon types:
  - **Cart-wise** (threshold, percent/flat discount).
  - **Product-wise** (specific product, percent/flat discount).
  - **BxGy** (Buy X Get Y).
- Endpoints to evaluate and apply coupons:
  - `POST /api/applicable-coupons`
  - `POST /api/apply-coupon/{id}`

---

## Assumptions
- One coupon applied at a time (no stacking).
- Cart items contain `productId`, `quantity`, `price`.
- `details` JSON is stored as a string (validated and parsed per coupon type).
- Currency is handled with `BigDecimal`, internal scale = 6, final comparisons scale = 2.
- BxGy behavior: greedy, deterministic. Buy-products aggregated across all buy definitions. Get-products chosen in order defined.
- Expired or inactive coupons are skipped.
- Validation is strict: missing fields in `details` JSON reject the coupon.

---

## Implemented Features
- Coupon CRUD (`/api/coupons`).
- Validation for each coupon type (`CartWiseDetailsDto`, `ProductWiseDetailsDto`, `BxGyDetailsDto`).
- Evaluation logic:
  - **Cart-wise**: threshold check, discount applied to total.
  - **Product-wise**: discount applied only to matching product.
  - **BxGy**: supports repetition limit, ensures available get-products.
- Apply logic:
  - For cart coupons: discount distributed proportionally across items.
  - For product coupons: discount applied per unit/percent.
  - For BxGy coupons: eligible get-products marked with free-unit discounts.
- Unit tests (JUnit + Mockito) covering:
  - Validation failures.
  - Expired/inactive coupons.
  - Cart-wise, product-wise, and BxGy evaluation.
  - Apply logic distribution & rounding.
  - Edge cases (repetition limit, insufficient get-products, mixed buy-products).

---

## Not Implemented / Deferred
- Coupon stacking or combining multiple coupons.
- Querying inside coupon `details` JSON at DB level (currently just stored as text).
- Integration tests with real DB and controllers (assignment required only unit tests).
- Admin/auth layer (open endpoints for simplicity).
- Tax/shipping adjustments.

---

## API Endpoints

### Coupon CRUD
- `POST /api/coupons`
- `GET /api/coupons`
- `GET /api/coupons/{id}`
- `PUT /api/coupons/{id}`
- `DELETE /api/coupons/{id}`

### Apply & Applicable
- `POST /api/applicable-coupons`  
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
    "applicable_coupons": [
      {
        "coupon_id": "abc123",
        "code": "CART10",
        "type": "CART",
        "discount": 25
      }
    ]
  }
  ```

- `POST /api/apply-coupon/{id}`  
  Request: same as above.  
  Response:
  ```json
  {
    "updated_cart": {
      "items": [
        {"productId": 1, "quantity": 2, "price": 100, "totalDiscount": 20},
        {"productId": 2, "quantity": 1, "price": 50, "totalDiscount": 5}
      ],
      "total_price": 250,
      "total_discount": 25,
      "final_price": 225
    }
  }
  ```

---

## How to Run

### Requirements
- Java 17+
- Maven 3+
- PostgreSQL (optional, H2 used for tests)

### Steps
```bash
# clone repo
git clone <repo-url>
cd coupon-service

# build & run tests
mvn clean test

# run locally
mvn spring-boot:run
```

Default server port: **8080**.  

Use Postman or curl to hit endpoints.

---

## Unit Tests
Run:
```bash
mvn test
```

Tests include:
- `CouponServiceTest` – evaluation logic.
- `CouponApplyTest` – apply logic distribution.
- `CouponValidationTest` – validation failures.
- `CouponExpiryTest` – inactive/expired coupons.
- `BxGyEdgeCasesTest` – repetition, insufficient get-products, mixed buy-products.
- `ApplyDistributionTest` – cart-wise proportional, product-wise percent/flat.

---

## Postman Collection
A ready-to-import Postman collection is provided in the postman/ folder:

- File: postman/CouponService_Postman_Collection_With_Tests.json

## How to use

- Start the app (example using H2 in-memory DB):

```json
java -jar target/CouponService-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE \
  --spring.datasource.driver-class-name=org.h2.Driver \
  --spring.datasource.username=sa \
  --spring.datasource.password= \
  --spring.jpa.hibernate.ddl-auto=create-drop
```
- Import the Postman collection into Postman.

- Set the environment variable baseUrl to http://localhost:8080.

- Run requests in order:

-- Create coupons (CART, PRODUCT, BXGY).

--List coupons.

-- POST /api/applicable-coupons to see applicable coupons.

-- POST /api/apply-coupon/{id} to apply a coupon.

The collection also includes Postman tests that check status codes, IDs, and discount application.
---

## Future Improvements
- Store `details` as JSONB in Postgres for querying and indexing.
- Add API auth (JWT, roles).
- Support coupon stacking with conflict resolution.
- Add integration tests with H2 and MockMvc.
- Observability: logs, metrics (applied/failed coupons counters).
- More coupon types (category-based, first-order, etc).

---

## Author **Anuradha Belgaonkar**  

