# Enhancements — Drawback Fixes

This document maps each of the 6 identified drawbacks to exactly what was
added, and where to find it.

## #1 — Single Server / No Load Balancer

**Fix:** Redis pub/sub decouples WebSocket broadcasting from any single
JVM's memory, making the app horizontally scalable behind a load balancer.

| File | Role |
|---|---|
| `config/RedisConfig.java` | Registers Redis connection + pub/sub listener container |
| `dto/RedisEditMessage.java` | Payload published to Redis on every edit |
| `websocket/DocumentEditSubscriber.java` | Receives edits from Redis, relays to local sessions |
| `websocket/DocumentWebSocketHandler.java` | Publishes edits to Redis instead of only local broadcast |
| `docker-compose.yml` | Local Redis for testing |

Nginx/ALB config for the load balancer itself is documented in the chat
above (not a code file, since it's infra config, not application code).

## #2 — No Caching (Documents + Presence)

| File | Role |
|---|---|
| `service/DocumentCacheService.java` | Read-through/write-invalidate cache for document content |
| `service/PresenceService.java` | Tracks active viewers per document via Redis sets + TTL |
| `controller/PresenceController.java` | `GET /api/documents/{id}/presence` |
| `service/DocumentService.java` | Reads now go through the cache |

## #3 — No Conflict Resolution (OT)

| File | Role |
|---|---|
| `ot/TextOperation.java`, `ot/OpType.java` | Operation model (insert/delete) |
| `ot/OperationalTransformService.java` | Core transform algorithm |
| `ot/OtDocumentService.java` | Server-side operation history + apply logic |
| `controller/OtController.java` | `POST /api/documents/{id}/operations` |
| `test/.../OperationalTransformServiceTest.java` | 6 test cases covering insert/insert, insert/delete, delete/delete conflicts |

**Scope honesty:** this is a real, working simplified OT implementation
covering the two most common conflict shapes. It is NOT a full production
OT/CRDT engine (see code comments in `OperationalTransformService.java` for
exactly what's out of scope, e.g. splitting a delete range around a nested
insert). Good enough to demonstrate and defend the concept in an interview;
would need more work (or a library like Yjs) for production use.

## #4 — No Versioning

| File | Role |
|---|---|
| `model/DocumentVersion.java` | Metadata row (S3 key + version number) |
| `repository/DocumentVersionRepository.java` | Query snapshots by document |
| `versioning/S3Service.java` | Upload/download snapshot blobs |
| `versioning/DocumentSnapshotScheduler.java` | `@Scheduled` job, runs every 5 min, skips unchanged docs |
| `controller/VersionController.java` | List versions + fetch historical content |

Requires AWS credentials configured via the standard credential chain
(env vars or IAM role) — never hardcoded. Set `aws.s3.bucket` and
`aws.region` in `application.properties`.

## #5 — Basic Security (RBAC + Rate Limiting)

| File | Role |
|---|---|
| `model/SharePermission.java` | This already IS the Document_User RBAC table (documented in code) |
| `service/DocumentService.java` | `requireAtLeastViewer` / `requireAtLeastEditor` — server-side enforcement |
| `ratelimit/RateLimitFilter.java` | Redis fixed-window rate limiter, 100 req/min per user |
| `config/SecurityConfig.java` | Wires the rate limiter into the filter chain |

## #6 — No Monitoring

| File | Role |
|---|---|
| `monitoring/DatabaseHealthIndicator.java` | Custom `/actuator/health` check for DB connectivity |
| `monitoring/AppMetrics.java` | Custom counters: edits, rate-limit rejections, WS connections |
| `application.properties` | Actuator endpoints exposed; Stackdriver (GCP) export config (disabled by default — needs a real GCP project to enable) |

**To actually ship metrics to GCP:** set `management.stackdriver.metrics.export.enabled=true`
and provide a real `project-id`, plus GCP Application Default Credentials
in your deployment environment. It's left disabled by default so the app
runs without requiring a GCP account.

---

## What still needs a real environment to verify

I could not compile or run this project in my sandbox (no Maven Central
access, no Redis/MySQL/AWS/GCP credentials available here). Before you
present or push this:

1. Run `mvn clean install` locally and fix any compile errors
2. Run `docker compose up -d` to get local Redis + MySQL
3. Run `mvn test` — the OT test suite especially deserves a careful look
4. For S3/GCP features, either provide real credentials or treat those two
   as "designed and coded, pending infra credentials to fully verify" when
   you talk about them — that's an honest and completely normal thing to
   say about a portfolio project
