# PasteShare

PasteShare is a small text-sharing application. It saves plain text in
PostgreSQL and gives each paste an unlisted, unpredictable link. A paste can
optionally expire at a specified time or after a maximum number of successful
views.

> This project guide lives at `docs/pasteshare/README.md` to avoid restoring the
> pre-existing deleted HOTEL `README.md` in the repository root.

## Features

- Create a paste with required non-blank text.
- Optionally set a future expiration time and/or positive maximum view count.
- Share a URL containing a 128-bit random, URL-safe ID.
- Retrieve submitted content as plain text; content is not interpreted as HTML.
- Enforce expiration and view limits in the backend.
- Serialize concurrent reads of one paste with a PostgreSQL row lock. The
  final permitted read succeeds; later reads receive `410 Gone`.
- No accounts, editing, deletion, search, analytics, or Markdown rendering.

## Project structure

```text
backend/
  Dockerfile
  pom.xml
  src/main/java/com/pasteshare/
    config/       # Application properties, clock, and CORS
    controller/   # REST endpoints
    dto/          # API request and response types
    entity/       # JPA entity
    exception/    # Domain exceptions and consistent API errors
    repository/   # Spring Data JPA and row-lock query
    service/      # Business rules and ID generation
  src/main/resources/
    application.properties
    db/migration/ # Flyway migrations
  src/test/java/  # Unit and PostgreSQL/Testcontainers tests
frontend/
  src/
    api/          # Fetch API integration
    pages/        # Create, success, and view routes
    test/         # Shared Vitest setup
  package.json
  vercel.json
render.yaml
.env.example
```

## Architecture

The React SPA talks to the Spring Boot REST API. Controllers validate requests
and translate service results into HTTP responses. Services implement creation,
expiration, and view-count rules. Spring Data JPA persists the `Paste` entity.
Flyway creates the PostgreSQL schema; Hibernate validates the schema at startup
but does not create or alter it.

Paste IDs are generated from 16 cryptographically random bytes and encoded as
unpadded URL-safe Base64. PostgreSQL's primary key enforces uniqueness. Inserts
use `ON CONFLICT (id) DO NOTHING`, so an ID collision is detected and retried
without converting unrelated database constraint errors into retries.

For retrieval, the service opens a transaction and selects the paste with
`PESSIMISTIC_WRITE`. It checks expiration and the view cap, increments the
count, then returns. This makes the view limit atomic across concurrent
requests for the same paste. Retrieval responses use `Cache-Control: no-store`.

The browser displays content in a `<pre>` element as text. It never uses
`dangerouslySetInnerHTML` or Markdown processing.

The backend exposes Spring Boot Actuator health only. Render checks
`/actuator/health/readiness`; health details are hidden from unauthenticated
responses.

## Database schema

Flyway migration `V1__create_pastes.sql` creates:

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `varchar(22)` | Primary key; fixed-length check |
| `content` | `text` | Not null; non-empty check |
| `created_at` | `timestamptz` | Not null |
| `expires_at` | `timestamptz` | Nullable |
| `max_views` | `integer` | Nullable; positive when set |
| `view_count` | `bigint` | Not null; defaults to zero; non-negative and no greater than `max_views` |

Expiration is enforced when reading. Expired records are not automatically
purged; database retention/cleanup is not part of this application.

## Requirements

- Java 21
- Maven 3.9+
- Node.js 20+ and npm
- PostgreSQL 14+ for local execution
- Docker for Testcontainers integration tests

## Environment variables

Copy `.env.example` to a local environment file for reference. Spring Boot and
Vite do not load a root `.env` file automatically; export the variables in the
shell or configure them through your IDE/deployment platform.

| Variable | Required | Default / purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | Yes for backend | JDBC URL starting with `jdbc:postgresql://`; Neon URLs should include `sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Yes for backend | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Yes for backend | Database password; do not commit it |
| `PORT` | No | Render-provided HTTP port; defaults to `8080` |
| `PASTESHARE_PUBLIC_BASE_URL` | Yes | Public frontend origin used to construct share URLs |
| `PASTESHARE_ALLOWED_ORIGIN` | Yes | Exact browser origin allowed by API CORS |
| `PASTESHARE_MAX_CONTENT_LENGTH` | No | Maximum submitted Java string length; defaults to `100000` |
| `PASTESHARE_ID_GENERATION_ATTEMPTS` | No | Maximum collision retries; defaults to `5` |
| `VITE_API_BASE_URL` | Yes for builds | Backend origin, e.g. `https://pasteshare-api.onrender.com`; production builds fail if missing |
| `VITE_MAX_CONTENT_LENGTH` | No | Frontend editor limit; defaults to `100000` and should match the backend limit |

The frontend API base URL is embedded at build time. Set it in Vercel before
building. The API CORS origin and public share URL must use the exact deployed
frontend origin (scheme and hostname, without a trailing path).

## Run locally

Create a PostgreSQL database and user, then set the backend variables in
PowerShell:

```powershell
createdb pasteshare
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/pasteshare"
$env:SPRING_DATASOURCE_USERNAME = "your_local_database_user"
$env:SPRING_DATASOURCE_PASSWORD = "your_local_database_password"
$env:PASTESHARE_PUBLIC_BASE_URL = "http://localhost:5173"
$env:PASTESHARE_ALLOWED_ORIGIN = "http://localhost:5173"
Set-Location backend
mvn spring-boot:run
```

Flyway applies migrations when the backend starts. In another terminal:

```powershell
Set-Location frontend
npm ci
$env:VITE_API_BASE_URL = ""
npm run dev
```

Open the Vite URL printed in the terminal (normally `http://localhost:5173`).
The Vite development server proxies `/api` requests to `http://localhost:8080`.
Production builds require `VITE_API_BASE_URL`; set it to the deployed backend
origin before running `npm run build`.

## REST API

Base path: `/api/v1`

### Create a paste

`POST /api/v1/pastes`

Request:

```json
{
  "content": "Hello World",
  "expiresAt": "2026-11-11T10:00:00Z",
  "maxViews": 5
}
```

`expiresAt` and `maxViews` are optional and independent. `expiresAt` must be a
future ISO-8601 instant; `maxViews` must be a positive integer. Content must be
non-blank and within the configured maximum.

Success: `201 Created` with a relative `Location: /api/v1/pastes/{id}` header.

```json
{
  "id": "0123456789abcdefghijkL",
  "shareUrl": "https://example.com/p/0123456789abcdefghijkL",
  "createdAt": "2026-11-10T10:00:00Z",
  "expiresAt": "2026-11-11T10:00:00Z",
  "maxViews": 5
}
```

The share URL's origin comes from `PASTESHARE_PUBLIC_BASE_URL`; request host
headers are not used to construct it.

### Retrieve a paste

`GET /api/v1/pastes/{id}`

Success: `200 OK`, with `Cache-Control: no-store`.

```json
{
  "id": "0123456789abcdefghijkL",
  "content": "Hello World",
  "createdAt": "2026-11-10T10:00:00Z",
  "expiresAt": "2026-11-11T10:00:00Z",
  "viewsRemaining": 4
}
```

`viewsRemaining` is `null` when the paste has no view limit. The current
successful request is counted; the last allowed request returns zero remaining.

### Errors

Validation errors return `400`; missing IDs return `404`; pastes whose time or
view limit is exhausted return `410`. Unexpected errors return `500` and are
logged server-side.

```json
{
  "timestamp": "2026-11-10T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "content: Content must not be blank.",
  "path": "/api/v1/pastes"
}
```

## Tests

Frontend:

```powershell
Set-Location frontend
npm ci
npm test
$env:VITE_API_BASE_URL = "https://your-pasteshare-api.onrender.com"
npm run build
```

Backend unit tests:

```powershell
Set-Location backend
mvn test
```

The Testcontainers integration tests start PostgreSQL 16 in Docker. Ensure
Docker is running before invoking Maven. They cover the REST contract and
concurrent view-limit enforcement, including the persisted count.

## Deployment

### Neon PostgreSQL

1. Create a Neon project/database and a database role.
2. Use a Neon PostgreSQL JDBC URL with TLS, for example
   `jdbc:postgresql://<neon-host>/<database>?sslmode=require`. Use Neon’s pooled
   hostname if desired.
3. Keep the URL, username, and password in Render environment variables; never
   put credentials in the repository.
4. Flyway applies schema changes at application startup. `ddl-auto=validate`
   makes the application fail fast if the migrated schema and JPA mapping do
   not agree.

### Render backend

1. Create a Render Web Service from this repository using the included
   `render.yaml` Blueprint or configure a Docker Web Service with
   `backend/Dockerfile` and `backend` as its Docker context.
2. Add `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
   `SPRING_DATASOURCE_PASSWORD` using the Neon connection details.
3. Set `PASTESHARE_PUBLIC_BASE_URL` to the Vercel production origin and
   `PASTESHARE_ALLOWED_ORIGIN` to the same exact origin.
4. Render provides `PORT`; the application listens on it.
5. Deploy and inspect startup logs to confirm Flyway migration and application
   startup succeed.

### Vercel frontend

1. Import the repository in Vercel and set the project Root Directory to
   `frontend`.
2. Use `npm run build` and `dist` as the output directory (also configured in
   `frontend/vercel.json`).
3. Set `VITE_API_BASE_URL` to the Render API origin, without a trailing slash,
   and `VITE_MAX_CONTENT_LENGTH` to the backend's configured content limit.
4. Deploy/redeploy after setting environment variables. The Vercel rewrite
   serves the React SPA for nested routes such as `/p/{id}`.
5. Confirm the Vercel origin matches both backend origin settings exactly.

## Operational notes

- No authentication is implemented; links are unlisted, not access-controlled.
- Configure HTTPS at the hosting providers and do not send sensitive content.
- There is no automatic expiration cleanup job.
- Set content limits consistently on frontend and backend.
- Browser clipboard access requires a secure context in production.
- Backend compilation and Testcontainers execution require Java 21, Maven, and
  Docker in the validation environment.
