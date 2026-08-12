# Connecta

Web društvena mreža zasnovana na mikroservisnoj arhitekturi.

## Stek

- Java 21, Spring Boot 3, Spring Cloud Gateway
- React + TypeScript (Vite)
- PostgreSQL, Redis, Zipkin
- Azure Service Bus (asinhroni eventi)

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
| postgres             | 5432 |
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
- Azure Service Bus nije u Docker Compose-u (cloud servis).
- Trenutno je skeleton: servisi se dižu, poslovna logika se dodaje po fazama.

