CREATE TABLE package_alpha_record (
  id bigint NOT NULL,
  record_code varchar(64) NOT NULL,
  PRIMARY KEY (id)
);
INSERT INTO package_alpha_record (id, record_code) VALUES (1, 'ALPHA-1');
