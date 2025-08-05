# Video Metadata Service (Reactive Spring Boot + GraphQL)

A production‑ready, **reactive** backend for ingesting, storing and analyzing video metadata across platforms (YouTube, Vimeo). It exposes a **GraphQL** API, uses **JWT** auth with **RS256 key rotation** and a **JWKS** endpoint, persists via **PostgreSQL (R2DBC)**, caches in **Redis**, and applies **Resilience4j** for retries, rate limiting, circuit‑breaking, and bulkheads. Includes Flyway DB migrations, Docker Compose, and a build‑time GraphQL schema printer.

---

## Table of Contents

* [Features](#features)
* [Tech Stack](#tech-stack)
* [Architecture](#architecture)
* [Project Structure](#project-structure)
* [Getting Started](#getting-started)

  * [Prerequisites](#prerequisites)
  * [Environment](#environment)
  * [Run with Docker Compose](#run-with-docker-compose)
  * [Run the App](#run-the-app)
  * [Generate GraphQL Schema (build‑time)](#generate-graphql-schema-build-time)
* [Database & Migrations](#database--migrations)
* [Security](#security)

  * [JWT & JWKS](#jwt--jwks)
  * [Authorization & Roles](#authorization--roles)
* [Caching](#caching)
* [Resilience](#resilience)
* [GraphQL API](#graphql-api)

  * [Custom Annotation Model](#custom-annotation-model)
  * [Scalars](#scalars)
  * [Operations Reference](#operations-reference)
  * [Postman Examples](#postman-examples)
* [Frontend Integration](#frontend-integration)

  * [GraphQL Codegen](#graphql-codegen)
  * [Example Frontend Documents](#example-frontend-documents)
  * [Token Refresh](#token-refresh)
* [Configuration Reference](#configuration-reference)
* [Troubleshooting](#troubleshooting)
* [CI / CD](#ci--cd)
* [License](#license)

---

## Features

* **Reactive** end‑to‑end (Spring WebFlux, R2DBC, Reactor).
* **GraphQL** endpoint (`POST /api/graphql`) with code‑first schema generation.
* **JWT** authentication & **role‑based access control**; **RS256** with **key rotation**; legacy HS256 supported during migration.
* **JWKS** publishing at `/.well-known/jwks.json`.
* **Redis** reactive cache for hot user data (with JavaTime serialization configured) & token/session helpers.
* **Resilience4j**: retry, rate limit, circuit breaker, **bulkhead** on provider calls.
* **YouTube & Vimeo** metadata adapters; async, resilient importing; duplicate protection.
* **Flyway** migrations (global sequence, tables, indices, default admin user).
* **Docker Compose** for PostgreSQL & Redis.
* **Schema printer** task to generate `schema.graphqls` at build time.

---

## Tech Stack

* **Java 21**, **Spring Boot 3.5.x**
* **WebFlux**, **Spring Security**, **Spring GraphQL 1.4.x**
* **R2DBC** (PostgreSQL) + **Flyway** (JDBC) for migrations
* **Redis (reactive)** via Lettuce
* **Resilience4j 2.3.x**
* **GraphQL Java 24.x** + Extended Scalars

---

## Architecture

* **CQRS‑style** separation by feature: command flows in services that write data (import, user CRUD), and query services for paged/filtered reads.
* **Adapters** per provider (YouTube/Vimeo) encapsulate HTTP specifics, rate limits, fallbacks.
* **Security**: Reactive resource server validates JWT (RS256/HS256) and enforces roles via annotations and method security.
* **Schema**: built from annotated operation classes at runtime; also printed at build time.

---

## Project Structure

```
src
├─ main
│  ├─ java/com/github/dimitryivaniuta/videometadata
│  │  ├─ VideoMetadataServiceApplication.java
│  │  ├─ config/
│  │  │  ├─ SecurityConfig.java
│  │  │  ├─ RedisConfig.java
│  │  │  ├─ DataLoaderConfig.java
│  │  │  ├─ VideoProvidersProperties.java
│  │  │  ├─ SecurityJwtProperties.java
│  │  │  ├─ JwkKeyManager.java
│  │  │  ├─ CompositeJwtDecoder.java
│  │  │  └─ AuthenticationLoggingWebFilter.java
│  │  ├─ graphql/
│  │  │  ├─ annotations/ (@GraphQLApplication, @GraphQLField, @GraphQLMutation, @GraphQLArgument, @GraphQLIgnore)
│  │  │  ├─ schema/ (AnnotationSchemaFactory, SchemaExporter, SecurityChecks, RequiresRole)
│  │  │  └─ operations/ (AuthOperations, UsersOperations, QueryOperations, ImportOperations)
│  │  ├─ model/ (User, UserRole, Role, UserStatus, Video, VideoProvider, VideoCategory)
│  │  ├─ repository/ (UserRepository, UserRoleRepository, VideoRepository)
│  │  ├─ service/
│  │  │  ├─ AuthService, JwtTokenProvider
│  │  │  ├─ UserService, UserServiceImpl, UserQueryService, UserQueryServiceImpl
│  │  │  ├─ VideoService, VideoServiceImpl, VideoQueryService, VideoQueryServiceImpl
│  │  │  ├─ UserCacheService
│  │  │  └─ videoprovider/
│  │  │     ├─ ExternalMetadataClient, ProviderAdapter
│  │  │     ├─ YoutubeAdapter, VimeoAdapter
│  │  ├─ util/ (DateTimeUtil …)
│  │  └─ web/dto/ (AuthRequest, TokenResponse, CachedUser, CreateUserInput, UpdateUserInput,
│  │             UserResponse, VideoResponse, imports/* ExternalYoutubeResponse, ExternalVimeoResponse, Metadata …)
│  └─ resources/
│     ├─ application.yml
│     ├─ video-providers.yml
│     └─ db/migration/V…__*.sql
└─ test/ …
```

---

## Getting Started

### Prerequisites

* JDK 21
* Docker (for Postgres & Redis)
* Postman / cURL (for testing)

### Environment

Create `.env` at project root:

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=video_db
DB_USER=postgres
DB_PASS=postgres

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DB=0

# JWT
JWT_ISSUER=video-metadata-app
JWT_AUDIENCE=video-metadata-clients
# Legacy HS256 (optional during migration) – base64 of a 32-byte secret
JWT_SECRET=Q0hBTkdFTUVfQkFTRTY0X1NFQ1JFVA==
# Access tokens TTL (seconds)
JWT_EXPIRATION_SECONDS=3600
# Refresh tokens TTL (seconds)
JWT_REFRESH_EXPIRATION_SECONDS=2592000

# Providers
YOUTUBE_API_KEY=your_youtube_api_key
VIMEO_ACCESS_TOKEN=your_vimeo_token
```

### Run with Docker Compose

A `docker-compose.yml` is included for Postgres & Redis:

```bash
docker compose up -d
```

It exposes:

* Postgres: `5432`
* Redis: `6379`

### Run the App

```bash
./gradlew bootRun
```

The server listens on `http://localhost:8080` with base path `/api`.

* GraphQL endpoint: `POST /api/graphql`
* JWKS endpoint: `GET /.well-known/jwks.json` (also `/api/.well-known/jwks.json`)
* OpenAPI/Swagger UI (if enabled): `/swagger-ui/index.html`

### Generate GraphQL Schema (build‑time)

The build registers a `printSchema` task that boots a light context (no DB/Redis/Security) and writes `build/generated/graphql/schema.graphqls`. It’s also embedded into the boot JAR under `BOOT-INF/classes`.

```bash
./gradlew printSchema
./gradlew bootJar
```

> If the printer fails because of missing beans, it runs under the `schema-print` profile and uses stub configuration to avoid wiring DB/Redis/Security.

---

## Database & Migrations

We use Flyway (JDBC) to create:

* Global `VM_UNIQUE_ID` sequence for BIGINT PKs
* `users`, `user_roles`, `videos` tables
* Constraints & indexes
* Default admin user: `username=admin`, `role=ADMIN`, password hash provided in migration (BCrypt)

On startup, Flyway applies migrations automatically using the **JDBC** datasource while the app uses **R2DBC** for runtime.

---

## Security

### JWT & JWKS

* Access tokens are **RS256** signed via a rotating RSA key managed by `JwkKeyManager`.
* Legacy **HS256** decoding is supported for a period (composite decoder) to allow migration.
* Published **JWKS** (public keys only) at `/.well-known/jwks.json`.
* Roles are emitted as a claim `roles: ["ADMIN","USER"]`, and the resource server maps them to `ROLE_*` authorities.

**Key Rotation**

* A new RSA key is generated on schedule (configurable). Old keys are retained for a retire period so existing tokens continue to validate.

### Authorization & Roles

* Method security with `@RequiresRole({"ADMIN"})` and annotations enforced in GraphQL invocation pipeline.
* `@PreAuthorize` may also be used on REST (if any). GraphQL operations are guarded in the schema factory.

---

## Caching

* Reactive Redis template configured with **Jackson `JavaTimeModule`** to serialize Java 8 time types.
* `UserCacheService` caches `CachedUser` by username with TTL matching access token expiry.

---

## Resilience

* Provider adapters (YouTube/Vimeo) use **Resilience4j** annotations: `@CircuitBreaker`, `@Retry`, `@RateLimiter`, `@Bulkhead`.
* Namespaces (e.g., `videoMeta-youtube`) are configurable in `application.yml`.

---

## GraphQL API

### Custom Annotation Model

Operations are implemented as Spring components annotated with:

* `@GraphQLApplication` – marks a bean class to scan for operations
* `@GraphQLField("name")` – query operation
* `@GraphQLMutation("name")` – mutation
* `@GraphQLArgument("name")` – argument binding
* `@RequiresRole({"…"})` – role guard for this field

The schema is produced at runtime by `AnnotationSchemaFactory`, which reflects on these annotations and builds a GraphQL Java schema. Extended scalars are registered once to avoid duplicate type names.

### Scalars

* `String`, `Int`, `Float`, `Boolean`, `ID` (built‑ins)
* Extended: `Long`, `BigDecimal`, `BigInteger`, `DateTime` (maps `OffsetDateTime`/`Instant`)

### Operations Reference

**Auth**

* `login(username: String!, password: String!): TokenResponse!`
* `signUp(input: SignUpInput!): UserResponse!`

**Users** (ADMIN)

* `user(id: ID!): UserResponse`
* `connectionUsers(page, pageSize, search, sortBy, sortDesc): UserConnection!`
* `connectionUsersCount(search): Long!`
* `createUser(input: CreateUserInput!): UserResponse!`
* `updateUser(input: UpdateUserInput!): UserResponse!`
* `deleteUser(id: ID!): Boolean!`

**Videos**

* `videos(page, pageSize, provider, sortBy, sortDesc): VideoConnection!`
* `videosCount(provider): Long!`

**Import**

* `importVideo(provider: VideoProvider!, externalVideoId: String!): VideoResponse!`
* `importVideosByPublisher(provider: VideoProvider!, publisherHandleOrId: String!, max: Int): [VideoResponse!]!`

### Postman Examples

**Login**

```json
{
  "query": "mutation Login($u:String!,$p:String!){ login(username:$u,password:$p){ token expiresAt } }",
  "variables": { "u": "admin", "p": "adminpass" }
}
```

**Get Users (ADMIN)**

```json
{
  "query": "query ConnectionUsers($page:Int,$pageSize:Int,$search:String,$sortBy:UserSort,$sortDesc:Boolean){ connectionUsers(page:$page,pageSize:$pageSize,search:$search,sortBy:$sortBy,sortDesc:$sortDesc){ items { id username email status roles createdAt updatedAt lastLoginAt } page pageSize total } }",
  "variables": { "page": 1, "pageSize": 20, "search": "", "sortBy": "USERNAME", "sortDesc": false }
}
```

**Import a video**

```json
{
  "query": "mutation ImportVideo($provider:VideoProvider!,$id:String!){ importVideo(provider:$provider, externalVideoId:$id){ id title source durationMs description externalVideoId uploadDate createdUserId videoCategory videoProvider } }",
  "variables": { "provider": "YOUTUBE", "id": "RD_Ojv9Zu1IZY" }
}
```

**Import by publisher**

```json
{
  "query": "mutation ImportByPublisher($provider:VideoProvider!,$publisher:String!,$max:Int){ importVideosByPublisher(provider:$provider,publisherHandleOrId:$publisher,max:$max){ id title source externalVideoId uploadDate } }",
  "variables": { "provider": "YOUTUBE", "publisher": "@lostmusicco", "max": 50 }
}
```

> Send to `POST http://localhost:8080/api/graphql` with header `Authorization: Bearer <JWT>` (except `login` & `signUp`).

---

## Frontend Integration

### GraphQL Codegen

`codegen.yml` (example):

```yaml
schema: "http://localhost:8080/api/graphql"
documents:
  - "src/graphql/**/*.graphql"
generates:
  src/graphql/generated/graphql.tsx:
    plugins:
      - typescript
      - typescript-operations
      - typescript-react-apollo
    config:
      withHooks: true
```

Run: `pnpm run codegen` (or `npm/yarn`).

### Example Frontend Documents

`src/graphql/auth/Login.graphql`

```graphql
mutation Login($username: String!, $password: String!) {
  login(username: $username, password: $password) {
    token
    expiresAt
  }
}
```

`src/graphql/users/ConnectionUsers.graphql`

```graphql
query ConnectionUsers($page:Int,$pageSize:Int,$search:String,$sortBy:UserSort,$sortDesc:Boolean){
  connectionUsers(page:$page,pageSize:$pageSize,search:$search,sortBy:$sortBy,sortDesc:$sortDesc){
    items { id username email status roles createdAt updatedAt lastLoginAt }
    page
    pageSize
    total
  }
}
```

`src/graphql/users/ConnectionUsersCount.graphql`

```graphql
query ConnectionUsersCount($search:String){
  connectionUsersCount(search:$search)
}
```

`src/graphql/videos/Videos.graphql`

```graphql
query Videos($page:Int,$pageSize:Int,$provider:String,$sortBy:VideoSort,$sortDesc:Boolean){
  videos(page:$page,pageSize:$pageSize,provider:$provider,sortBy:$sortBy,sortDesc:$sortDesc){
    items { id title source durationMs description externalVideoId uploadDate createdUserId videoCategory videoProvider }
    page
    pageSize
    total
  }
}
```

### Token Refresh

* The backend provides `/api/auth/refresh` (if enabled) to exchange a refresh token (HttpOnly cookie) for a new access token.
* Frontend strategy: on **401** from GraphQL, call refresh, update access token, and **retry** the failed operation.

---

## Configuration Reference

Key `application.yml` highlights (see full file for all):

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASS}
  datasource:              # for Flyway
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASS}
  flyway:
    enabled: true
    locations: classpath:db/migration

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      database: ${REDIS_DB}
  cache:
    type: redis

security:
  oauth2:
    resourceserver:
      jwt:
        # decoder config handled by CompositeJwtDecoder / JWKS
  jwt:
    issuer: ${JWT_ISSUER}
    audience: ${JWT_AUDIENCE}
    secret: ${JWT_SECRET}
    expiration-seconds: ${JWT_EXPIRATION_SECONDS}
    refresh-expiration-seconds: ${JWT_REFRESH_EXPIRATION_SECONDS}

video:
  default-page-size: 20
  max-page-size: 100
```

---

## Troubleshooting

**JwtEncodingException: Failed to select a JWK signing key**

* Ensure `JwkKeyManager` is wired and `JwtEncoder` uses its `ImmutableJWKSet`.
* Verify `/.well-known/jwks.json` is reachable (for RS256 decode) and not blocked by security.

**JWKS 404**

* Check the security config permits GET `/.well-known/jwks.json` (and `/api/.well-known/jwks.json` if behind base path).

**Redis: Java 8 date/time not supported**

* Confirm `RedisConfig` registers `JavaTimeModule` and disables `WRITE_DATES_AS_TIMESTAMPS`.

**R2DBC URL missing**

* Set `spring.r2dbc.url` properly (R2DBC, not JDBC). Keep JDBC datasource for Flyway.

**Cache manager missing (REDIS)**

* Ensure `spring.cache.type=redis` and a `ReactiveRedisTemplate` bean exists.

**MappingException for COUNT**

* For custom count queries, alias to `cnt` and map to `Mono<Long>` with `@Query("SELECT COUNT(*) AS cnt …")`.

**Schema printer errors about missing beans**

* Run `printSchema` task; it boots a minimal context under `schema-print` profile with stubs and excludes DB/Redis/Security auto‑configs.

---

## CI / CD

A minimal CI stub is provided (GitHub Actions) to:

* Build (Gradle)
* Run `printSchema`
* Package `bootJar`
* (Extend to run tests, lint, image build & push)

---

## License

This project is licensed under the [MIT License](LICENSE).

---

## Contact

**Dimitry Ivaniuta** — [dzmitry.ivaniuta.services@gmail.com](mailto:dzmitry.ivaniuta.services@gmail.com) — [GitHub](https://github.com/DimitryIvaniuta)
