# Supify — MySQL 8.0 Database

Simple 5-table relational database matching our 5 Java model classes (`Supplier`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation`) **1:1**.

---

## 1. Folder Structure

```text
db/
├── schema/
│   └── 01_supify_schema.sql       # Drops/recreates supify DB with 5 tables
├── seeds/
│   └── 01_canonical_suppliers.sql # Inserts 5 suppliers, 12 claims, 12 evidence rows, 10 checks, 10 attestations
├── migrations/                    # Reserved for future scripts
├── setup_mysql.bat                # One-click Windows initializer
└── README.md
```

---

## 2. Schema Overview (5 Basic Tables)

| # | Table | Java Class | Purpose |
| :-: | :--- | :--- | :--- |
| 1 | `supplier` | `Supplier.java` | Supplier profile details + `trust_band` and `trust_score` (no `is_verified` boolean) |
| 2 | `claim` | `Claim.java` | Layer 1 claims (`claim_type_key`, `pillar`, `status`) linked to `supplier` |
| 3 | `evidence` | `Evidence.java` | Layer 2 proof files (`title`, `kind`, `storage_ref`) linked to `claim` |
| 4 | `verification_check` | `VerificationCheck.java` | Layer 3 verifier PASS/FAIL check linked to `claim` |
| 5 | `attestation` | `Attestation.java` | Layer 4 time-bounded trust badge (`valid_until`, `state`) linked to `claim` & `verification_check` |

*(Audit logs and CSV exports stay flat-file in `backend/data/` via `FileHelper`.)*

---

## 3. Setup & Display Queries

### Initialize Database
Run `setup_mysql.bat` from `db\`, enter your MySQL username (default `root`) and password. It loads `schema/01_supify_schema.sql` and `seeds/01_canonical_suppliers.sql`.

### Display Data in MySQL (`USE supify;`)
```sql
USE supify;
SHOW TABLES;

SELECT * FROM supplier;
SELECT * FROM claim;
SELECT * FROM evidence;
SELECT * FROM verification_check;
SELECT * FROM attestation;

-- Join all 5 tables together:
SELECT s.name AS supplier, c.label AS claim, c.status,
       e.title AS evidence, vc.method, vc.outcome,
       a.state AS attestation_state, a.valid_until
FROM supplier s
JOIN claim c ON c.supplier_id = s.id
LEFT JOIN evidence e ON e.claim_id = c.id
LEFT JOIN verification_check vc ON vc.claim_id = c.id
LEFT JOIN attestation a ON a.claim_id = c.id;
```

See [`docs/database-architecture.md`](../docs/database-architecture.md) for the ER diagram and [`docs/setup.md`](../docs/setup.md) for setup troubleshooting.
