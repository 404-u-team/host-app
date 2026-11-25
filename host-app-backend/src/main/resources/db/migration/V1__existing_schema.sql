-- A fresh database and a database previously managed by Hibernate start here.
CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY,
    name varchar(255) NOT NULL,
    surname varchar(255) NOT NULL,
    email varchar(255) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified boolean NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_code_hash varchar(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_code_expires_at timestamptz;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_code_sent_at timestamptz;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_code_attempts integer NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS devices (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id),
    ip varchar(255),
    user_agent varchar(255),
    token_hash varchar(255) NOT NULL,
    session_valid_until timestamptz NOT NULL,
    session_created_at timestamptz NOT NULL,
    session_last_used_at timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS devices_token_hash_idx ON devices(token_hash);
CREATE INDEX IF NOT EXISTS devices_user_idx ON devices(user_id);

CREATE TABLE IF NOT EXISTS servers (
    id uuid PRIMARY KEY,
    hostname varchar(255) NOT NULL UNIQUE,
    os varchar(255),
    cpu_cores integer NOT NULL,
    ram_gb integer NOT NULL,
    disk_gb integer NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE TABLE IF NOT EXISTS server_ipv4_addresses (
    server_id uuid NOT NULL REFERENCES servers(id),
    ipv4_address varchar(255)
);
CREATE TABLE IF NOT EXISTS server_ipv6_addresses (
    server_id uuid NOT NULL REFERENCES servers(id),
    ipv6_address varchar(255)
);
CREATE TABLE IF NOT EXISTS server_requests (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES users(id),
    server_id uuid REFERENCES servers(id),
    cpu_cores integer NOT NULL,
    ram_gb integer NOT NULL,
    disk_gb integer NOT NULL,
    os varchar(255) NOT NULL,
    status varchar(255) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
ALTER TABLE server_requests ADD COLUMN IF NOT EXISTS type varchar(255) NOT NULL DEFAULT 'CREATE';
