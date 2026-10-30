CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    surname VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
INSERT INTO users VALUES ('7da031fe-159d-4ea8-a9ec-49ed3c531e27', 'Admin', 'Legacy', 'ADMIN', 'placeholder', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

CREATE TABLE server_requests (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    server_id UUID,
    cpu_cores INTEGER NOT NULL,
    ram_gb INTEGER NOT NULL,
    disk_gb INTEGER NOT NULL,
    os VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
INSERT INTO server_requests VALUES ('6da031fe-159d-4ea8-a9ec-49ed3c531e27',
    '7da031fe-159d-4ea8-a9ec-49ed3c531e27', NULL, 2, 4, 20, 'Debian', 'CREATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
