# Supify — Java Backend & Database Viva Guide

Quick revision notes aligned to the **14-file pure-Java backend** (`backend/src/com/supify/` across **4 packages**: `model`, `dao`, `service`, `app`) and **5-table MySQL schema** (`db/`).

---

## 1. Quick Pitch & Live CLI Commands

- **Problem:** A buyer cannot cheaply tell whether a supplier truly holds claimed certifications or if they are still valid today.
- **4-Layer Spine (no `boolean isVerified` shortcut):** `Claim` (supplier assertion) → `Evidence` (uploaded document) → `VerificationCheck` (`final` immutable PASS/FAIL check) → `Attestation` (time-bounded active badge expiring automatically via `DecayMonitorThread`).
- **Simple Thread-Safe Architecture:** Just like our classroom `Account` / `OrderQueue` pattern, one shared `SupifyPlatform` holds all state and business rules cleanly, and `DecayMonitorThread(platform)` runs alongside `Main`.
- **Launch Tests:** Run `backend\test.bat` (`TestApp` runs 7 automated checks).
- **Launch CLI:** Run `backend\run.bat` (`Main` opens Buyer, Supplier, Verifier, and Admin menus with a 30-second live expiry demo on `SUP-1`).

---

## 2. Architecture by Package (4 Packages, 14 Files)

| Package | Classes | Syllabus Concepts |
| :--- | :--- | :--- |
| `com.supify.model` | `TrustBand`, `VerificationMethod`, `Supplier`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation` | OOP, Encapsulation, Enums with fields, `final` immutability, `LocalDateTime` & `Duration` |
| `com.supify.dao` | `Repository<T, ID>`, `FileHelper` | Generics (`<T, ID>`), Character File I/O (`FileWriter`/`FileReader` + buffered CSV & `audit.log`) |
| `com.supify.service` | `SupifyException`, `SupifyPlatform`, `DecayMonitorThread` | Custom checked exception (`extends Exception`), `HashMap`/`ArrayList`, `synchronized`, Java Stream API & lambdas (`search`), Authority Split, Trust Capping, Daemon thread (`extends Thread`, `setDaemon(true)`, `volatile`, `sleep`, `interrupt`) |
| `com.supify.app` | `Main`, `TestApp` | Interactive menus (`Scanner`, classic `switch`/`case`), try-with-resources & `finally`, 7-test verification |

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
   `start()` creates a separate background thread (`DecayMonitor` with `setDaemon(true)`). Calling `run()` directly runs the infinite loop on `main` and freezes the menu.
6. **Difference between `volatile` and `synchronized`?**  
   `volatile boolean running` ensures immediate memory visibility for stopping the daemon loop. `synchronized` acquires the `SupifyPlatform` monitor lock so concurrent reads/writes and expiry updates never collide.
7. **Where are Java Streams and Lambdas used?**  
   Inside `SupifyPlatform.search()` using `.stream().filter(...).sorted(getComparator(sortBy)).collect(Collectors.toList())`.
8. **How does `FileHelper` handle CSV & audit log I/O safely?**  
   Uses `BufferedWriter(new FileWriter(...))` and `BufferedReader(new FileReader(...))` inside try-with-resources; `appendAuditLog` passes `true` (`new FileWriter(file, true)`) to append lines without overwriting older logs.
9. **How is the MySQL database organized?**  
   5 simple tables (`supplier`, `claim`, `evidence`, `verification_check`, `attestation`) in `db/schema/01_supify_schema.sql` seeded by `db/seeds/01_canonical_suppliers.sql`, matching the 5 Java model classes 1:1.
