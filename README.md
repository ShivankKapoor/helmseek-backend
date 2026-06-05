# HelmSeek Backend

REST API for [HelmSeek](https://helmseek.com) — a personalized browser homepage. Handles authentication, session management, and user configuration storage.

## Stack

- **Kotlin 2.3** + **Spring Boot 4.0**
- **Java 25** with virtual threads (`spring.threads.virtual.enabled=true`)
- **PostgreSQL** — schema in `schema.sql`
- **Argon2** password hashing (BouncyCastle)
- **Bucket4j** per-IP rate limiting with Guava cache
- **HttpOnly session cookies** — frontend never touches the token

## Requirements

- Java 25
- PostgreSQL (run `schema.sql` against your database before first boot)
- A `.env` file in the project root (see below)

## Environment Variables

Create a `.env` file in the project root:

```env
DB_URL=jdbc:postgresql://<host>:<port>/<dbname>
DB_USERNAME=<user>
DB_PASSWORD=<password>

PORT=7666
ALLOWED_ORIGIN=https://yourdomain.com

RATE_LIMIT_PER_MINUTE=30
RATE_LIMIT_BURST=10
RATE_LIMIT_AUTH_PER_MINUTE=3

PROD=true
```

`DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are required — the app will fail to start without them.

## Running Locally

```bash
./gradlew bootRun
```

Requires PostgreSQL running and a `.env` file present.

## Running in Production (Podman)

```bash
./run.sh    # build image, start container, begin log streaming
./stop.sh   # stop container and remove image
```

The container runs on port **7666**. The `.env` file is bind-mounted read-only at runtime — it is never baked into the image.

Logs are written to `logs/<timestamp>_CST.log` on the host. A new file is created on each container start.

## API

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/health` | None | Health check (includes DB status) |
| POST | `/auth/register` | None | Create account |
| POST | `/auth/login` | None | Authenticate, set session cookie |
| POST | `/auth/logout` | Cookie | Invalidate session, clear cookie |
| GET | `/api/config` | Cookie | Get user configuration |
| POST | `/api/config` | Cookie | Update user configuration |

## Tests

```bash
./gradlew test
```

Covers: `AuthService`, `IpService`, `HealthService`, `SessionCleanupJob`, `RateLimitFilter`.

## Security Notes

- Session cookie is `HttpOnly`, `Secure`, `SameSite=Strict` with 30-day expiry
- Login response time is constant whether or not the username exists (dummy Argon2 hash on miss)
- Quick link URLs are validated server-side to block non-HTTP(S) schemes
- CORS is locked to `ALLOWED_ORIGIN` — never wildcard
- Rate limiting: 30 req/min globally, 3 req/min on `POST /auth/login`, per IP
