-- MySQL Community Server 8.0.46. Ejecutar una vez con una cuenta administradora.
-- No borra datos existentes. Los CHECK son aplicados por MySQL 8.0.16 y posteriores.
CREATE DATABASE IF NOT EXISTS inventario_ues CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE inventario_ues;

CREATE TABLE IF NOT EXISTS productos (
    code VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL DEFAULT '',
    category VARCHAR(80) NOT NULL,
    price DECIMAL(11,2) NOT NULL,
    quantity INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_code CHECK (CHAR_LENGTH(TRIM(code)) > 0 AND code REGEXP '^[A-Z0-9_-]+$'),
    CONSTRAINT ck_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_category CHECK (CHAR_LENGTH(TRIM(category)) > 0),
    CONSTRAINT ck_price CHECK (price >= 0),
    CONSTRAINT ck_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_version CHECK (version >= 0),
    INDEX ix_name (name),
    INDEX ix_category (category),
    INDEX ix_quantity (quantity)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS usuarios (
    id INT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    salt VARCHAR(32) NOT NULL,
    password_hash VARCHAR(64) NOT NULL,
    CONSTRAINT ck_single_admin CHECK (id = 1),
    CONSTRAINT ck_username CHECK (CHAR_LENGTH(TRIM(username)) > 0)
) ENGINE=InnoDB;

