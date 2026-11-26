# Supify — Setup, Run & Demo Guide

Complete local setup instructions for all three parts of the repository: the React Frontend (`src/`), the Pure-Java Backend CLI (`backend/`), and the MySQL 8.0 Database & JDBC Layer (`db/`).

---

## 1. Repository Layout

```text
supplier-platform/
├── AGENTS.md                   # Single canonical working rules, invariants & status
├── README.md                   # Project overview & quick tour
├── src/                        # React frontend (Vite, React Router 6, Tailwind, Zustand)
├── public/data/                # JSON fixtures served over HTTP to the frontend
├── backend/
│   ├── src/com/supify/         # Pure-Java CLI across 4 packages (15 .java files)
│   ├── lib/                    # Optional external JARs (mysql-connector-j-*.jar, gitignored)
│   ├── bin/                    # Compiled .class files (gitignored)
│   ├── run.bat                 # Compile & launch Main
│   ├── test.bat                # Compile & run TestApp (8 checks)
│   └── README.md               # Java backend reference
├── db/
│   ├── schema/                 # MySQL 8.0 DDL (01_supify_schema.sql - 5 tables)
│   ├── seeds/                  # Sample seed data (01_canonical_suppliers.sql)
│   ├── migrations/             # Incremental scripts (future)
│   ├── setup_mysql.bat         # One-click MySQL initializer
│   └── README.md               # Database structure & SQL queries
└── docs/
    ├── setup.md                # This guide
    ├── understanding.md        # Problem framing, design axioms & trust model
    ├── architecture.md         # Frontend architecture & state contracts
    ├── backend-architecture.md # Backend UML class diagrams, sequences & concurrency charts
    └── database-architecture.md# MySQL 8.0 ER diagram & JDBC flow
```

---

## 2. Prerequisites

| Layer | Required Software | Minimum Version | Notes |
| :--- | :--- | :--- | :--- |
| **Frontend (`src/`)** | Node.js & npm | Node 18+ | Serves Vite dev server and runs Vitest suite |
| **Backend (`backend/`)** | Java Development Kit (JDK) | JDK 17+ | `javac` and `java` available on `PATH`; no Maven/Gradle needed |
| **Database (`db/`)** | MySQL Server + `mysql` CLI | MySQL 8.0+ | Optional for pure in-memory CLI mode; required for JDBC checks |
| **JDBC Driver** | `mysql-connector-j-*.jar` | 8.x+ (tested on 26.10.0) | Place inside `backend/lib/` (excluded from Git by `.gitignore`) |

---

## 3. Running the React Frontend (`src/`)

From the repository root:

```bash
npm install
npm run dev
```

- Opens on `http://localhost:5173`.
- Uses decoupled HTTP JSON fixtures in `public/data/*.json` (does not depend on `backend/`).
- Run automated frontend checks or production build:

```bash
npm test
npm run build
```

---

## 4. Running the Java Backend CLI (`backend/`)

The backend CLI runs standalone with zero external JARs using `SupifyPlatform` and `loadSampleData()`.

### Option A: One-Click Windows Batch Scripts

Inside `backend\`:
- Run **`run.bat`** to compile into `bin\` and start `com.supify.app.Main`.
- Run **`test.bat`** to compile and run the 8-check test suite (`com.supify.app.TestApp`).

### Option B: Manual Compile & Run Commands

From `backend\`:

```bat
if not exist bin mkdir bin
javac -d bin -sourcepath src src\com\supify\app\Main.java src\com\supify\app\TestApp.java src\com\supify\dao\JdbcSupplierDao.java
java -cp "bin;lib\*" com.supify.app.Main
java -cp "bin;lib\*" com.supify.app.TestApp
```

### Quick CLI Demo Flow

1. Launch `backend\run.bat`.
2. **Buyer Menu (`[1]`):** List suppliers (`[1]`), search/sort (`[2]`), inspect `SUP-1`'s 4-layer Claim -> Evidence -> VerificationCheck -> Attestation chain (`[3]`), or compare 2 suppliers (`[4]`).
3. **Supplier & Verifier Menus (`[2]` -> `[3]`):** Watch active expiry countdowns under `[2] -> [3]` (including `SUP-1`'s 30-second badge decaying via `DecayMonitorThread`), submit a claim with PDF evidence (`[2] -> [2]`), and approve/reject in `[3]`.
4. **Admin Menu (`[4]`):** View thread status (`[1]`), CSV export/import (`[2]`), plain-text audit log (`[3]`), and MySQL JDBC status (`[4]`).

---

## 5. Setting Up MySQL 8.0 & JDBC (`db/`)

### Step 1: Initialize 5-Table Schema & Seeds

Run **`db\setup_mysql.bat`** (from Explorer or terminal):
- Detects `mysql.exe`, prompts for MySQL username (default `root`) and password (`-p`).
- Loads in order:
  1. `db/schema/01_supify_schema.sql` (recreates `supify` database + 5 basic tables: `supplier`, `claim`, `evidence`, `verification_check`, `attestation`)
  2. `db/seeds/01_canonical_suppliers.sql` (seeds 5 suppliers, 12 claims, 12 evidence rows, 10 checks, and 10 attestations)

### Step 2: Stage the MySQL JDBC Driver

Copy `mysql-connector-j-*.jar` into **`backend\lib\`** (ignored by `.gitignore`). Both `backend\run.bat` and `backend\test.bat` include `-cp "bin;lib\*"` automatically.

### Step 3: Optional Password Override

Default JDBC settings in `JdbcSupplierDao` connect to `jdbc:mysql://localhost:3306/supify?...` using user `root` and password `root`. If your MySQL password differs, set `SUPIFY_DB_PASS`:

```bat
set SUPIFY_DB_PASS=your_password
```

### Step 4: Check Connection Inside the CLI

Launch `backend\run.bat`, open **`[4] Admin Menu`**, and choose **`[4] MySQL JDBC Status`** to check `Driver: true` and `Connected: true`.

---

## 6. Windows Troubleshooting

- **`javac` or `mysql` not recognized:** Add your JDK `bin` folder and MySQL `bin` folder to Windows `PATH`.
- **PowerShell splitting `-cp bin;lib\*`:** Always quote `"bin;lib\*"` so PowerShell does not split on `;`.
- **Stale `.class` files in `backend/bin/`:** Delete `backend/bin/` and re-run `backend\run.bat` for a clean rebuild.
