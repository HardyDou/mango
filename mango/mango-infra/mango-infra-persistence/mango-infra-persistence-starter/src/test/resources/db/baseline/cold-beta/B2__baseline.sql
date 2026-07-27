-- mango:baseline-idempotent
CREATE TABLE IF NOT EXISTS cold_beta_record (
    id bigint NOT NULL,
    record_code varchar(64) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO cold_beta_record (id, record_code)
SELECT 2, 'IT_COLD_BETA'
WHERE NOT EXISTS (SELECT 1 FROM cold_beta_record WHERE id = 2);
