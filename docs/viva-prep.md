# Supify — Java Backend Viva Guide

Quick revision notes aligned to the **13-file pure-Java backend** (`backend/src/com/supify/` across **4 packages**: `model`, `dao`, `service`, `app` — one package per team member).

---

## 1. Quick Pitch & Live CLI Commands

- **Problem:** A buyer cannot cheaply tell whether a supplier truly holds claimed certifications or if they are still valid today.
- **4-Layer Spine (no `boolean isVerified` shortcut):** `Claim` (supplier assertion) → `Evidence` (uploaded document) → `VerificationCheck` (`final` immutable PASS/FAIL check) → `Attestation` (time-bounded active badge expiring automatically via `ExpiryThread`).
- **Simple Thread-Safe Architecture:** One shared synchronized `SupifyPlatform` holds Supplier CRUD, verification rules, search/sort, and CSV startup persistence (`data/suppliers.csv`), while `ExpiryThread(platform)` runs alongside `Main`.
- **Launch Tests:** Run `backend\test.bat` (`TestApp` runs 7 automated checks).
- **Launch CLI:** Run `backend\run.bat` (`Main` opens Buyer, Supplier, Verifier, and Files & Monitor menus with persistent Supplier Add/Update/Delete and a 30-second live expiry demo on `SUP-1`).

---

## 2. 4-Member Architecture by Package (4 Packages, 13 Files)

| Package | Classes | Java Concepts Covered |
| :--- | :--- | :--- |
| `com.supify.model` | `TrustBand`, `VerificationMethod`, `Supplier`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation` | OOP, Encapsulation, Enums with fields, `final` immutability, `LocalDateTime` & `Duration` |
| `com.supify.dao` | `FileHelper` | Character & Byte File I/O (`FileWriter`/`FileReader` CSV persistence, `FileInputStream`/`FileOutputStream` proof copy, & append-mode `audit.log`) |
| `com.supify.service` | `SupifyException`, `SupifyPlatform`, `ExpiryThread` | Custom checked exception (`extends Exception`), `HashMap`/`ArrayList`, `synchronized`, `Comparator` lambdas (`search`), Authority Split, Trust Capping, Daemon thread (`extends Thread`, `setDaemon(true)`, `volatile`, `sleep`, `interrupt`) |
| `com.supify.app` | `Main`, `TestApp` | Interactive CRUD menus (`Scanner`, classic `switch`/`case`), try-with-resources & `finally`, 7-test suite |

---

## 3. Core Rules & Common Viva Questions

1. **Why no `boolean isVerified` field?**  
   A boolean hides *how* a claim was verified, *who* checked it, *why*, and *until when*. We calculate `TrustBand` (`UNVERIFIED`, `BASIC`, `VERIFIED`, `AUDITED`) and `trustScore` strictly from active `Attestation` objects inside `SupifyPlatform.recalculateTrust()`.
2. **How do you keep Suppliers from verifying themselves?**  
   `SupifyPlatform.submitClaim(...)` only creates `Claim` (in `"SUBMITTED"` state) and `Evidence`. Only `SupifyPlatform.verifyClaim(...)` can create `VerificationCheck` and `Attestation`.
3. **Why is `VerificationCheck` marked `public final` with zero setters?**  
   It is an immutable audit record (`final` class + all `private final` fields assigned once in constructor).
4. **Why does `SupifyPlatform.recalculateTrust()` cap suppliers without `IDENTITY` or `LEGAL`?**  
   No active `IDENTITY` attestation caps the supplier at `UNVERIFIED (<= 0.30)`. Having `IDENTITY` without `LEGAL` caps at `BASIC (<= 0.65)`. Both plus `HUMAN_AUDIT`/`SITE_VISIT` and score `>= 0.85` earns `AUDITED`.
5. **Difference between `monitor.start()` and `monitor.run()`?**  
   `start()` creates a separate background thread (`ExpiryThread` with `setDaemon(true)`). Calling `run()` directly runs the infinite loop on `main` and freezes the menu.
6. **Difference between `volatile` and `synchronized`?**  
   `volatile boolean running` ensures immediate memory visibility for stopping the daemon loop. `synchronized` acquires the `SupifyPlatform` monitor lock so concurrent reads/writes and expiry updates never collide.
7. **How do search & sorting work in `SupifyPlatform.search()`?**  
   A plain `for` loop filters by keyword/city/category and minimum `TrustBand`, and `result.sort(getComparator(sortBy))` sorts using clean lambda `Comparator` functions.
8. **How does CSV persistence work across restarts?**  
   `SupifyPlatform.loadStartupData(csvFile)` loads saved rows from `backend/data/suppliers.csv` on startup using `FileHelper` (`FileReader`/`BufferedReader`), while Supplier Add/Update/Delete and Verifier approval immediately save changes back using `FileWriter`/`BufferedWriter`. `Files & Monitor -> [4]` resets cleanly to the 5 default suppliers.
9. **How does `FileHelper` write to `audit.log` without erasing older entries?**  
   `appendAuditLog` passes `true` (`new FileWriter(file, true)`) inside try-with-resources to append one timestamped line at a time.
