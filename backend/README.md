# Supify — Java Backend & CLI

Simple pure-Java console application (**13 `.java` files across 4 packages** + `run.bat` & `test.bat`), organized cleanly for a 4-member project.

---

## 1. Core Java Concepts Covered

| Capability | Implementation | File(s) |
| :--- | :--- | :--- |
| **OOP, Encapsulation & Enums** | 4-layer entities (`Supplier`, `Claim`, `Evidence`, `final VerificationCheck`, `Attestation`) + enums (`TrustBand`, `VerificationMethod`) | `com.supify.model.*` |
| **Collections, Comparators & Lambdas** | `HashMap`, `ArrayList`, diamond `<>`, lambda `Comparator` search/sorting & Supplier CRUD | `SupifyPlatform.java` |
| **Custom Checked Exception** | `SupifyException extends Exception` for validation guards | `SupifyException.java`, `SupifyPlatform.java` |
| **Multithreading & Synchronization** | Daemon `ExpiryThread(platform)` (`sleep`, `interrupt`, `volatile`) sharing `synchronized SupifyPlatform` | `ExpiryThread.java`, `SupifyPlatform.java` |
| **File I/O (Character & Byte Streams)** | Buffered `FileWriter`/`FileReader` automatic CSV persistence (`data/suppliers.csv`), `FileInputStream`/`FileOutputStream` proof file copy (`data/proofs/`), & append-mode `audit.log` | `FileHelper.java`, `SupifyPlatform.java` |
| **Interactive CRUD & Automated Tests** | Add/Update/Delete/Switch Supplier + zero-dependency 7-check suite | `Main.java`, `TestApp.java` |

---

## 2. Directory & 4-Package Structure

```text
backend/
├── src/com/supify/
│   ├── model/                    # 7 files: 4-layer spine entities + enums (Member 1)
│   │   ├── TrustBand.java
│   │   ├── VerificationMethod.java
│   │   ├── Supplier.java
│   │   ├── Claim.java
│   │   ├── Evidence.java
│   │   ├── VerificationCheck.java
│   │   └── Attestation.java
│   ├── dao/                      # 1 file: CSV, proof copy & audit log File I/O (Member 2)
│   │   └── FileHelper.java
│   ├── service/                  # 3 files: exception, shared platform, daemon thread (Member 3)
│   │   ├── SupifyException.java
│   │   ├── SupifyPlatform.java
│   │   └── ExpiryThread.java
│   └── app/                      # 2 files: CLI menus + automated tests (Member 4)
│       ├── Main.java
│       └── TestApp.java
├── bin/                          # Compiled .class files (gitignored)
├── data/                         # Persistent suppliers.csv, proofs/, and audit.log (gitignored)
├── run.bat                       # Compile & run Main
├── test.bat                      # Compile & run TestApp
└── README.md
```

---

## 3. Compile & Run

### Windows Batch Scripts
Inside `backend\`:
- Run **`run.bat`** to compile and open the interactive CLI (`Buyer`, `Supplier`, `Verifier`, `Files & Monitor` menus).
- Run **`test.bat`** to compile and run the 7-check test suite (`TestApp`).

### Manual Commands
```bat
cd backend
if exist bin rd /s /q bin
mkdir bin
javac -d bin -sourcepath src src\com\supify\app\Main.java src\com\supify\app\TestApp.java
java -cp bin com.supify.app.Main
java -cp bin com.supify.app.TestApp
```

---

## 4. Interactive CLI Menus (`Main.java`)

- **`[1] Buyer Menu`:** `[1]` List All Suppliers · `[2]` Search & Sort Suppliers (Score, Capacity, or Name) · `[3]` Inspect Supplier & Claims (4-layer `Claim → Evidence → Check → Attestation`) · `[4]` Compare Two Suppliers
- **`[2] Supplier Menu`:** `[1]` Switch Active Supplier · `[2]` Add New Supplier · `[3]` Update Active Supplier · `[4]` Delete a Supplier · `[5]` Submit Claim + Proof Document (optional real local file path copied into `data/proofs/`) · `[6]` View Active Expiry Timers
- **`[3] Verifier Menu`:** `[1]` View Pending Claims · `[2]` Approve or Reject Claim (creates immutable `VerificationCheck`, issues `Attestation` on PASS, recalculates `TrustBand`, saves CSV)
- **`[4] Files & Monitor Menu`:** `[1]` Background Thread Status (`main` + `ExpiryThread` daemon) · `[2]` Save & Reload CSV Now (`data/suppliers.csv`) · `[3]` View Audit Log (`data/audit.log`) · `[4]` Reset to 5 Default Sample Suppliers
