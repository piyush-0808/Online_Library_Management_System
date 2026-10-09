-- ============================================================
-- LIBRARY MANAGEMENT SYSTEM
-- Database Schema
-- ============================================================

-- Create database
CREATE DATABASE IF NOT EXISTS library_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE library_db;


-- ============================================================
-- USERS
-- Authentication and common account information
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    password_salt VARCHAR(32) NOT NULL,
    role ENUM('LIBRARIAN', 'MEMBER') NOT NULL,
    status ENUM('PENDING', 'ACTIVE', 'BLOCKED') NOT NULL DEFAULT 'PENDING',
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(10) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_users_role (role),
    INDEX idx_users_status (status)
);


-- ============================================================
-- MEMBERS
-- Student / Faculty specific information
-- ============================================================

CREATE TABLE IF NOT EXISTS members (
    member_id VARCHAR(20) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    member_type ENUM('STUDENT', 'FACULTY') NOT NULL,
    department VARCHAR(100),
    joined_date DATE NOT NULL,
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,

    CONSTRAINT fk_members_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_members_approved_by
        FOREIGN KEY (approved_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL,

    INDEX idx_members_type (member_type),
    INDEX idx_members_department (department)
);


-- ============================================================
-- BOOKS
-- Library catalogue
-- ============================================================

CREATE TABLE IF NOT EXISTS books (
    book_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    is_reference BOOLEAN NOT NULL DEFAULT FALSE,
    publisher VARCHAR(150),
    publication_year INT,
    total_copies INT NOT NULL,
    available_copies INT NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_books_total_copies
        CHECK (total_copies > 0),

    CONSTRAINT chk_books_available_copies
        CHECK (
            available_copies >= 0
            AND available_copies <= total_copies
        ),

    INDEX idx_books_title (title),
    INDEX idx_books_author (author),
    INDEX idx_books_category (category),
    INDEX idx_books_isbn (isbn)
);

-- Keep existing installations compatible when the schema is re-applied.
SET @has_reference_flag = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'books'
      AND column_name = 'is_reference'
);
SET @reference_flag_migration = IF(
    @has_reference_flag = 0,
    'ALTER TABLE books ADD COLUMN is_reference BOOLEAN NOT NULL DEFAULT FALSE AFTER category',
    'SELECT 1'
);
PREPARE reference_flag_stmt FROM @reference_flag_migration;
EXECUTE reference_flag_stmt;
DEALLOCATE PREPARE reference_flag_stmt;


-- ============================================================
-- ISSUES
-- Book borrowing records
-- ============================================================

CREATE TABLE IF NOT EXISTS issues (
    issue_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(20) NOT NULL,
    book_id BIGINT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE NULL,
    status ENUM('ISSUED', 'RETURNED', 'OVERDUE') NOT NULL DEFAULT 'ISSUED',
    issued_by BIGINT NOT NULL,
    returned_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_issues_member
        FOREIGN KEY (member_id)
        REFERENCES members(member_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_issues_book
        FOREIGN KEY (book_id)
        REFERENCES books(book_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_issues_issued_by
        FOREIGN KEY (issued_by)
        REFERENCES users(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_issues_returned_by
        FOREIGN KEY (returned_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL,

    INDEX idx_issues_member (member_id),
    INDEX idx_issues_book (book_id),
    INDEX idx_issues_status (status),
    INDEX idx_issues_due_date (due_date)
);


-- ============================================================
-- RESERVATIONS
-- Waiting queue for unavailable books
-- ============================================================

CREATE TABLE IF NOT EXISTS reservations (
    reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(20) NOT NULL,
    book_id BIGINT NOT NULL,
    reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('WAITING', 'READY', 'FULFILLED', 'CANCELLED') NOT NULL DEFAULT 'WAITING',

    CONSTRAINT fk_reservations_member
        FOREIGN KEY (member_id)
        REFERENCES members(member_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_reservations_book
        FOREIGN KEY (book_id)
        REFERENCES books(book_id)
        ON DELETE CASCADE,

    INDEX idx_reservations_book_queue
        (book_id, status, reservation_date),

    INDEX idx_reservations_member
        (member_id, status)
);


-- ============================================================
-- FINES
-- ============================================================

CREATE TABLE IF NOT EXISTS fines (
    fine_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id BIGINT NOT NULL UNIQUE,
    member_id VARCHAR(20) NOT NULL,
    days_late INT NOT NULL DEFAULT 0,
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status ENUM('UNPAID', 'PAID') NOT NULL DEFAULT 'UNPAID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP NULL,
    paid_by BIGINT NULL,

    CONSTRAINT fk_fines_issue
        FOREIGN KEY (issue_id)
        REFERENCES issues(issue_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_fines_member
        FOREIGN KEY (member_id)
        REFERENCES members(member_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_fines_paid_by
        FOREIGN KEY (paid_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL,

    CONSTRAINT chk_fines_days_late
        CHECK (days_late >= 0),

    CONSTRAINT chk_fines_amount
        CHECK (amount >= 0),

    INDEX idx_fines_member_status
        (member_id, status),

    INDEX idx_fines_status
        (status)
);


-- ============================================================
-- DEFAULT LIBRARIAN
-- username: admin
-- password: admin123
--
-- Password hash generated using:
-- SHA-256(salt + password)
-- ============================================================

INSERT INTO users
(
    username,
    password_hash,
    password_salt,
    role,
    status,
    full_name,
    email,
    phone
)
SELECT
    'admin',
    'f64ac4ea937878afb4f803f962f46c5722a602fb774f7e944c59a05cda59c07d',
    '0622044f39f77bde4af6dff19e811eac',
    'LIBRARIAN',
    'ACTIVE',
    'System Administrator',
    'admin@library.local',
    '9999999999'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE username = 'admin'
);


-- ============================================================
-- MYSQL APPLICATION USER
-- ============================================================

CREATE USER IF NOT EXISTS
    'libuser'@'localhost'
IDENTIFIED BY 'lib123';

GRANT ALL PRIVILEGES
    ON library_db.*
    TO 'libuser'@'localhost';

FLUSH PRIVILEGES;


-- ============================================================
-- VERIFICATION
-- ============================================================

USE library_db;

SELECT
    user_id,
    username,
    role,
    status,
    full_name
FROM users
WHERE username = 'admin';

SHOW TABLES;
