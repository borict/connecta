# Social Service — follow model (Phase 3)

Follow status lives here, not in User Service.

## follows (or follow_requests unified)

Recommended shape:

- `follower_id` UUID
- `followee_id` UUID
- `status` VARCHAR — `PENDING` | `ACCEPTED`
- audit columns via AuditEntity

Rules:

- Public profile (`User.isPrivate = false`): Follow → insert `ACCEPTED`
- Private profile: Follow → insert `PENDING`
- Accept: `PENDING` → `ACCEPTED`
- Reject: delete row (no DENIED status)
- Unfollow: delete `ACCEPTED` row

User Service is source of truth for `isPrivate`; Social calls it via Feign when handling follow.

User and Post ask Social whether the viewer is an `ACCEPTED` follower before showing a full private profile or that user's posts. If Social is down, they hide private content instead of leaking it.

Feed (`GET /api/feed`) lives in Social: ACCEPTED following IDs + self → Post `by-authors`. Post down → empty page.

`USER_FOLLOWED` is sent only when status becomes `ACCEPTED` (not on PENDING, reject, unfollow, or idempotent replay). Notification Service maps it to a `FOLLOW` row for the followee (`docs/domain-decisions.md`).
