-- Creates one database per microservice (logical isolation).
-- Runs automatically the first time the Postgres container starts.

CREATE DATABASE users_db;
CREATE DATABASE posts_db;
CREATE DATABASE socials_db;
CREATE DATABASE messages_db;
CREATE DATABASE notifications_db;
