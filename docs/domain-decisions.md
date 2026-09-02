# Connecta — domain decisions (User / Auth / Social / Posts / Notifications / Messages)

Locked for implementation. Private master plan PDF may lag; this file is the repo source of truth for these rules.

## User model

| Field | Required | Notes |
|-------|----------|--------|
| id | yes | UUID |
| username | yes | unique |
| email | yes | unique |
| passwordHash | yes | BCrypt only |
| displayName | yes | |
| bio | no | max 100 |
| profilePictureUrl | no | set only via upload; URL is Azure Blob or local `/media` |
| dateOfBirth | yes | min age 15 |
| location | no | free text, e.g. "Novi Sad" |
| gender | no | MALE, FEMALE, OTHER |
| isPrivate | yes | default false |
| role | yes | USER or ADMIN; never from client register |
| isActive | yes | soft deactivate; do not hard-delete users |
| isBanned | yes | admin ban; separate from isActive |
| createdAt / updatedAt / updatedBy | yes | AuditEntity |

Not in MVP: websiteUrl, phone, cover, emailVerified, lastLoginAt, locale, timezone.

## Role

- Register always assigns `USER`.
- `ADMIN` only via server-side seed / manual DB update.
- JWT claim `role`; Gateway forwards `User-Role`.
- Admin API and `/admin` UI require `ADMIN`.

## Admin (MVP)

- FE route: `/admin` (ADMIN only; navbar link is hidden for `USER`).
- Backend (ADMIN role required):
  - `GET /api/admin/users`
  - `GET /api/admin/users/{userId}`
  - `PATCH /api/admin/users/{userId}/ban`
  - `PATCH /api/admin/users/{userId}/unban`
  - `PATCH /api/admin/users/{userId}/deactivate` (`isActive=false`)
  - `PATCH /api/admin/users/{userId}/restore` (`isActive=true`)
- Ban sets `isBanned=true` (profile retained). Unban clears it.
- Admins cannot ban/deactivate themselves via admin API.
- Seed admin (Flyway V2): username `admin`, password `Admin123!` — change outside local use.
- Login rejects banned and inactive users.

## Profile picture

- Client uploads multipart file (`multipart/form-data`); no manual URL entry.
- Storage: see **Image storage** below. User Service uses `ProfilePictureStorage` (Azure Blob container `avatars`, or local `CONNECTA_STORAGE_DIR/profile-pictures/` at `/media/profile-pictures/**`).
- Register (`POST /api/auth/register`): optional part `profilePicture` + JSON part `data`.
- Login: `POST /api/auth/login`.
- Missing picture → FE placeholder / initials.

## Post image

- Client uploads multipart file on create (`data` JSON `{ "content" }` + optional `image`); no manual URL entry.
- Storage: see **Image storage** below. Post Service uses `PostImageStorage` (Azure Blob container `posts`, or local `CONNECTA_STORAGE_DIR/posts/` at `/media/posts/**`).
- Gateway routes `/media/posts/**` to Post Service **before** `/media/**` to User Service (local files only; Blob URLs are absolute HTTPS).
- One optional image per post; JPEG / PNG / WebP; max 5MB.
- Missing image → `imageUrl` is null. In Swagger, do not send an empty file part.

## Image storage

- DB stores only the public URL (`profilePictureUrl` / `imageUrl`). No binary in Postgres.
- If `AZURE_STORAGE_CONNECTION_STRING` is set and the client starts, new uploads go to Azure Blob. Public URL is the blob URL.
- If the string is empty, or Azure cannot be reached, User/Post Service use the local filesystem (`CONNECTA_STORAGE_DIR`, public base `CONNECTA_STORAGE_PUBLIC_BASE_URL`, default `http://localhost:8080/media`).
- Containers (create in the Storage account; anonymous **Blob** read for `<img>`):
  - `avatars` (`AZURE_STORAGE_CONTAINER_AVATARS`) — profile pictures
  - `posts` (`AZURE_STORAGE_CONTAINER_POSTS`) — post images
- Existing local `/media/**` URLs stay valid; Gateway still serves them. New Blob uploads do not go through `/media`.

## Account flags

- `isActive=false` — deactivated / soft-deleted account (data kept for other services).
- `isBanned=true` — admin ban; login rejected.
- Admin API: ban, unban, deactivate, restore.

## Gateway auth headers

- Gateway validates JWT and sets `User-Id`, `Username`, `User-Role`.
- Gateway strips/overwrites any client-supplied `User-Id` / `Username` / `User-Role`
  (and legacy `X-User-*` variants) before forwarding.
- Public through gateway: `POST /api/auth/register`, `POST /api/auth/login`, `/media/**`, actuator health/info.
- `/ws` handshake requires JWT (`Authorization: Bearer` or `?token=`). Query token is accepted only on `/ws/**` and stripped before forwarding.
- `/api/admin/**` requires JWT role `ADMIN` at the gateway (user-service also enforces).
- Client entrypoint: `http://localhost:8080`

## Private profiles + Social Service

`isPrivate` lives on User. Follow relationship status lives in **Social Service** (not User Service).

Statuses:

- `PENDING` — follow request on a private profile
- `ACCEPTED` — active follow (public follow creates this immediately)

No persistent `DENIED`: reject = delete the pending row (sender may request again).

Flow:

1. Public + Follow → `ACCEPTED` follow
2. Private + Follow → `PENDING` request
3. Owner Accept → `ACCEPTED`
4. Owner Reject → delete request

FE for private profile when viewer is not an accepted follower: picture, displayName, username, Follow / Requested. Full profile after `ACCEPTED`.

Social Service should call User Service (Feign) for `isPrivate` when handling follow.

User Service and Post Service ask Social (`GET /api/social/{userId}/is-following`, and Post uses `GET /api/social/me/following` for batch) whether the viewer has an `ACCEPTED` follow. If Social (or User, for Post `isPrivate`) is down, do not leak: treat the viewer as not a follower / the profile as private.

Home feed is `GET /api/feed` on Social: ACCEPTED followee IDs plus the viewer, then Post `GET /api/posts/by-authors`. If Post is down, Social returns an empty page (not 500).

`USER_FOLLOWED` is published to Azure Service Bus only when a follow becomes `ACCEPTED` (public follow or accept). Fail-soft: missing connection string is a no-op; publish errors do not fail the HTTP request.

## Notifications

Notification Service (`:8085`, `notifications_db`) owns in-app notifications. Gateway route: `/api/notifications/**`.

REST (JWT, current user only; `page`/`size` like other services):

- `GET /api/notifications` — newest first
- `GET /api/notifications/unread-count`
- `PUT /api/notifications/{id}/read` — idempotent; missing or another user's id → **404**
- `PUT /api/notifications/read-all` — **204**

Events on topic `connecta-events` (subscription `notification-service`). Publishers (Post / Social / Message) stay fail-soft. Mapping:

| Event | `type` | `resource_type` | `resource_id` | recipient | actor |
|-------|--------|-----------------|---------------|-----------|-------|
| `POST_LIKED` | `LIKE` | `POST` | `postId` | `postAuthorId` | `actorId` |
| `POST_COMMENTED` | `COMMENT` | `POST` | `postId` | `postAuthorId` | `actorId` |
| `USER_FOLLOWED` | `FOLLOW` | `USER` | `followerId` | `followeeId` | `followerId` |
| `MESSAGE_SENT` | `MESSAGE` | `CONVERSATION` | `conversationId` | `recipientId` | `senderId` |

Self-events and unknown types are ignored. Duplicate Azure `messageId` is stored as `source_message_id` (unique) so retries do not insert twice. Messages are generic English strings; `actorId` is returned for later FE enrichment. No Feign to User Service.

Handler results for the Azure processor: persist → complete; ignore → complete; invalid payload → dead-letter; transient DB errors → abandon (subscription `maxDeliveryCount` → DLQ).

## Direct messages

Message Service (`:8084`, `messages_db`) owns 1:1 conversations. Gateway routes: `/api/conversations/**` and `/ws/**`.

REST (JWT, current user only; `page`/`size` like other services; page 0 = newest messages first):

- `GET /api/conversations`
- `POST /api/conversations/users/{userId}` — **201** new, **200** existing; self → **400**; other user missing / User Service down → **404** / **503**
- `GET /api/conversations/users/{userId}` — **404** if the pair does not exist yet
- `GET/POST /api/conversations/users/{userId}/messages` — content max 2000
- `PUT /api/conversations/users/{userId}/read` — **204**, idempotent

WebSocket: raw STOMP `/ws` (no SockJS). Gateway handshake JWT via Bearer or `?token=`. STOMP `CONNECT` still sends `Authorization: Bearer`. `SEND /app/chat.send` `{ conversationId, content }`; subscribe `/topic/conversations.{conversationId}` (participants only). HTTP send uses the same persist path and also broadcasts. `/user/queue/notifications` is skipped. Broadcast is fail-soft.

Feign only to User Service batch lookup (list enrichment is fail-soft). No Feign to Social. `MESSAGE_SENT` mapping is in the Notifications table above.

## Auth headers (Gateway → services)

| Header | Source |
|--------|--------|
| `User-Id` | JWT `sub` |
| `Username` | JWT `username` |
| `User-Role` | JWT `role` |
