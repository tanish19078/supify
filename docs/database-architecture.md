# Supify — MySQL 8.0 ER Diagram & Schema Architecture

Relational database specification for [`db/`](../db/README.md) and [`backend/`](../backend/README.md).

---

## 1. Entity–Relationship Diagram (5 Basic Tables = 1:1 with Java Models)

Matches [`db/schema/01_supify_schema.sql`](../db/schema/01_supify_schema.sql). Each table corresponds directly to one class in `backend/src/com/supify/model/` (`Supplier`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation`).

```mermaid
erDiagram
    supplier ||--o{ claim : "submits"
    claim ||--o{ evidence : "backed by"
    claim ||--o{ verification_check : "checked by"
    claim ||--o{ attestation : "attested by"
    verification_check ||--o{ attestation : "issues"

    supplier {
        varchar id PK
        varchar name
        varchar city
        varchar category
        int capacity_per_month
        int moq
        varchar lead_time
        varchar trust_band
        double trust_score
        timestamp created_at
    }

    claim {
        varchar id PK
        varchar supplier_id FK
        varchar claim_type_key
        varchar label
        varchar pillar
        varchar asserted_value
        varchar status
        timestamp created_at
    }

    evidence {
        varchar id PK
        varchar claim_id FK
        varchar title
        varchar kind
        varchar storage_ref
        timestamp created_at
    }

    verification_check {
        varchar id PK
        varchar claim_id FK
        varchar method
        varchar outcome
        varchar performed_by
        text rationale
        timestamp performed_at
    }

    attestation {
        varchar id PK
        varchar claim_id FK
        varchar source_check_id FK
        varchar method
        timestamp verified_at
        timestamp valid_until
        varchar state
        varchar issued_by
    }
```

---

## 2. Schema Notes & Verification Integrity

```mermaid
flowchart LR
    SUP["supplier"] -->|"1:N"| CLM["claim"]
    CLM -->|"1:N"| EVD["evidence"]
    CLM -->|"1:N"| CHK["verification_check (immutable PASS/FAIL)"]
    CHK -->|"1:N"| ATT["attestation (time-bounded valid_until)"]
    CLM -->|"1:N"| ATT
```

- **1:1 alignment with `com.supify.model`:** The 5 tables (`supplier`, `claim`, `evidence`, `verification_check`, `attestation`) mirror the 5 Java entity classes in `backend/src/com/supify/model/` directly.
- **No boolean verification shortcut:** No `is_verified` column exists; supplier trust is stored cleanly as `trust_band` + `trust_score` derived from active rows in `attestation`.
- **CLI runtime vs. SQL scripts:** The Java CLI runs in memory via `SupifyPlatform` and persists CSV exports and `audit.log` in `backend/data/` via `FileHelper`; `db/` holds the standalone MySQL 8.0 DDL schema and canonical seed data for relational querying (`mysql -u root -p supify`).
