# Supify — MySQL 8.0 ER Diagram & JDBC Data Flow

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

## 2. Java JDBC Connection Flow

```mermaid
flowchart LR
    CLI["Main (Admin -> [4] MySQL JDBC Status)"] --> DAO["JdbcSupplierDao (DriverManager + PreparedStatement + ResultSet)"]
    DAO -->|"Class.forName"| DRV["mysql-connector-j (*.jar in backend/lib)"]
    DRV -->|"JDBC over TCP 3306"| DB["MySQL 8.0 (supify)"]
    DB --> TBL["5 Tables: supplier, claim, evidence, verification_check, attestation"]
```

- **`JdbcSupplierDao`** (`backend/src/com/supify/dao/JdbcSupplierDao.java`) handles driver loading (`Class.forName`), `DriverManager.getConnection(...)`, `testConnection()`, and parameterized `PreparedStatement` + `ResultSet` CRUD against the `supplier` table (`save`, `findById`, `findAll`, `updateTrust`, `deleteById`, `count`).
- No boolean `is_verified` column exists; supplier trust is stored cleanly as `trust_band` + `trust_score` derived from the verification spine (`claim`, `evidence`, `verification_check`, `attestation`).
