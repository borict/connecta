# Connecta — domain decisions (User / Auth / Social)

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
| profilePictureUrl | no | set only via upload → Azure Blob |
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

- FE route: `/admin` (ADMIN only) — later.
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
- Now: local filesystem via `ProfilePictureStorage` (`CONNECTA_STORAGE_DIR`), served at `/media/**`.
- Later: swap implementation for Azure Blob; DB still keeps only `profilePictureUrl`.
- Register (`POST /api/auth/register`): optional part `profilePicture` + JSON part `data`.
- Login: `POST /api/auth/login`.
- Missing picture → FE placeholder / initials.

## Account flags

- `isActive=false` — deactivated / soft-deleted account (data kept for other services).
- `isBanned=true` — admin ban; login rejected.
- Admin API: ban, unban, deactivate, restore.

## Gateway auth headers

- Gateway validates JWT and sets `User-Id`, `Username`, `User-Role`.
- Gateway must strip/overwrite any client-supplied `User-Id` / `Username` / `User-Role`
  (and legacy `X-User-*` variants) before forwarding.

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

## Auth headers (Gateway → services)

| Header | Source |
|--------|--------|
| `User-Id` | JWT `sub` |
| `Username` | JWT `username` |
| `User-Role` | JWT `role` |
