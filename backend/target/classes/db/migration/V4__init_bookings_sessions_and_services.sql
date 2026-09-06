-- ===================================================================
-- EVShare 3D Platform - Flyway Migration V4
-- Module: Bookings, Usage Sessions, Inspections & Workshop Services
-- Tables: bookings, usage_sessions, vehicle_inspections, vehicle_services
-- ===================================================================

CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    estimated_cost DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vehicle_time (vehicle_id, start_time, end_time),
    CONSTRAINT fk_booking_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE usage_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    start_odometer DECIMAL(10, 2) NOT NULL,
    end_odometer DECIMAL(10, 2) NULL,
    start_battery INT NOT NULL,
    end_battery INT NULL,
    check_in_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    check_out_time TIMESTAMP NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_session_start_battery CHECK (start_battery BETWEEN 0 AND 100),
    CONSTRAINT chk_session_end_battery CHECK (end_battery BETWEEN 0 AND 100),
    CONSTRAINT fk_session_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicle_inspections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usage_session_id BIGINT NOT NULL,
    inspector_user_id BIGINT NOT NULL,
    inspection_type VARCHAR(20) NOT NULL,
    condition_mesh_flags JSON NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inspection_session FOREIGN KEY (usage_session_id) REFERENCES usage_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_inspection_inspector FOREIGN KEY (inspector_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicle_services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    service_type VARCHAR(40) NOT NULL,
    description TEXT NOT NULL,
    technician_user_id BIGINT NOT NULL,
    cost_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    odometer_at_service DECIMAL(10, 2) NOT NULL,
    service_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_service_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_service_technician FOREIGN KEY (technician_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
