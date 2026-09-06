-- ===================================================================
-- EVShare 3D Platform - Flyway Migration V2
-- Module: Digital Twin Vehicles, Ownership Groups & Equity Shares
-- Tables: vehicles, ownership_groups, ownership_shares
-- ===================================================================

CREATE TABLE vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vin VARCHAR(50) NOT NULL UNIQUE,
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    model_name VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(50) NOT NULL,
    model_3d_asset_path VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    battery_level INT NOT NULL DEFAULT 100,
    odometer_km DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    stall_location_code VARCHAR(30) NOT NULL DEFAULT 'BAY-01',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_vehicle_battery CHECK (battery_level BETWEEN 0 AND 100),
    CONSTRAINT chk_vehicle_odometer CHECK (odometer_km >= 0.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ownership_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(100) NOT NULL,
    vehicle_id BIGINT NOT NULL UNIQUE,
    formation_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_ownership_group_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ownership_shares (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    percentage DECIMAL(5, 2) NOT NULL,
    share_certificate_number VARCHAR(100) NOT NULL UNIQUE,
    acquired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_share_percentage CHECK (percentage > 0.00 AND percentage <= 100.00),
    CONSTRAINT uk_share_group_user UNIQUE (group_id, user_id),
    CONSTRAINT fk_share_group FOREIGN KEY (group_id) REFERENCES ownership_groups(id) ON DELETE RESTRICT,
    CONSTRAINT fk_share_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
