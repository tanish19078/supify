# Supify — Pure-Java Backend & Database Viva Preparation Guide

> **Complete 4-Member Revision Book | Plain-English Explanations, File-by-File Walkthrough & Spoken Viva Q&As**  
> Aligned 100% to the **13-file pure-Java backend** (`backend/src/com/supify/` across **4 packages**: `model`, `dao`, `service`, `app`) and **5-table MySQL 8.0 schema** (`db/`).

---

## 1. Quick Start: 30-Second Pitch & Live CLI Demo Path

### 30-Second Viva Pitch
> *"Supify is a B2B supplier discovery and verification platform written in pure Java. Any supplier can claim 'we hold ISO 9001', and a buyer cannot cheaply tell whether that is real or still valid today. Instead of a misleading `boolean isVerified` flag, we built a **4-layer verification spine**: a Supplier submits a **Claim** with **Evidence**; an independent Verifier writes an immutable **VerificationCheck**; and on PASS, the platform issues a time-bounded **Attestation** that expires automatically in the background via our daemon thread (`ExpiryThread`). All shared state lives inside one synchronized `SupifyPlatform` with persistent Supplier CRUD (`data/suppliers.csv`), binary proof copying (`data/proofs/`), and append-only audit logging (`data/audit.log`) via `FileHelper`."*

Two anchor principles examiners love:
- **Asymmetry Razor:** *"Every feature cuts either the cost of **finding** a supplier or the cost of **trusting** one."*
- **Authority Split:** *"The supplier asserts (`Claim` + `Evidence`); the platform attests (`VerificationCheck` + `Attestation`)."*

### 4-Member Architecture by Package (4 Packages, 13 `.java` Files)

| Member | Package | Files | Core Java Syllabus Topics |
| :---: | :--- | :--- | :--- |
| **Member 1** | `com.supify.model` (7) | `TrustBand`, `VerificationMethod`, `Supplier`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation` | OOP, Encapsulation, Rich Enums with fields/constructors, `final` class immutability, `synchronized` state methods, `LocalDateTime` & `Duration` |
| **Member 2** | `com.supify.dao` (1) | `FileHelper` | **Character Streams** (`BufferedWriter`/`FileWriter`, `BufferedReader`/`FileReader` for CSV & append-mode `audit.log`) + **Byte Streams** (`FileInputStream`/`FileOutputStream` with `byte[4096]` buffer for proof copy), `try-with-resources`, defensive CSV parsing |
| **Member 3** | `com.supify.service` (3) | `SupifyException`, `SupifyPlatform`, `ExpiryThread` | Checked exception (`extends Exception`), Collections (`HashMap`, `ArrayList`), Supplier CRUD + cascade delete, CSV restart persistence, `Comparator` lambdas (`search`), Pillar Capping, `synchronized` monitor, Daemon Thread (`extends Thread`, `setDaemon(true)`, `volatile`, `sleep`, `interrupt`) |
| **Member 4** | `com.supify.app` (2) | `Main`, `TestApp` | Interactive 4-role menus (`Scanner`, `switch`/`case`, flushed non-blocking prompts), `try-with-resources` & `finally` shutdown, 7-check automated verification suite |

### Live CLI Commands (`backend/`)
- **Launch Interactive CLI:** `backend\run.bat` (compiles into `bin/` and launches `com.supify.app.Main`)
- **Run 7-Check Suite:** `backend\test.bat` (runs `com.supify.app.TestApp` — **7 PASSED, 0 FAILED**)
- **Initialize MySQL 5-Table DB:** `db\setup_mysql.bat` (loads `01_supify_schema.sql` + 49 rows from `01_canonical_suppliers.sql`)

| Menu | Keys | What Happens Live |
| :--- | :--- | :--- |
| **`[1]` Buyer Menu** | `[1] → [1]..[4]` | Lists ranked suppliers, filters & sorts via lambda `Comparator` (`score`/`capacity`/`name`), inspects `SUP-1`'s 4-layer spine + live countdown, compares 2 suppliers |
| **`[2]` Supplier Menu** | `[2] → [1]..[6]` | Switches active supplier, **Adds / Updates / Deletes** suppliers (auto-saved to `data/suppliers.csv`), attaches real local proof files copied to `data/proofs/` via byte streams, views live `30s` expiry timers |
| **`[3]` Verifier Menu** | `[3] → [1]..[2]` | Lists pending `SUBMITTED` claims + proof, records `PASS`/`FAIL` `VerificationCheck`, issues `Attestation` on `PASS`, recalculates `TrustBand`, saves CSV & `audit.log` |
| **`[4]` Files & Monitor** | `[4] → [1]..[4]` | Shows `main` + `ExpiryThread` daemon status, saves & reloads `data/suppliers.csv`, reads `data/audit.log`, resets catalog to 5 default sample suppliers |

---

## 2. File-by-File Rapid Walkthrough (All 13 Java Files)

### Package 1: `com.supify.model`
1. **`TrustBand.java`** — Enum `UNVERIFIED(0)`, `BASIC(1)`, `VERIFIED(2)`, `AUDITED(3)` with `private int level` and `getLevel()`.
2. **`VerificationMethod.java`** — Enum `SELF_DECLARED(0.25)`, `DOCUMENT_REVIEW(0.60)`, `THIRD_PARTY_API(0.85)`, `HUMAN_AUDIT(0.95)`, `SITE_VISIT(1.00)` with `getScore()`.
3. **`Supplier.java`** — Vendor profile (`final id`, `name`, `city`, `category`, `capacityPerMonth`, `moq`, `leadTime`) + `synchronized setTrustBand` / `setTrustScore`. Starts `UNVERIFIED (0.0)`.
4. **`Claim.java`** *(Layer 1)* — Supplier assertion (`id`, `supplierId`, data-driven `claimTypeKey`, `label`, `pillar`, `value`, `synchronized status` starting at `"SUBMITTED"`). Zero hardcoded `switch (claimType)`.
5. **`Evidence.java`** *(Layer 2)* — Proof metadata (`id`, `claimId`, `title`, `docType`, `filePath`); all fields `private final`.
6. **`VerificationCheck.java`** *(Layer 3)* — **`public final class`** with all `private final` fields (`id`, `claimId`, `method`, `outcome`, `verifierName`, `checkedAt`, `note`) and zero setters → strictly immutable audit record.
7. **`Attestation.java`** *(Layer 4)* — Time-bounded badge minted **only on `PASS`** (`validUntil = verifiedAt.plusSeconds(validSeconds)`). Has `isExpired()`, `secondsLeft()` (`Duration.between`), and `synchronized expireIfNeeded()` / `revoke()`.

### Package 2: `com.supify.dao`
8. **`FileHelper.java`** — Dual-stream file persistence utility:
   - **Character Streams:** `exportToCsv` & `importFromCsv` (`BufferedWriter`/`FileWriter`, `BufferedReader`/`FileReader`) for `data/suppliers.csv` (skips corrupt rows via `catch (IllegalArgumentException)`), plus `appendAuditLog` (`new FileWriter(file, true)`) & `readAuditLog` for `data/audit.log`.
   - **Byte Streams:** `copyProofFile(sourceFile, destDir)` copies binary PDFs/images into `data/proofs/` using `FileInputStream` and `FileOutputStream` with a `byte[4096]` buffer inside `try-with-resources`.

### Package 3: `com.supify.service`
9. **`SupifyException.java`** — Custom **checked** exception (`extends Exception`) with 2 overloaded constructors (`message`, `message + cause`).
10. **`SupifyPlatform.java`** — Synchronized core engine holding `HashMap` (`suppliers`, `claims`, `attestations`) and `ArrayList` (`evidences`, `checks`, `alerts`):
    - **Supplier CRUD & Cascade Delete:** `addSupplier`, `updateSupplier`, and `deleteSupplier` (removes supplier + cascades removal of its claims, evidences, checks, and attestations).
    - **CSV Restart Persistence:** `loadStartupData(csvFile)` reconciles saved `data/suppliers.csv` rows with seeded claims on boot; `resetToSampleData(csvFile)` restores the 5 canonical suppliers.
    - **Search & Sort (`search`):** Filters with a clean `for` loop and sorts via `result.sort(getComparator(sortBy))` using lambda `Comparator<Supplier>` expressions (`score`, `capacity`, `name`).
    - **Pillar Capping (`recalculateTrust`):** `avgScore = min(1.0, total / max(3, activeCount))`. Missing `IDENTITY` → `UNVERIFIED (<= 0.30)`; missing `LEGAL` → `BASIC (<= 0.65)`; `IDENTITY + LEGAL + Audit/Site + avg >= 0.85` → `AUDITED`; else `VERIFIED`.
11. **`ExpiryThread.java`** — Background daemon thread (`extends Thread`, `super("ExpiryThread")`, `setDaemon(true)`) polling `platform.checkExpiredAttestations()` every `4000ms` with `volatile boolean running = true` and `stopMonitor()` (`running = false; interrupt();`).

### Package 4: `com.supify.app`
12. **`Main.java`** — Interactive console driving `Buyer`, `Supplier`, `Verifier`, and `Files & Monitor` menus inside `try (Scanner sc = ...)` with `finally { monitor.stopMonitor(); }`, auto-saving `suppliers.csv`, logging actions to `audit.log`, and flushing prompts via `System.out.flush()`.
13. **`TestApp.java`** — Zero-dependency 7-check automated suite verifying CRUD & CSV restart persistence, Comparator search/sorting, 4-layer spine, pillar capping & rejection, `ExpiryThread` auto-decay, `FileHelper` CSV/proof-copy/audit I/O, and checked `SupifyException` guards.

---

## 3. High-Frequency Viva Questions & Spoken Answers

### OOP, Immutability & Enums
- **Q1. Why is there no `boolean isVerified` field on `Supplier` or `Claim`?**  
  *Say:* *"A boolean hides **how** a claim was verified, **who** checked it, **why**, and **until when** it remains valid. Supify derives `TrustBand` and `trustScore` strictly from non-expired `Attestation` records backed by immutable `VerificationCheck`s."*
- **Q2. How did you make `VerificationCheck` strictly immutable, and why isn't `Attestation` immutable?**  
  *Say:* *"`VerificationCheck` follows all 4 immutability rules: `public final class`, all `private final` fields, zero setters, and immutable field types (`String`, enum, `LocalDateTime`) because historical audit verdicts must never be altered. `Attestation` represents live standing whose `state` must legitimately transition from `ACTIVE` to `EXPIRED` or `REVOKED` via `synchronized` methods."*
- **Q3. How do you enforce Authority Split in code?**  
  *Say:* *"Supplier Menu `[5]` calls `submitClaim()`, which only creates a `SUBMITTED` `Claim` and `Evidence`. Only Verifier Menu `[2]` calls `verifyClaim()`, which creates `VerificationCheck`, issues `Attestation` on `PASS`, and triggers `recalculateTrust()`."*

### Collections, Search & Lambdas
- **Q4. Why use `HashMap` for `suppliers`, `claims`, and `attestations`, but `ArrayList` for `evidences` and `checks`?**  
  *Say:* *"`suppliers`, `claims`, and `attestations` are looked up or removed by ID (`O(1)` average time in `HashMap`), whereas `evidences` and `checks` are ordered, append-only histories per claim (`ArrayList`)."*
- **Q5. How does `SupifyPlatform.search()` sort suppliers by score, capacity, or name?**  
  *Say:* *"`getComparator(sortBy)` returns a **lambda expression** implementing the `@FunctionalInterface Comparator<Supplier>`, such as `(a, b) -> Double.compare(b.getTrustScore(), a.getTrustScore())`, passed directly into `result.sort(...)`."*
- **Q6. When deleting a supplier in `deleteSupplier(id)` (`SupifyPlatform.java`), why loop backward (`for (int i = evidences.size() - 1; i >= 0; i--)`) when removing from `ArrayList`?**  
  *Say:* *"Removing elements forward while indexing shifts remaining indices left and skips elements (or throws `ConcurrentModificationException` in a `for-each` loop). Looping backward from `size() - 1` down to `0` removes matching items safely in place."*

### Character Streams vs. Byte Streams (`FileHelper`)
- **Q7. Why does `FileHelper` use `FileReader`/`FileWriter` for CSV & `audit.log`, but `FileInputStream`/`FileOutputStream` in `copyProofFile()`?**  
  *Say:* *"CSV catalogs and audit logs are human-readable text, so **16-bit Unicode Character Streams** (`BufferedReader`/`BufferedWriter`) give us line-by-line `readLine()` and `newLine()`. Proof attachments like PDFs and images are raw binary files—reading them as characters would corrupt bytes, so `copyProofFile` uses **8-bit Byte Streams** (`FileInputStream`/`FileOutputStream`) with a `4096`-byte buffer."*
- **Q8. Why pass `true` to `new FileWriter(file, true)` in `appendAuditLog()`, and how are streams closed safely?**  
  *Say:* *"Passing `true` enables **append mode** so older audit entries are preserved instead of truncated. Every stream is opened inside **`try-with-resources`**, which auto-closes `AutoCloseable` streams even if an `IOException` occurs."*
- **Q9. What happens if `data/suppliers.csv` has a corrupted line?**  
  *Say:* *"`importFromCsv()` checks `parts.length < 9` and catches `IllegalArgumentException` (covering both `NumberFormatException` from `Integer.parseInt`/`Double.parseDouble` and bad `TrustBand.valueOf` names) per row, skipping the bad line without crashing startup."*

### Multithreading, Synchronization & Checked Exceptions
- **Q10. Difference between `monitor.start()` and `monitor.run()` on `ExpiryThread`?**  
  *Say:* *"`start()` allocates a new background thread stack and runs `ExpiryThread.run()` concurrently alongside `main`. Calling `run()` directly executes the `while (running)` loop on `main` and freezes the console menu."*
- **Q11. Why is `ExpiryThread` a daemon thread (`setDaemon(true)`), and why use BOTH `volatile` and `synchronized`?**  
  *Say:* *"`setDaemon(true)` ensures the JVM exits cleanly as soon as `main` finishes. `volatile boolean running` guarantees immediate cross-thread visibility for the stop flag, while `synchronized` on `SupifyPlatform` provides **mutual exclusion and atomicity** across multi-step operations like expiring an attestation, updating claim status, recalculating `TrustBand`, and queueing `[ALERT]`s."*
- **Q12. Why does `SupifyException` extend `Exception` instead of `RuntimeException`?**  
  *Say:* *"Extending `Exception` makes it a **checked exception**, forcing `Main.java` at compile time to `try-catch` recoverable user-input mistakes (blank supplier name, invalid capacity `<= 0`, missing claim ID, blank verifier note) and print a clean `[ERROR]` line."*

### MySQL 8.0 5-Table Database (`db/`)
- **Q13. How do the 5 MySQL tables map to Java, and why does `attestation.source_check_id` use `ON DELETE RESTRICT` while `claim_id` uses `ON DELETE CASCADE`?**  
  *Say:* *"The 5 tables (`supplier`, `claim`, `evidence`, `verification_check`, `attestation`) match our 5 domain classes 1:1 with zero `is_verified` boolean columns. Deleting a supplier or claim cascades cleanly to child rows (`ON DELETE CASCADE`), whereas `ON DELETE RESTRICT` on `source_check_id` blocks deleting a `verification_check` while an `attestation` still relies on it as proof."*
