# Supify — Java Backend Architecture

Architecture specification for the **13-file pure-Java backend** under `backend/src/com/supify/` (**4 packages**: `model`, `dao`, `service`, `app`) — organized cleanly for a **4-member team**.

---

## 1. 4-Package Layered Architecture (1 Package per Team Member)

```mermaid
flowchart TD
    APP["com.supify.app (Member 4)<br/>Main · TestApp"]
    SVC["com.supify.service (Member 3)<br/>SupifyPlatform · ExpiryThread · SupifyException"]
    DAO["com.supify.dao (Member 2)<br/>FileHelper (Character &amp; Byte Streams)"]
    MODEL["com.supify.model (Member 1)<br/>Supplier · Claim · Evidence · VerificationCheck<br/>Attestation · TrustBand · VerificationMethod"]
    FS[("Persistent File Storage (backend/data/)<br/>suppliers.csv · proofs/* · audit.log")]

    APP --> SVC
    APP --> DAO
    APP --> MODEL
    SVC --> DAO
    SVC --> MODEL
    DAO --> MODEL
    DAO --> FS
```

| Package | Owner | Classes (13 Total) | Responsibility |
| :--- | :---: | :--- | :--- |
| `com.supify.model` | Member 1 | `TrustBand`, `VerificationMethod`, `Supplier`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation` | 4-layer verification spine domain entities & weighted enums |
| `com.supify.dao` | Member 2 | `FileHelper` | Buffered CSV persistence (`suppliers.csv`), binary proof copy (`data/proofs/`), & append-only `audit.log` |
| `com.supify.service` | Member 3 | `SupifyException`, `SupifyPlatform`, `ExpiryThread` | Checked validation exception, shared `synchronized` platform (Supplier CRUD, CSV startup/reset, lambda `Comparator` search, trust rules), background daemon |
| `com.supify.app` | Member 4 | `Main`, `TestApp` | Interactive 4-role CLI menus (`Buyer`, `Supplier`, `Verifier`, `Files & Monitor`) and 7-check automated suite |

---

## 2. Complete UML Class Diagram (13 Classes)

![Supify Pure-Java UML Class Diagram](../albums/diagrams/supify_uml_class_diagram.png)

```mermaid
classDiagram
    direction TB

    class TrustBand {
        <<enumeration>>
        UNVERIFIED(0)
        BASIC(1)
        VERIFIED(2)
        AUDITED(3)
        -int level
        +getLevel() int
    }

    class VerificationMethod {
        <<enumeration>>
        SELF_DECLARED(0.25)
        DOCUMENT_REVIEW(0.60)
        THIRD_PARTY_API(0.85)
        HUMAN_AUDIT(0.95)
        SITE_VISIT(1.00)
        -double score
        +getScore() double
    }

    class Supplier {
        -final String id
        -String name
        -String city
        -String category
        -int capacityPerMonth
        -int moq
        -String leadTime
        -TrustBand trustBand
        -double trustScore
        +getId() String
        +getTrustBand() TrustBand
        +setTrustBand(TrustBand) void
        +setTrustScore(double) void
    }

    class Claim {
        -final String id
        -final String supplierId
        -final String claimTypeKey
        -final String label
        -final String pillar
        -final String value
        -String status
        +getStatus() String
        +setStatus(String) void
    }

    class Evidence {
        -final String id
        -final String claimId
        -final String title
        -final String docType
        -final String filePath
    }

    class VerificationCheck {
        <<final / immutable>>
        -final String id
        -final String claimId
        -final VerificationMethod method
        -final String outcome
        -final String verifierName
        -final LocalDateTime checkedAt
        -final String note
    }

    class Attestation {
        -final String id
        -final String claimId
        -final String checkId
        -final VerificationMethod method
        -final LocalDateTime verifiedAt
        -final LocalDateTime validUntil
        -String state
        +isExpired() boolean
        +expireIfNeeded() boolean
        +revoke() void
        +secondsLeft() long
    }

    class FileHelper {
        +exportToCsv(File, List~Supplier~)$ void
        +importFromCsv(File)$ List~Supplier~
        +copyProofFile(File sourceFile, File destDir)$ File
        +appendAuditLog(File, String)$ void
        +readAuditLog(File)$ List~String~
    }

    class SupifyPlatform {
        -Map~String,Supplier~ suppliers
        -Map~String,Claim~ claims
        -List~Evidence~ evidences
        -List~VerificationCheck~ checks
        -Map~String,Attestation~ attestations
        -List~String~ alerts
        +loadStartupData(File csvFile) String
        +resetToSampleData(File csvFile) void
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

    class ExpiryThread {
        -SupifyPlatform platform
        -int delayMs
        -volatile boolean running
        +run() void
        +stopMonitor() void
        +printThreadInfo() void
    }

    class SupifyException {
        +SupifyException(String message)
        +SupifyException(String message, Throwable cause)
    }

    class Main {
        -File CSV_FILE$
        -File LOG_FILE$
        +main(String[] args)$ void
        -buyerMenu(Scanner, SupifyPlatform)$ void
        -supplierMenu(Scanner, SupifyPlatform)$ void
        -verifierMenu(Scanner, SupifyPlatform)$ void
        -adminMenu(Scanner, SupifyPlatform, ExpiryThread)$ void
    }

    class TestApp {
        +main(String[] args)$ void
    }

    Supplier --> TrustBand
    VerificationCheck --> VerificationMethod
    Attestation --> VerificationMethod
    Supplier "1" o-- "0..*" Claim : submits
    Claim "1" o-- "0..*" Evidence : backed by
    Claim "1" o-- "0..*" VerificationCheck : audited by
    VerificationCheck "1" --> "0..1" Attestation : issues on PASS

    Thread <|-- ExpiryThread
    Exception <|-- SupifyException
    SupifyPlatform *-- Supplier
    SupifyPlatform *-- Claim
    SupifyPlatform *-- Evidence
    SupifyPlatform *-- VerificationCheck
    SupifyPlatform *-- Attestation
    SupifyPlatform ..> FileHelper : CSV startup & reset
    SupifyPlatform ..> SupifyException : throws
    ExpiryThread --> SupifyPlatform : polls every 4s
    Main ..> SupifyPlatform
    Main ..> ExpiryThread
    Main ..> FileHelper
    TestApp ..> SupifyPlatform
    TestApp ..> ExpiryThread
    TestApp ..> FileHelper
```

---

## 3. End-to-End Lifecycle Sequence (CRUD, Proof Copy, Attestation & Auto-Decay)

```mermaid
sequenceDiagram
    participant CLI as Main (Supplier / Verifier)
    participant FH as FileHelper (I/O Streams)
    participant SP as SupifyPlatform (synchronized)
    participant ET as ExpiryThread (daemon)

    CLI->>SP: loadStartupData(data/suppliers.csv)
    SP->>FH: importFromCsv() or exportToCsv() on first run

    Note over CLI,SP: Authority Split — Suppliers never create checks or attestations
    CLI->>FH: copyProofFile(sourcePdf, data/proofs/) [Byte Streams]
    CLI->>SP: submitClaim(SUP-1, label, pillar, val, storedPath)
    SP-->>CLI: Claim(SUBMITTED) + Evidence saved

    CLI->>SP: verifyClaim(claimId, Verifier-01, SITE_VISIT, true, note, 30s)
    SP->>SP: Save immutable final VerificationCheck(PASS)
    SP->>SP: Issue Attestation(ACTIVE, validUntil = now + 30s)
    SP->>SP: Claim.setStatus(VERIFIED) & recalculateTrust(SUP-1)
    CLI->>FH: exportToCsv(data/suppliers.csv) + appendAuditLog(data/audit.log)

    loop Every 4000ms while running
        ET->>SP: checkExpiredAttestations()
        alt validUntil passed on ACTIVE attestation
            SP->>SP: att.expireIfNeeded() -> EXPIRED
            SP->>SP: claim.setStatus(EXPIRED) & recalculateTrust(supplierId)
            SP->>SP: Queue expiry [ALERT]
        end
    end

    CLI->>SP: getAndClearAlerts()
    SP-->>CLI: Print [ALERT] banner before next menu prompt
```

---

## 4. Concurrency, File I/O Streams & Trust Capping Rules

### Concurrency & Synchronization
- **Shared Monitor (`SupifyPlatform`):** All mutators and snapshots (`save`, `findAll`, `addSupplier`, `updateSupplier`, `deleteSupplier`, `loadStartupData`, `resetToSampleData`, `submitClaim`, `verifyClaim`, `recalculateTrust`, `checkExpiredAttestations`, `getAndClearAlerts`) are `synchronized` on the shared `SupifyPlatform` object so `main` and `ExpiryThread` never collide.
- **Daemon Lifecycle (`ExpiryThread`):** Calls `super("ExpiryThread")` and `setDaemon(true)` so background polling never blocks JVM exit. Uses `volatile boolean running = true` for cross-thread visibility, and `stopMonitor()` calls `interrupt()` to wake `Thread.sleep(4000)` immediately.

### Dual Stream File I/O (`FileHelper`)

| Storage Target | Stream Type & Classes | Behavior |
| :--- | :--- | :--- |
| `data/suppliers.csv` | **Character Streams:** `BufferedWriter(new FileWriter(file))` & `BufferedReader(new FileReader(file))` | Auto-loaded on startup (`loadStartupData`) and auto-saved on Add/Update/Delete/Verify/Exit; skips malformed CSV rows safely via `catch (IllegalArgumentException)` |
| `data/proofs/*` | **Byte Streams:** `FileInputStream` & `FileOutputStream` (`byte[4096]` buffer) | Copies real binary proof files (PDFs/images) attached in Supplier Menu `[5]` without corrupting binary bytes |
| `data/audit.log` | **Character Stream (Append Mode):** `BufferedWriter(new FileWriter(file, true))` | Appends timestamped audit lines (`[LocalDateTime] ...`) without erasing prior log entries |

### Trust Calculation & Pillar Capping (`SupifyPlatform.recalculateTrust`)
Only **`ACTIVE`** attestations count toward `avgScore = min(1.0, sum(method.getScore()) / max(3, activeCount))`:

| Active Pillar / Audit Condition | Resulting `TrustBand` | Score Rule |
| :--- | :--- | :--- |
| Missing active `IDENTITY` attestation | `UNVERIFIED` (Lv 0) | Capped at `<= 0.30` |
| Active `IDENTITY`, missing active `LEGAL` | `BASIC` (Lv 1) | Capped at `<= 0.65` |
| Active `IDENTITY` + `LEGAL`, plus `HUMAN_AUDIT`/`SITE_VISIT` and `avgScore >= 0.85` | `AUDITED` (Lv 3) | Full `avgScore` |
| Active `IDENTITY` + `LEGAL` (otherwise) | `VERIFIED` (Lv 2) | Full `avgScore` |
