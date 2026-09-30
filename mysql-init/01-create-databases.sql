-- Each microservice owns its own database (database-per-service pattern) — no cross-service joins.
CREATE DATABASE IF NOT EXISTS labourse_auth;
CREATE DATABASE IF NOT EXISTS labourse_customer;
CREATE DATABASE IF NOT EXISTS labourse_labour;
CREATE DATABASE IF NOT EXISTS labourse_job;
CREATE DATABASE IF NOT EXISTS labourse_payment;
CREATE DATABASE IF NOT EXISTS labourse_rating;
CREATE DATABASE IF NOT EXISTS labourse_admin;
CREATE DATABASE IF NOT EXISTS labourse_kyc;
CREATE DATABASE IF NOT EXISTS labourse_chat;
