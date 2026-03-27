drop table if exists saved_emails;

CREATE TABLE saved_emails(
    id SERIAL PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    saved_email VARCHAR NOT NULL
);