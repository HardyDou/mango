-- mango:baseline-idempotent
CREATE TABLE IF NOT EXISTS cold_alpha_record (
    id bigint NOT NULL,
    record_code varchar(64) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO cold_alpha_record (id, record_code)
SELECT 1, 'IT_COLD_ALPHA'
WHERE NOT EXISTS (SELECT 1 FROM cold_alpha_record WHERE id = 1);
