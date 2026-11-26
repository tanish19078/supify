-- Supify MySQL 8.0 Schema (5 Simple Tables Matching Java Models 1:1)

DROP DATABASE IF EXISTS supify;
CREATE DATABASE supify;
USE supify;

-- 1. Supplier profile and derived trust standing
CREATE TABLE supplier (
    id                 VARCHAR(36)  PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    city               VARCHAR(100) NOT NULL,
    category           VARCHAR(150) NOT NULL,
    capacity_per_month INT          NOT NULL DEFAULT 0,
    moq                INT          NOT NULL DEFAULT 1,
    lead_time          VARCHAR(80)  NOT NULL DEFAULT '14 days',
    trust_band         VARCHAR(32)  NOT NULL DEFAULT 'UNVERIFIED',
    trust_score        DOUBLE       NOT NULL DEFAULT 0.0,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Layer 1: Claims submitted by a supplier
CREATE TABLE claim (
    id             VARCHAR(36)  PRIMARY KEY,
    supplier_id    VARCHAR(36)  NOT NULL,
    claim_type_key VARCHAR(100) NOT NULL,
    label          VARCHAR(255) NOT NULL,
    pillar         VARCHAR(40)  NOT NULL,
    asserted_value VARCHAR(500),
    status         VARCHAR(32)  NOT NULL DEFAULT 'SUBMITTED',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES supplier (id) ON DELETE CASCADE
);

-- 3. Layer 2: Proof documents attached to a claim
CREATE TABLE evidence (
    id          VARCHAR(36)  PRIMARY KEY,
    claim_id    VARCHAR(36)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    kind        VARCHAR(40)  NOT NULL,
    storage_ref VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (claim_id) REFERENCES claim (id) ON DELETE CASCADE
);

-- 4. Layer 3: Verifier check record (PASS or FAIL)
CREATE TABLE verification_check (
    id           VARCHAR(36)  PRIMARY KEY,
    claim_id     VARCHAR(36)  NOT NULL,
    method       VARCHAR(40)  NOT NULL,
    outcome      VARCHAR(40)  NOT NULL,
    performed_by VARCHAR(150) NOT NULL,
    rationale    TEXT         NOT NULL,
    performed_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (claim_id) REFERENCES claim (id) ON DELETE CASCADE
);

-- 5. Layer 4: Time-bounded active trust badge
CREATE TABLE attestation (
    id              VARCHAR(36)  PRIMARY KEY,
    claim_id        VARCHAR(36)  NOT NULL,
    source_check_id VARCHAR(36)  NOT NULL,
    method          VARCHAR(40)  NOT NULL,
    verified_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until     TIMESTAMP    NULL,
    state           VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    issued_by       VARCHAR(150) NOT NULL,
    FOREIGN KEY (claim_id) REFERENCES claim (id) ON DELETE CASCADE,
    FOREIGN KEY (source_check_id) REFERENCES verification_check (id) ON DELETE RESTRICT
);
