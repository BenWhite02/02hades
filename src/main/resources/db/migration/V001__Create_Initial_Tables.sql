-- =============================================================================
-- File: src/main/resources/db/migration/V001__Create_Initial_Tables.sql
-- 🔥 HADES BACKEND - DATABASE MIGRATION
-- Author: Sankhadeep Banerjee
-- Project: Hades - Kotlin + Spring Boot Backend (The Powerful Decision Engine)
-- Purpose: Create initial tables for users and tenants (MISSING FROM CURRENT REPO)
-- =============================================================================

-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- TENANTS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS tenants (
    -- Primary identification
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Business information
    name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100) UNIQUE NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    website_url VARCHAR(500),
    logo_url VARCHAR(500),
    
    -- Contact information
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    billing_email VARCHAR(255),
    support_email VARCHAR(255),
    
    -- Address information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    
    -- Subscription and billing
    subscription_tier VARCHAR(50) NOT NULL DEFAULT 'TRIAL',
    subscription_status VARCHAR(50) NOT NULL DEFAULT 'TRIAL',
    trial_ends_at TIMESTAMP,
    subscription_starts_at TIMESTAMP,
    subscription_ends_at TIMESTAMP,
    billing_cycle VARCHAR(20) DEFAULT 'MONTHLY',
    
    -- Status and settings
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en',
    date_format VARCHAR(20) DEFAULT 'MM/dd/yyyy',
    currency VARCHAR(10) DEFAULT 'USD',
    
    -- Security settings
    password_policy JSONB DEFAULT '{}',
    session_timeout INTEGER DEFAULT 3600,
    max_failed_logins INTEGER DEFAULT 5,
    account_lockout_duration INTEGER DEFAULT 1800,
    require_email_verification BOOLEAN DEFAULT TRUE,
    
    -- Features and limits
    features JSONB DEFAULT '{}',
    usage_limits JSONB DEFAULT '{}',
    current_usage JSONB DEFAULT '{}',
    
    -- API and integration
    api_key VARCHAR(255) UNIQUE,
    webhook_url VARCHAR(500),
    webhook_secret VARCHAR(255),
    
    -- Metadata
    metadata JSONB DEFAULT '{}',
    settings JSONB DEFAULT '{}',
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    
    -- Constraints
    CONSTRAINT tenants_name_check CHECK (LENGTH(name) > 0),
    CONSTRAINT tenants_subdomain_check CHECK (LENGTH(subdomain) > 0 AND subdomain ~ '^[a-z0-9-]+$'),
    CONSTRAINT tenants_contact_email_check CHECK (contact_email ~ '^[^@]+@[^@]+\.[^@]+$'),
    CONSTRAINT tenants_subscription_tier_check CHECK (subscription_tier IN ('TRIAL', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE', 'CUSTOM')),
    CONSTRAINT tenants_subscription_status_check CHECK (subscription_status IN ('TRIAL', 'ACTIVE', 'SUSPENDED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT tenants_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING'))
);

-- =============================================================================
-- USERS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    -- Primary identification
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    external_id VARCHAR(255),
    
    -- Authentication fields
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email_verified_at TIMESTAMP,
    email_verification_token VARCHAR(255),
    email_verification_expires_at TIMESTAMP,
    
    -- Personal information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200),
    phone VARCHAR(20),
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en',
    
    -- Profile information
    avatar_url VARCHAR(500),
    title VARCHAR(100),
    department VARCHAR(100),
    bio TEXT,
    
    -- Role and permissions
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    permissions TEXT[], -- Array of permission strings
    
    -- Status and security
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    account_locked_until TIMESTAMP,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    last_password_change_at TIMESTAMP,
    
    -- Password reset
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMP,
    
    -- Two-factor authentication
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    two_factor_backup_codes TEXT[],
    
    -- Preferences and settings
    preferences JSONB DEFAULT '{}',
    settings JSONB DEFAULT '{}',
    metadata JSONB DEFAULT '{}',
    notes TEXT,
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    
    -- Constraints
    CONSTRAINT users_email_check CHECK (email ~ '^[^@]+@[^@]+\.[^@]+$'),
    CONSTRAINT users_first_name_check CHECK (LENGTH(first_name) > 0),
    CONSTRAINT users_last_name_check CHECK (LENGTH(last_name) > 0),
    CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN', 'MANAGER', 'VIEWER')),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING', 'DELETED')),
    CONSTRAINT users_failed_attempts_check CHECK (failed_login_attempts >= 0)
);

-- =============================================================================
-- INDEXES FOR PERFORMANCE
-- =============================================================================

-- Tenant indexes
CREATE INDEX idx_tenants_subdomain ON tenants (subdomain);
CREATE INDEX idx_tenants_contact_email ON tenants (contact_email);
CREATE INDEX idx_tenants_status ON tenants (status);
CREATE INDEX idx_tenants_subscription_tier ON tenants (subscription_tier);
CREATE INDEX idx_tenants_subscription_status ON tenants (subscription_status);
CREATE INDEX idx_tenants_created_at ON tenants (created_at);
CREATE INDEX idx_tenants_trial_ends_at ON tenants (trial_ends_at) WHERE trial_ends_at IS NOT NULL;
CREATE INDEX idx_tenants_subscription_ends_at ON tenants (subscription_ends_at) WHERE subscription_ends_at IS NOT NULL;

-- User indexes
CREATE UNIQUE INDEX idx_users_tenant_email ON users (tenant_id, email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_external_id ON users (external_id) WHERE external_id IS NOT NULL;
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_last_login ON users (last_login_at);
CREATE INDEX idx_users_created_at ON users (created_at);
CREATE INDEX idx_users_email_verification_token ON users (email_verification_token) WHERE email_verification_token IS NOT NULL;
CREATE INDEX idx_users_password_reset_token ON users (password_reset_token) WHERE password_reset_token IS NOT NULL;

-- Composite indexes for common queries
CREATE INDEX idx_users_tenant_status ON users (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_tenant_role ON users (tenant_id, role) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_login ON users (tenant_id, email, status) WHERE deleted_at IS NULL;

-- =============================================================================
-- TRIGGERS FOR UPDATED_AT
-- =============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for tenants
CREATE TRIGGER update_tenants_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Triggers for users
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- DEFAULT DATA
-- =============================================================================

-- Insert default tenant (for development)
INSERT INTO tenants (
    id,
    name,
    subdomain,
    display_name,
    description,
    contact_email,
    subscription_tier,
    subscription_status,
    status,
    api_key,
    created_by
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Default Tenant',
    'default',
    'Default Organization',
    'Default tenant for development and testing',
    'admin@default.dev',
    'ENTERPRISE',
    'ACTIVE',
    'ACTIVE',
    'default-api-key-' || encode(gen_random_bytes(16), 'hex'),
    'SYSTEM'
) ON CONFLICT (subdomain) DO NOTHING;

-- Insert default super admin user
INSERT INTO users (
    id,
    tenant_id,
    email,
    password_hash,
    first_name,
    last_name,
    display_name,
    role,
    status,
    email_verified_at,
    created_by
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'admin@default.dev',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfyIZcT5OW.EJXW', -- 'password123'
    'System',
    'Administrator',
    'System Admin',
    'SUPER_ADMIN',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    'SYSTEM'
) ON CONFLICT DO NOTHING;

-- =============================================================================
-- COMMENTS FOR DOCUMENTATION
-- =============================================================================

-- Table comments
COMMENT ON TABLE tenants IS 'Multi-tenant organizations with subscription management';
COMMENT ON TABLE users IS 'User accounts with multi-tenant support and comprehensive authentication';

-- Column comments for tenants
COMMENT ON COLUMN tenants.id IS 'Unique tenant identifier (UUID)';
COMMENT ON COLUMN tenants.subdomain IS 'Unique subdomain for tenant (used for routing)';
COMMENT ON COLUMN tenants.subscription_tier IS 'Subscription plan level (TRIAL, BASIC, PROFESSIONAL, ENTERPRISE, CUSTOM)';
COMMENT ON COLUMN tenants.api_key IS 'API key for external integrations';

-- Column comments for users
COMMENT ON COLUMN users.id IS 'Unique user identifier (UUID)';
COMMENT ON COLUMN users.tenant_id IS 'Reference to tenant (multi-tenancy)';
COMMENT ON COLUMN users.email IS 'User email address (unique within tenant)';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN users.role IS 'User role (USER, ADMIN, SUPER_ADMIN, MANAGER, VIEWER)';
COMMENT ON COLUMN users.permissions IS 'Array of specific permissions';
COMMENT ON COLUMN users.failed_login_attempts IS 'Count of consecutive failed login attempts';
COMMENT ON COLUMN users.account_locked_until IS 'Account lock expiration timestamp';

-- Performance hints
COMMENT ON INDEX idx_users_tenant_email IS 'Unique constraint for email within tenant - critical for authentication';
COMMENT ON INDEX idx_users_login IS 'Optimized index for login queries';

-- =============================================================================
-- VIEWS FOR COMMON QUERIES
-- =============================================================================

-- Active users view
CREATE OR REPLACE VIEW active_users AS
SELECT 
    u.*,
    t.name as tenant_name,
    t.subdomain as tenant_subdomain
FROM users u
JOIN tenants t ON u.tenant_id = t.id
WHERE u.status = 'ACTIVE' 
  AND u.deleted_at IS NULL 
  AND t.status = 'ACTIVE'
  AND t.deleted_at IS NULL;

-- Tenant statistics view
CREATE OR REPLACE VIEW tenant_stats AS
SELECT 
    t.id,
    t.name,
    t.subdomain,
    t.subscription_tier,
    t.status,
    COUNT(u.id) as user_count,
    COUNT(CASE WHEN u.status = 'ACTIVE' THEN 1 END) as active_user_count,
    COUNT(CASE WHEN u.role = 'ADMIN' THEN 1 END) as admin_count,
    MAX(u.last_login_at) as last_user_login,
    t.created_at,
    t.updated_at
FROM tenants t
LEFT JOIN users u ON t.id = u.tenant_id AND u.deleted_at IS NULL
WHERE t.deleted_at IS NULL
GROUP BY t.id, t.name, t.subdomain, t.subscription_tier, t.status, t.created_at, t.updated_at;