ALTER TABLE users ADD COLUMN active boolean NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN must_change_password boolean NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN auth_version bigint NOT NULL DEFAULT 0;

CREATE TABLE user_roles (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role varchar(16) NOT NULL CHECK (role IN ('VIEWER','EDITOR','ADMIN')),
    PRIMARY KEY (user_id, role)
);
INSERT INTO user_roles
SELECT id, CASE WHEN upper(email) = 'ADMIN' THEN 'ADMIN' ELSE 'EDITOR' END FROM users;

CREATE TABLE refresh_sessions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    auth_version bigint NOT NULL
);
CREATE INDEX refresh_sessions_user_idx ON refresh_sessions(user_id);

CREATE TABLE os_references (
    id uuid PRIMARY KEY,
    code varchar(64) NOT NULL UNIQUE,
    name varchar(255) NOT NULL,
    enabled boolean NOT NULL DEFAULT true
);
INSERT INTO os_references VALUES
    ('00000000-0000-0000-0000-000000000001', 'OTHER', 'Other', true),
    ('00000000-0000-0000-0000-000000000002', 'DEBIAN', 'Debian', true),
    ('00000000-0000-0000-0000-000000000003', 'UBUNTU', 'Ubuntu', true),
    ('00000000-0000-0000-0000-000000000004', 'WINDOWS', 'Windows', true);
INSERT INTO os_references (id, code, name)
SELECT md5('host-app-os:' || lower(os))::uuid, 'LEGACY-' || upper(md5(lower(os))), min(os)
FROM server_requests r WHERE NOT EXISTS (SELECT 1 FROM os_references ref WHERE lower(ref.name)=lower(r.os))
GROUP BY lower(os);

ALTER TABLE server_requests ADD COLUMN external_id varchar(80);
ALTER TABLE server_requests ADD COLUMN title varchar(160) NOT NULL DEFAULT 'Заявка на сервер';
ALTER TABLE server_requests ADD COLUMN description varchar(2000) NOT NULL DEFAULT '';
ALTER TABLE server_requests ADD COLUMN reference_id uuid REFERENCES os_references(id);
ALTER TABLE server_requests ADD COLUMN urgent boolean NOT NULL DEFAULT false;
ALTER TABLE server_requests ADD COLUMN archived boolean NOT NULL DEFAULT false;
ALTER TABLE server_requests ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE server_requests ADD COLUMN priority varchar(16) NOT NULL DEFAULT 'STANDARD'
    CHECK (priority IN ('STANDARD','HIGH'));
UPDATE server_requests SET priority = 'HIGH' WHERE urgent OR cpu_cores >= 16 OR ram_gb >= 64 OR disk_gb >= 500;
CREATE INDEX server_requests_priority_idx ON server_requests(priority);
UPDATE server_requests SET external_id = 'REQ-' || upper(id::text);
UPDATE server_requests r SET reference_id = ref.id FROM os_references ref WHERE lower(r.os)=lower(ref.name);
ALTER TABLE server_requests ALTER COLUMN external_id SET NOT NULL;
ALTER TABLE server_requests ADD CONSTRAINT server_requests_external_id_key UNIQUE (external_id);
ALTER TABLE server_requests ALTER COLUMN reference_id SET NOT NULL;
CREATE INDEX server_requests_owner_archived_idx ON server_requests(owner_id, archived, created_at);
CREATE INDEX server_requests_reference_idx ON server_requests(reference_id);
CREATE INDEX server_requests_status_idx ON server_requests(status);
CREATE INDEX server_requests_cpu_idx ON server_requests(cpu_cores);
CREATE INDEX server_requests_created_idx ON server_requests(created_at);
CREATE INDEX server_requests_server_idx ON server_requests(server_id);
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX server_requests_title_text_idx ON server_requests USING gin (lower(title) gin_trgm_ops);
CREATE INDEX server_requests_description_text_idx ON server_requests USING gin (lower(description) gin_trgm_ops);
CREATE INDEX server_requests_os_text_idx ON server_requests USING gin (lower(os) gin_trgm_ops);
CREATE INDEX server_requests_external_text_idx ON server_requests USING gin (lower(external_id) gin_trgm_ops);

CREATE TABLE request_tags (
    request_id uuid NOT NULL REFERENCES server_requests(id) ON DELETE CASCADE,
    tag varchar(48) NOT NULL,
    PRIMARY KEY (request_id, tag)
);
CREATE INDEX request_tags_tag_idx ON request_tags(tag);

CREATE TABLE request_notes (
    id uuid PRIMARY KEY,
    request_id uuid NOT NULL REFERENCES server_requests(id) ON DELETE CASCADE,
    author_id uuid NOT NULL REFERENCES users(id),
    body varchar(2000) NOT NULL,
    pinned boolean NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE INDEX request_notes_request_idx ON request_notes(request_id, pinned, created_at);

CREATE TABLE audit_events (
    id uuid PRIMARY KEY,
    actor_id uuid,
    actor_login varchar(255),
    event_type varchar(64) NOT NULL,
    entity_type varchar(64) NOT NULL,
    entity_id uuid,
    happened_at timestamptz NOT NULL,
    correlation_id varchar(64),
    description varchar(500) NOT NULL
);
CREATE INDEX audit_events_event_time_idx ON audit_events(event_type, happened_at);
CREATE INDEX audit_events_time_idx ON audit_events(happened_at);
CREATE INDEX audit_events_description_text_idx ON audit_events USING gin (lower(description) gin_trgm_ops);
INSERT INTO audit_events (id, actor_id, actor_login, event_type, entity_type, entity_id, happened_at, description)
SELECT md5('host-app-create:' || r.id::text)::uuid, r.owner_id, u.email,
    'SERVER_CREATE_REQUEST', 'REQUEST', r.id, r.created_at, 'Создание заявки (перенос из истории)'
FROM server_requests r JOIN users u ON u.id=r.owner_id WHERE r.type='CREATE';

CREATE TABLE forecast_demo_days (
    day date PRIMARY KEY,
    request_count integer NOT NULL CHECK (request_count >= 0)
);

CREATE TABLE csv_import_previews (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_hash varchar(64) NOT NULL,
    plan_hash varchar(64) NOT NULL,
    mode varchar(16) NOT NULL CHECK (mode IN ('CREATE_ONLY','UPSERT')),
    expires_at timestamptz NOT NULL
);
CREATE INDEX csv_import_previews_expiry_idx ON csv_import_previews(expires_at);
