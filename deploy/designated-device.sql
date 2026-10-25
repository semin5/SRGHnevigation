CREATE TABLE IF NOT EXISTS designated_device (
    username varchar(80) PRIMARY KEY,
    token_hash varchar(64) NOT NULL,
    user_agent varchar(512),
    ip_address varchar(64),
    registered_at timestamp(6) with time zone NOT NULL
);
