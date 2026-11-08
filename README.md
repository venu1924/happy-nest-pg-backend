# Happy Nest PG — Backend (Spring Boot)

REST API for the Happy Nest PG Management frontend.

**Location:** `C:\Users\duggi\happy-nest-pg-backend`

## Stack

- Java 17+
- Spring Boot 3.4
- Spring Web
- Spring Data JPA
- PostgreSQL
- Lombok
- Validation

## Building model (matches frontend)

| Floor | Rooms | Notes |
|-------|--------|--------|
| 1–5 | 101–112 … 501–512 | 100–500 series |
| …01 | Single | 1 bed |
| …02–…11 | Double | 2 beds. Can be single-sharing (both beds, one person) or triple-sharing (Bed C added, up to 3 people) |
| …12 | Triple | 3 beds; optional **2 members only** (Bed C unused, full at 2) |

Starts with **all rooms vacant** (seeded on first run).

## 1. Create PostgreSQL database

```sql
CREATE DATABASE happy_nest;
```

Edit `src/main/resources/application.properties` if needed:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/happy_nest
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

## 2. Run the backend

### IntelliJ IDEA (recommended)

1. **File → Open** → `C:\Users\duggi\happy-nest-pg-backend`
2. Wait for Maven import
3. Open `HappyNestPgApplication.java` → Run

### Or Maven

```bash
cd C:\Users\duggi\happy-nest-pg-backend
mvn spring-boot:run
```

API base: **http://localhost:8080/api**

Quick check: http://localhost:8080/api/floors

## 3. Connect the frontend

In `C:\Users\duggi\happy-nest-pg\.env`:

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://localhost:8080/api
```

Restart Vite:

```bash
cd C:\Users\duggi\happy-nest-pg
npm run dev
```

Profile page should show **Data mode: API**.

## API endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/floors` | Building floors + rooms + beds |
| GET | `/api/floors/{n}` | One floor |
| GET | `/api/rooms` | All rooms |
| GET | `/api/rooms/{roomNumber}` | One room |
| GET | `/api/residents` | Current residents |
| GET | `/api/residents/past` | Past residents |
| GET | `/api/residents/{id}` | Resident details |
| POST | `/api/residents` | Add resident |
| PUT | `/api/residents/{id}` | Update details |
| DELETE | `/api/residents/{id}` | Delete (optional body `{ "notes": "..." }`) |
| POST | `/api/residents/{id}/transfer` | Room transfer |
| GET | `/api/transfers` | Transfer history |
| GET | `/api/payments?month=&year=` | Payments for month |
| GET | `/api/payments/{id}` | One payment |
| PUT | `/api/payments/{id}/mark-paid` | Record Cash / UPI / Card |
| GET | `/api/dashboard` | Monthly residents, temporary residents, vacant beds, vacant beds (P+T), payment summary |
| GET | `/api/temporary-residents` | Few-day stays |
| POST | `/api/temporary-residents` | Add a temporary stay |
| GET | `/api/temporary-residents/{id}` | One temporary stay (days left, vacate today) |
| PUT | `/api/temporary-residents/{id}/extend` | Add extra days (`extraDays`) |
| PUT | `/api/temporary-residents/{id}/mark-paid` | Record temporary payment |
| DELETE | `/api/temporary-residents/{id}` | Remove temporary stay and free the bed |
| GET | `/api/admin/me` | Current admin |

CORS is enabled for `http://localhost:5173`.

## Login (fixed credentials)

| Field | Value |
|-------|--------|
| Username | `admin` |
| Password | `happynest@602` |

Change in `application.properties`:

```properties
app.auth.username=admin
app.auth.password=happynest@602
```

- `POST /api/auth/login` — public
- All other `/api/**` routes need `Authorization: Bearer <token>`

## Notes

- Payments are **record keeping only** (Cash / UPI / Card). No payment gateway.
- Opening a month creates a **Pending** rent row for every monthly resident already staying, unless their joining date is after that month. Past months are left as recorded.
- `refundAmount` is the amount returned on leaving (`0` if none). `depositRefundable` follows that amount.
- Temporary stays are a separate table. Their beds are marked `TEMPORARY`. Monthly vacant-bed counts can ignore those beds; vacant beds (P+T) count them as taken.
- Not full Spring Security — simple fixed login for family admins.
- Free Render Postgres expires about 30 days after creation. See the frontend `DEPLOY.md`.
