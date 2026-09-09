# HelmSeek Backend

REST API for [HelmSeek](https://helmseek.com) — a personalized browser homepage. Stores user
configuration and acts as the BFF for the frontend.

Authentication is **not** handled here. It is delegated to
[aldrop](https://github.com/ShivankKapoor/aldrop), a standalone auth service that owns
credentials, password hashing and session tokens. This backend holds the `helmseek_session`
cookie and exchanges it with aldrop on each request; the aldrop API key never reaches the
browser. Users are created in aldrop, and a matching helmseek row is provisioned automatically
on their first login.

## Stack

- **Kotlin 2.3** + **Spring Boot 4.0**
- **Java 25** with virtual threads (`spring.threads.virtual.enabled=true`)
- **PostgreSQL** — schema in `schema.sql`
- **[aldrop](https://github.com/ShivankKapoor/aldrop)** for authentication (see above)
- **Bucket4j** per-IP rate limiting, with **Caffeine** caches
- **HttpOnly session cookies** — frontend never touches the token

## Requirements

- Java 25
- PostgreSQL (run `schema.sql` against your database before first boot)
- A reachable [aldrop](https://github.com/ShivankKapoor/aldrop) instance, and a platform
  registered on it whose API key you hold
- A `.env` file in the project root (see below)

## Environment Variables

Create a `.env` file in the project root:

```env
DB_URL=jdbc:postgresql://<host>:<port>/<dbname>
DB_USERNAME=<user>
DB_PASSWORD=<password>

PORT=7666
ALLOWED_ORIGIN=https://yourdomain.com

RATE_LIMIT_PER_MINUTE=100
RATE_LIMIT_BURST=10
RATE_LIMIT_AUTH_PER_MINUTE=2

PROD=true

ALDROP_URL=http://<host>:4000
ALDROP_API_KEY=<platform api key>

MERIDIAN_URL=
QUOTE_SERVICE_URL=
```

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ALDROP_URL` and `ALDROP_API_KEY` are required — the app
will fail to start without them. See `.env.example` for the full list.

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
| POST | `/auth/login` | None | Authenticate via aldrop, set session cookie |
| POST | `/auth/logout` | Cookie | Revoke the aldrop session, clear cookie |
| GET | `/user/config` | Cookie | Get user configuration |
| POST | `/user/config` | Cookie | Update user configuration |
| POST | `/user/weather` | Cookie | Push cached weather data |
| GET | `/quote` | Cookie | Get the current quote |
| POST | `/quote/hideQuote` | Cookie | Hide the quote |
| POST | `/quote/unhideQuote` | Cookie | Unhide the quote |

There is no registration endpoint — accounts are created in aldrop.

## Tests

```bash
./gradlew test
```

Covers the controllers, `AuthService`, `UserService`, `QuoteService`, `IpService`,
`HealthService`, `InteractionService`, `WeatherHistoryService` and `RateLimitFilter`.
aldrop is stubbed with `MockRestServiceServer`, so the tests need no running auth service.

## Security Notes

- Session cookie is `HttpOnly`, `Secure`, `SameSite=Strict` with 30-day expiry
- No credentials are stored here — aldrop owns password hashes and session rows
- The aldrop API key is server-side only and is never exposed to the browser
- Session tokens are cached in-process for 45s, so a session revoked elsewhere can remain
  usable for up to that long; logouts through this backend take effect immediately
- Quick link URLs are validated server-side to block non-HTTP(S) schemes
- CORS is locked to `ALLOWED_ORIGIN` — never wildcard
- Per-IP rate limiting here complements aldrop's own per-username login limit
