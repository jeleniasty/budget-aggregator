# Budget Aggregator

Spring Boot application to import and aggregate transaction data from CSV files.

## Features

- Import transactions asynchronously
- Aggregate stats by category, IBAN, and month

---

## Prerequisites

- Docker & Docker Compose
- Java 25
- Gradle (or use the Gradle wrapper included)

---

## 1. Clone the Repository

```bash
git clone https://github.com/jeleniasty/budget-aggregator
cd budget-aggregator
```

## 2. Run with Docker Compose

The project includes MongoDB replica set and your app configured for Docker:
```bash
docker compose up --build
```

- Spring Boot app will be available at http://localhost:8080
- MongoDB is accessible internally to the app as mongo:27017
- Swagger/OpenAPI docs: http://localhost:8080/swagger-ui/index.html

## 3. CSV Format

```csv
Bank,Reference number,IBAN,Date,Currency,Category,Transaction type,Amount
Global Bank,TXN-001,DE89370400440532013000,2026-02-03T19:15:00Z,EUR,Groceries,DEBIT,45.50
Digital Bank,TXN-002,GB29NWBK60161331926819,2026-02-01T12:54:33Z,GBP,Salary,CREDIT,3200.00
```

## 4. Test with Bruno
Project includes bruno API collection with predefined request examples.  
You can install Bruno https://www.usebruno.com/downloads, get collection from budget-aggregator directory and test API on your own. 