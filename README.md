# HelmSeek Backend

REST API for [HelmSeek](https://helmseek.com) — a personalized browser homepage. Stores user
configuration and acts as the BFF for the frontend.

Authentication is **not** handled here. It is delegated to
[aldrop](https://github.com/ShivankKapoor/aldrop), a standalone auth service that owns
credentials, password hashing and session tokens. This backend holds the `helmseek_session`
cookie and exchanges it with aldrop on each request; the aldrop API key never reaches the
browser. Users are created in aldrop, and a matching helmseek row is provisioned automatically
on their first login.

## What it stores

A HelmSeek homepage is a small set of widgets, and this API is the source of truth for how
each user has configured theirs. One `users` row holds the whole configuration:

- **Theme** — light/dark and an accent colour pair
- **Hero widget** — a clock, a greeting, both or neither; 12h/24h, optional seconds, greeting name
- **Weather widget** — enabled flag, ZIP, screen corner, and the resolved city/lat/lng
- **Quick links** — a user-defined list of labelled links, stored as JSON
- **Quote of the day** — whether the quote is shown, and whether this user has hidden today's
- **Font** — the typeface the page renders in

Two tables record history rather than configuration:

- **`weather_history`** — every weather reading pushed by a client, kept for later analysis and
  deduplicated in-process so repeat pushes for the same user do not pile up
- **`interaction_log`** — auth events with the client IP, enriched with city/country from
  Meridian when configured. User ids are nulled rather than deleted if a user goes away.

### Weather, and why the frontend pushes it

The browser fetches weather from a free, rate-limited API and pushes the result here via
`POST /user/weather`. The backend caches it on the user's row, so page loads and other devices
read the stored copy instead of each one spending quota against that API.

### Quotes

Quotes come from an external quote service (`QUOTE_SERVICE_URL`). A user can hide the current
one, and `QuoteHideResetJob` unhides every user's quote nightly at 00:30 America/Chicago, so
hiding lasts for the day rather than forever. If the quote service is unset or unreachable, the
API returns a placeholder rather than failing the request.

## External services

| Service | Variable | Required | Purpose |
|---|---|---|---|
| [aldrop](https://github.com/ShivankKapoor/aldrop) | `ALDROP_URL`, `ALDROP_API_KEY` | Yes | Authentication — credentials, hashing, sessions |
| Meridian | `MERIDIAN_URL` | No | Resolves a client IP to city/country for the interaction log |
| Quote service | `QUOTE_SERVICE_URL` | No | Supplies the quote of the day |

Only aldrop is required. The other two degrade to `UNKNOWN` values when unset or unreachable,
so the API keeps working without them.

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
| GET | `/` | None | Plain welcome page |
| GET | `/health` | None | Health check (includes DB status) |
| GET | `/monitor` | None | Uptime, thread count and status |
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
