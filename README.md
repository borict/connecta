# Connecta

Web društvena mreža zasnovana na mikroservisnoj arhitekturi.

## Stek

- Java 21, Spring Boot 3, Spring Cloud Gateway
- React + TypeScript (Vite)
- PostgreSQL, Redis, Zipkin
- Azure Service Bus (asinhroni eventi)
- Azure Blob Storage (profilne i post slike)

## Struktura

```
backend/          Maven multimodule (gateway + 5 servisa)
frontend/         React aplikacija
scripts/          SQL inicijalizacija baza
docker-compose.yml
```


| Servis               | Port |
| -------------------- | ---- |
| api-gateway          | 8080 |
| user-service         | 8081 |
| post-service         | 8082 |
| social-service       | 8083 |
| message-service      | 8084 |
| notification-service | 8085 |
| frontend (dev)       | 5173 |
| postgres             | 5433 |
| redis                | 6379 |
| zipkin               | 9411 |




## Preduslovi

- JDK 21
- Maven 3.9+
- Node.js 20+
- Docker Desktop
- Azure nalog (Service Bus namespace, topic, subscription)



## Lokalni setup



### 1. Kloniranje

```bash
git clone https://github.com/borict/connecta.git
cd connecta
```



### 2. Environment

```bash
cp .env.example .env
```

U `.env` unesi Azure Service Bus **Primary connection string** (namespace → Shared access policies → RootManageSharedAccessKey).

Potrebne Azure entitete:

- Namespace (npr. `connecta-bus`)
- Topic: `connecta-events`
- Subscription: `notification-service`



### 3. Infrastruktura

```bash
docker compose up -d
```

Provera baza:

```bash
docker exec -it connecta-postgres psql -U connecta -d postgres -c "\l"
```

Zipkin UI: [http://localhost:9411](http://localhost:9411)

### 4. Backend

```bash
cd backend
mvn clean install
```

Pokretanje pojedinačnog servisa (primer):

```bash
mvn -pl user-service spring-boot:run
```

Ili pokreni module iz IntelliJ IDEA (Community Edition je dovoljan).

### 5. Frontend

```bash
cd frontend
npm install
npm run dev
```

Aplikacija: [http://localhost:5173](http://localhost:5173)

## Napomene

- `.env` se ne commituje. Koristi `.env.example` kao šablon.
- Azure Service Bus i Azure Blob Storage nisu u Docker Compose-u (cloud servisi).
- Domen odluke (User model, admin, privatni profili, JWT headeri, post slike, notifikacije, poruke): [docs/domain-decisions.md](docs/domain-decisions.md).
- Follow model (PENDING/ACCEPTED, feed, `USER_FOLLOWED`): [docs/social-follow-model.md](docs/social-follow-model.md).
- Seed admin (User Service Flyway): username `admin`, password `Admin123!`.
- API entrypoint: Gateway `http://localhost:8080` (User `8081`, Post `8082`, Social `8083`, Message `8084`, Notification `8085`).
- Swagger (User Service): [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- Swagger (Post Service): [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
- Swagger (Social Service): [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)
- Swagger (Message Service): [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)
- Swagger (Notification Service): [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)
- Gateway **nema** Swagger UI.
- Faza 1: User Service + Gateway + Swagger.
- Faza 2: Post Service (CRUD, likes, comments, local post images, User Service enrichment, Azure Service Bus publisher).
- Faza 3: Social Service (follow/unfollow, private-profile requests, lists, home feed, `USER_FOLLOWED`).
- Faza 4: Notification Service (REST lista/read/unread-count, Azure consumer na `connecta-events` / `notification-service`, fail-soft publisheri).
- Faza 5: Message Service (1:1 konverzacije, REST poruke, STOMP `/ws`, `MESSAGE_SENT`).
- Faza 6: React frontend (`:5173`) kroz Gateway — auth, feed, profil, follow, notifikacije, chat, `/admin`.

### Smoke test (Faza 1)

1. `docker compose up -d`
2. Pokreni `user-service` i `api-gateway` (isti `JWT_SECRET`)
3. Swagger: login kao `admin` / `Admin123!` → Authorize (Bearer token) → `GET /api/users/me`
4. Preko Gateway-a: `POST http://localhost:8080/api/auth/login` pa `GET http://localhost:8080/api/users/me` sa tokenom

### Smoke test (Faza 2)

1. `docker compose up -d` (Postgres host port **5433**)
2. Pokreni `user-service`, `post-service` i `api-gateway` (isti `JWT_SECRET`)
3. User Swagger: login (`admin` / `Admin123!` ili registrovani nalog) → kopiraj token
4. Post Swagger → **Authorize** (Bearer) → `POST /api/posts`: JSON u `data` (npr. `{ "content": "Hello Connecta!" }`); sliku ostavi praznu i **ne** štikliraj “Send empty value”
5. `GET /api/posts/{postId}` — `authorUsername` popunjen ako User Service radi; inače `null` uz `authorId`
6. Like: `POST .../likes` dva puta → isti `count`; unlike `DELETE .../likes` → 204 i kad nije lajkovano
7. Comment: `POST .../comments` → list → `DELETE /api/posts/comments/{commentId}` (samo autor komentara)
8. Delete post: drugi user → 403; autor → 204
9. Gateway: `http://localhost:8080/api/posts/**` sa Bearer tokenom; slika na `http://localhost:8080/media/posts/...`
10. Bez JWT-a → 401 `ApiErrorResponse`. Azure eventi (`POST_LIKED` / `POST_COMMENTED`) se proveravaju kroz Notification Service (vidi smoke Faza 4).

### Smoke test (Faza 3)

1. `docker compose up -d` (Postgres host port **5433**)
2. Pokreni `user-service`, `post-service`, `social-service` i `api-gateway` (isti `JWT_SECRET`)
3. User Swagger: login (`admin` / `Admin123!` ili dva registrovana naloga) → kopiraj token
4. Social Swagger [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) → **Authorize** (Bearer). Tagovi: **Feed**, **Followers**, **Following**, **Social**
5. `POST /api/social/{userId}` na **javni** profil → **201** `ACCEPTED`; ponovo → **200** (idempotentno)
6. `POST /api/social/{userId}` na **privatni** profil → **201** `PENDING`; vlasnik: `GET /api/social/me/requests` → `POST .../accept` (**200** `ACCEPTED`) ili `POST .../reject` (**204**)
7. `GET /api/social/{userId}/followers` i `.../following` — samo `ACCEPTED`; `GET .../stats`; `GET .../is-following` → `{ following, pending }`
8. Self-follow → **400**. `DELETE /api/social/{userId}` → **204** (i kad relacija ne postoji)
9. Privatni profil: ko **nije** ACCEPTED follower vidi limited User polja i **403** na `GET /api/posts/user/{userId}`; posle accept-a vidi pun profil i postove
10. `GET /api/feed` — tvoji postovi + ACCEPTED followee-i; ako Post Service ne radi → prazna strana **200**. Gateway: `http://localhost:8080/api/social/**` i `http://localhost:8080/api/feed`
11. Bez JWT-a → **401**. `USER_FOLLOWED` se proverava kroz Notification Service na `ACCEPTED` (vidi smoke Faza 4).

### Smoke test (Faza 4)

1. `docker compose up -d` (Postgres host port **5433**)
2. Pokreni `user-service`, `post-service`, `social-service`, `notification-service` i `api-gateway` (isti `JWT_SECRET`)
3. User Swagger: login (`admin` / `Admin123!` ili dva registrovana naloga) → kopiraj token
4. Notification Swagger [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html) → **Authorize** (Bearer). Tag: **Notifications**
5. `GET /api/notifications` → prazna strana **200** `{ content, page, size, totalElements, totalPages }`; `GET /api/notifications/unread-count` → `{ unreadCount: 0 }`
6. `PUT /api/notifications/{randomUuid}/read` → **404** `ApiErrorResponse`; `PUT /api/notifications/read-all` → **204**
7. Gateway: `http://localhost:8080/api/notifications/**` sa Bearer tokenom
8. Bez JWT-a → **401** `ApiErrorResponse`
9. End-to-end Azure: setuj `AZURE_SERVICEBUS_CONNECTION_STRING` (topic `connecta-events`, subscription `notification-service`). Bez stringa consumer je no-op i lista ostaje prazna; like/follow i dalje uspevaju. Sa stringom: drugi user like/comment na tvoj post, ili `ACCEPTED` follow → tvoja lista dobija `LIKE` / `COMMENT` / `FOLLOW`; unread-count raste; `PUT .../{id}/read` → `read: true`; ponovo read → **200**. Self-like/comment/follow ne prave notifikaciju. `MESSAGE_SENT` vidi smoke Faza 5.

### Smoke test (Faza 5)

1. `docker compose up -d` (Postgres host port **5433**)
2. Pokreni `user-service`, `message-service`, `notification-service` i `api-gateway` (isti `JWT_SECRET`). IntelliJ: Application `MessageServiceApplication`, working directory `backend/message-service`, EnvFile plugin na root `.env` (Spring sam ne učitava `.env`)
3. User Swagger: login dva naloga (`admin` / `Admin123!` i registrovani user) → kopiraj tokene
4. Message Swagger [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) → **Authorize** (Bearer). Tagovi: **Conversations**, **Messages**
5. `POST /api/conversations/users/{otherUserId}` → **201**; ponovo → **200**. Self → **400**. Nepostojeći user → **404**
6. `GET /api/conversations` — `unreadCount`, last message, `otherUsername` (null ako User Service ne radi)
7. `POST /api/conversations/users/{otherUserId}/messages` `{ "content": "Hey" }` → **201**; `GET .../messages` page 0 = najnovije prvo; `PUT .../read` → **204**
8. Gateway REST: `http://localhost:8080/api/conversations/**` sa Bearer tokenom. Bez JWT-a → **401** `ApiErrorResponse`
9. WebSocket (raw STOMP, **bez SockJS**): handshake `ws://localhost:8080/ws?token=<JWT>` ili `Authorization: Bearer` na upgrade. STOMP `CONNECT` i dalje šalje `Authorization: Bearer <JWT>`. `SUBSCRIBE /topic/conversations.{conversationId}` (samo učesnik). `SEND /app/chat.send` body `{ "conversationId": "...", "content": "hi" }` → isti `MessageResponse` kao HTTP
10. Dve sesije: A pošalje preko WS, B vidi na topic-u. HTTP send takođe stigne na WS. Gateway `/ws` bez tokena → **401**
11. Azure: sa `AZURE_SERVICEBUS_CONNECTION_STRING`, send (HTTP ili WS) → recipient vidi notifikaciju `MESSAGE` / `CONVERSATION` / `"Someone sent you a message"`; self-send se ne dešava na 1:1. Bez stringa send i dalje uspeva (fail-soft)

### Smoke test (Faza 6)

1. `docker compose up -d` (Postgres host port **5433**)
2. Pokreni svih 6 Spring app (isti `JWT_SECRET`) i `cd frontend && npm run dev` → [http://localhost:5173](http://localhost:5173)
3. Register ili login. FE ide samo kroz Gateway `http://localhost:8080` (nikad direktno na `:8081`–`:8085`)
4. Home: feed (`GET /api/feed`, `page`/`size`, default size **20**), novi post, like, komentar, brisanje svog posta
5. Profil `/u/:username`, follow / Requested / Unfollow, followers/following, follow zahtevi `/requests`, pretraga, izmena `/settings`
6. Zvono: unread badge + lista; klik označava pročitano. Messages: crvena tačka = zbir `unreadCount` (nije zvono). Chat HTTP + STOMP uživo (`ws://localhost:8080/ws?token=<JWT>`, bez SockJS)
7. Liste na dnu učitavaju sledeći `page` (infinite scroll)
8. Kao `USER`: nema **Admin** u navbaru; `/admin` redirect na Home. Kao `admin` / `Admin123!`: `/admin` lista korisnike; Ban / Unban / Deactivate / Restore; sopstveni nalog se ne menja ovde


