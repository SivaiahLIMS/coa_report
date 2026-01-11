-- Database initialization script for COA Report Management System
-- Run this script to set up initial data

-- Create database if not exists (run as postgres user)
-- CREATE DATABASE coa_report_db;

-- Insert sample company
INSERT INTO companies (id, name, code, address, contact_email, contact_phone, active, created_at, updated_at)
VALUES (1, 'Pharma MNC Corp', 'PMC001', '123 Medical Street, New York, NY', 'contact@pharmamnc.com', '+1-555-0100', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert sample branches
INSERT INTO branches (id, name, code, location, contact_email, contact_phone, company_id, active, created_at, updated_at)
VALUES
(1, 'New York Branch', 'NYC001', 'New York, NY', 'nyc@pharmamnc.com', '+1-555-0101', 1, true, NOW(), NOW()),
(2, 'Boston Branch', 'BOS001', 'Boston, MA', 'boston@pharmamnc.com', '+1-555-0102', 1, true, NOW(), NOW()),
(3, 'San Francisco Branch', 'SF001', 'San Francisco, CA', 'sf@pharmamnc.com', '+1-555-0103', 1, true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert sample users (password for all users: 'password123')
-- Password is BCrypt hashed: $2a$10$N9qo8uLOickgx2ZMRZoMve6U7IqZLQ6KlWrKKAGGqHQPVXqLGvk9q
INSERT INTO users (id, username, email, password, first_name, last_name, department, role, branch_id, active, created_at, updated_at)
VALUES
(1, 'admin', 'admin@pharmamnc.com', '$2a$10$N9qo8uLOickgx2ZMRZoMve6U7IqZLQ6KlWrKKAGGqHQPVXqLGvk9q', 'System', 'Administrator', 'IT', 'ADMIN', 1, true, NOW(), NOW()),
(2, 'branchadmin', 'branchadmin@pharmamnc.com', '$2a$10$N9qo8uLOickgx2ZMRZoMve6U7IqZLQ6KlWrKKAGGqHQPVXqLGvk9q', 'John', 'Smith', 'Quality Control', 'BRANCH_ADMIN', 1, true, NOW(), NOW()),
(3, 'manager', 'manager@pharmamnc.com', '$2a$10$N9qo8uLOickgx2ZMRZoMve6U7IqZLQ6KlWrKKAGGqHQPVXqLGvk9q', 'Jane', 'Doe', 'Quality Assurance', 'MANAGER', 1, true, NOW(), NOW()),
(4, 'user', 'user@pharmamnc.com', '$2a$10$N9qo8uLOickgx2ZMRZoMve6U7IqZLQ6KlWrKKAGGqHQPVXqLGvk9q', 'Bob', 'Johnson', 'Laboratory', 'USER', 1, true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Reset sequences
SELECT setval('companies_id_seq', (SELECT MAX(id) FROM companies));
SELECT setval('branches_id_seq', (SELECT MAX(id) FROM branches));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
