CREATE TABLE forecast_demo_days (
    day date PRIMARY KEY,
    request_count integer NOT NULL CHECK (request_count >= 0)
);
