# KidBank API

Spring Boot REST API for the kids finance app.

## Stack
- Java 21
- Spring Boot 3.2
- Spring Web + Spring Data JPA
- PostgreSQL (prod) / H2 (tests)

---

## Local Development

### Prerequisites
- Java 21
- Docker (for Postgres)

### Run Postgres locally
```bash
docker run --name kidbank-db \
  -e POSTGRES_DB=kidbank \
  -e POSTGRES_USER=kidbank \
  -e POSTGRES_PASSWORD=kidbank \
  -p 5432:5432 \
  -d postgres:16
```

### Run the app
```bash
./mvnw spring-boot:run
```

### Run tests
```bash
./mvnw test
```

---

## API Reference

### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create a child user |
| GET | `/api/users/{id}` | Get user with balances |

**POST /api/users**
```json
{ "name": "יובל", "username": "yuval" }
```

**Response**
```json
{
  "id": 1,
  "name": "יובל",
  "checkingBalance": 0,
  "depositTotal": 0,
  "totalBalance": 0
}
```

---

### Transactions
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users/{id}/transactions` | Add transaction |
| GET | `/api/users/{id}/transactions?range=month` | Get history |
| GET | `/api/users/{id}/transactions/summary?range=month` | Income/expense summary |

**range values:** `month` · `half` · `year` · `all` (default)

**POST /api/users/{id}/transactions**
```json
{
  "type": "INCOME",
  "amount": 50.00,
  "description": "דמי כיס"
}
```
**Types:** `INCOME` · `EXPENSE` · `DEPOSIT_IN` · `DEPOSIT_OUT`

---

### Deposit
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/{id}/deposit` | Get deposit + projections |
| POST | `/api/users/{id}/deposit/add` | Add money to deposit |
| POST | `/api/users/{id}/deposit/withdraw` | Withdraw from deposit |
| PUT | `/api/users/{id}/deposit/interest-rate` | Set interest rate (parent) |

**POST /api/users/{id}/deposit/add**
```json
{ "amount": 80.00 }
```

**Response**
```json
{
  "totalAmount": 80.00,
  "interestRate": 0.12,
  "projectedOneMonth": 80.80,
  "projectedSixMonths": 84.80,
  "projectedOneYear": 89.60
}
```

**PUT /api/users/{id}/deposit/interest-rate** *(parent only)*
```json
{ "interestRate": 0.08 }
```
Value must be between 0.0 and 1.0 (e.g. 0.12 = 12%)

---

## Deploy to Render.com (free)

1. Push to GitHub
2. New → Web Service → connect repo
3. Build command: `./mvnw package -DskipTests`
4. Start command: `java -jar target/kidbank-0.0.1-SNAPSHOT.jar`
5. Add environment variables:
   ```
   DATABASE_URL=jdbc:postgresql://<supabase-host>:5432/postgres
   DATABASE_USER=postgres
   DATABASE_PASSWORD=<your-password>
   ```

## Supabase (free Postgres)

1. Create project at supabase.com
2. Settings → Database → Connection string (JDBC)
3. Paste as `DATABASE_URL` in Render

## UptimeRobot (keep-alive)

1. Create free account at uptimerobot.com
2. Add HTTP monitor → your Render URL → every 5 minutes
3. The server stays warm indefinitely
