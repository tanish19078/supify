# Supify — Java Backend & CLI

Clean pure-Java console application (**14 `.java` files across 4 packages** + `run.bat` & `test.bat`).

---

## 1. Course Concepts Covered (25CSE0202)

| Concept | Implementation | File(s) |
| :--- | :--- | :--- |
| **OOP, Encapsulation & Enums** | 4-layer entities (`Supplier`, `Claim`, `Evidence`, `final VerificationCheck`, `Attestation`) + enums (`TrustBand`, `VerificationMethod`) | `com.supify.model.*` |
| **Generics & Collections** | Generic `Repository<T, ID>`, `HashMap`, `ArrayList`, `<>` diamond | `Repository.java`, `SupifyPlatform.java` |
| **Lambda Expressions & Streams** | `.stream().filter(...).sorted(...).collect(Collectors.toList())` search & sorting | `SupifyPlatform.java` |
| **Custom Checked Exception** | `SupifyException extends Exception` for validation guards | `SupifyException.java`, `SupifyPlatform.java` |
| **Multithreading & Synchronization** | Daemon `DecayMonitorThread(platform)` (`sleep`, `interrupt`, `volatile`) sharing `synchronized SupifyPlatform` | `DecayMonitorThread.java`, `SupifyPlatform.java` |
| **File I/O (Character Streams)** | Buffered `FileWriter`/`FileReader` CSV catalog export/import & append-mode `audit.log` | `FileHelper.java` |
| **Automated Verification** | Zero-dependency 7-check suite | `TestApp.java` |

---

## 2. Directory & 4-Package Structure

```text
backend/
├── src/com/supify/
│   ├── model/                    # 7 files: 4-layer spine entities + enums
│   │   ├── TrustBand.java
│   │   ├── VerificationMethod.java
│   │   ├── Supplier.java
│   │   ├── Claim.java
│   │   ├── Evidence.java
│   │   ├── VerificationCheck.java
│   │   └── Attestation.java
│   ├── dao/                      # 2 files: generic repo & File I/O
│   │   ├── Repository.java
│   │   └── FileHelper.java
│   ├── service/                  # 3 files: exception, shared platform, daemon thread
│   │   ├── SupifyException.java
│   │   ├── SupifyPlatform.java
│   │   └── DecayMonitorThread.java
│   └── app/                      # 2 files: CLI entry + automated tests
│       ├── Main.java
│       └── TestApp.java
├── bin/                          # Compiled .class files (gitignored)
├── run.bat                       # Compile & run Main
├── test.bat                      # Compile & run TestApp
└── README.md
```

---

## 3. Compile & Run

### Windows Batch Scripts
Inside `backend\`:
- Run **`run.bat`** to compile and open the interactive CLI (`Buyer`, `Supplier`, `Verifier`, `Admin` menus).
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
