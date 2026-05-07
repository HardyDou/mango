CREATE TABLE another_flyway_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL
);

INSERT INTO another_flyway_user (id, username) VALUES (1, 'migrated');
