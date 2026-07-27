CREATE TABLE IF NOT EXISTS mango_bootstrap_control (
    environment_key VARCHAR(128) NOT NULL,
    stable_generation BIGINT NOT NULL DEFAULT 0,
    stable_fingerprint VARCHAR(64) NULL,
    candidate_generation BIGINT NULL,
    candidate_fingerprint VARCHAR(64) NULL,
    authoritative_generation BIGINT NOT NULL DEFAULT 0,
    state VARCHAR(32) NOT NULL,
    fencing_token BIGINT NOT NULL DEFAULT 0,
    release_id VARCHAR(128) NULL,
    build_revision VARCHAR(128) NULL,
    started_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (environment_key)
);

CREATE TABLE IF NOT EXISTS mango_bootstrap_execution (
    execution_id VARCHAR(36) NOT NULL,
    environment_key VARCHAR(128) NOT NULL,
    release_id VARCHAR(128) NOT NULL,
    build_revision VARCHAR(128) NOT NULL,
    generation BIGINT NOT NULL,
    manifest_fingerprint VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    phase VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL,
    fencing_token BIGINT NOT NULL,
    error_type VARCHAR(255) NULL,
    error_summary VARCHAR(1000) NULL,
    started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (execution_id),
    KEY idx_bootstrap_execution_generation (environment_key, generation, started_at)
);

CREATE TABLE IF NOT EXISTS mango_bootstrap_step_execution (
    id BIGINT NOT NULL AUTO_INCREMENT,
    execution_id VARCHAR(36) NOT NULL,
    environment_key VARCHAR(128) NOT NULL,
    generation BIGINT NOT NULL,
    phase VARCHAR(32) NOT NULL,
    step_code VARCHAR(128) NOT NULL,
    step_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    result_summary VARCHAR(1000) NULL,
    error_type VARCHAR(255) NULL,
    error_summary VARCHAR(1000) NULL,
    started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bootstrap_step_attempt (execution_id, step_code),
    KEY idx_bootstrap_step_reuse (environment_key, generation, phase, step_code, step_fingerprint, status)
);

CREATE TABLE IF NOT EXISTS mango_runtime_instance (
    instance_id VARCHAR(128) NOT NULL,
    environment_key VARCHAR(128) NOT NULL,
    release_id VARCHAR(128) NOT NULL,
    generation BIGINT NOT NULL,
    manifest_fingerprint VARCHAR(64) NOT NULL,
    draining TINYINT(1) NOT NULL DEFAULT 0,
    started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_heartbeat_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    lease_expires_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (instance_id),
    KEY idx_runtime_generation_lease (environment_key, generation, lease_expires_at)
);
