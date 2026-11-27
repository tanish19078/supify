# Supify — Java Backend Architecture

Architecture summary for the 13-file pure-Java backend under `backend/src/com/supify/` (**4 packages**: `model`, `dao`, `service`, `app`) — organized simply for a 4-member project.

---

## 1. 4-Package Layered Architecture

```mermaid
flowchart TD
    APP["app (Main, TestApp)"] --> SVC["service (SupifyPlatform, ExpiryThread, SupifyException)"]
    APP --> DAO["dao (FileHelper)"]
    APP --> MODEL["model (7 Spine Entities & Enums)"]

    SVC --> DAO
    SVC --> MODEL
    DAO --> MODEL
    DAO --> FS[("Local File I/O<br/>data/suppliers.csv, data/proofs/, data/audit.log")]
```

| Package | Classes | Responsibility |
| :--- | :--- | :--- |
| `com.supify.model` | `TrustBand`, `VerificationMethod`, `Supplier`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation` | 4-layer verification spine & enums |
| `com.supify.dao` | `FileHelper` | CSV catalog, byte-stream proof copy, & plain-text `audit.log` File I/O |
| `com.supify.service` | `SupifyException`, `SupifyPlatform`, `ExpiryThread` | Custom checked exception, shared synchronized platform (CRUD, CSV persistence, Comparator search, trust rules), daemon thread |
| `com.supify.app` | `Main`, `TestApp` | Interactive console menus (with Add/Update/Delete Supplier) and 7 automated checks |

---

## 2. Core Class Relationships

```mermaid
classDiagram
    class SupifyPlatform {
        +loadStartupData(File) String
        +resetToSampleData(File) void
        +addSupplier(...) Supplier
        +updateSupplier(...) Supplier
        +deleteSupplier(String id) boolean
        +search(String keyword, TrustBand minBand, String sortBy) List~Supplier~
        +submitClaim(...) Claim
        +verifyClaim(...) VerificationCheck
        +recalculateTrust(String supplierId) void
        +checkExpiredAttestations() void
        +getAndClearAlerts() List~String~
    }

    class FileHelper {
        +exportToCsv(File, List)$ void
        +importFromCsv(File)$ List~Supplier~
        +copyProofFile(File, File)$ File
        +appendAuditLog(File, String)$ void
        +readAuditLog(File)$ List~String~
    }

    class ExpiryThread {
        +run() void
        +stopMonitor() void
        +printThreadInfo() void
    }

    class SupifyException {
        +SupifyException(String message)
        +SupifyException(String message, Throwable cause)
    }

    Thread <|-- ExpiryThread
    Exception <|-- SupifyException
    SupifyPlatform ..> FileHelper
    SupifyPlatform ..> SupifyException
    ExpiryThread --> SupifyPlatform
```

---

## 3. Trust Calculation & Authority Split (`SupifyPlatform`)

1. **Supplier write path (`submitClaim`):** Creates `Claim` (`"SUBMITTED"`) and optional `Evidence`. Never creates checks or attestations.
2. **Verifier write path (`verifyClaim`):** Creates immutable `final VerificationCheck` (`"PASS"`/`"FAIL"`), issues time-bounded `Attestation` only on `"PASS"`, updates `Claim.status`, and calls `recalculateTrust(...)` all inside `synchronized` locks.
3. **TrustBand rules:** Only `ACTIVE` attestations count. Missing `IDENTITY` caps at `UNVERIFIED` (`<= 0.30`); missing `LEGAL` caps at `BASIC` (`<= 0.65`); `HUMAN_AUDIT`/`SITE_VISIT` + avg `>= 0.85` earns `AUDITED`; otherwise `VERIFIED`.
