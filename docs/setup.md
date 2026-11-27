# Supify — Setup, Run & Demo Guide

Complete local setup instructions for all three parts of the repository: the React Frontend (`src/`), the Pure-Java Backend CLI (`backend/`), and the MySQL 8.0 Database (`db/`).

---

## 1. Repository Layout

```text
supplier-platform/
├── AGENTS.md                   # Single canonical working rules, invariants & status
├── README.md                   # Project overview & quick tour
├── src/                        # React frontend (Vite, React Router 6, Tailwind, Zustand)
├── public/data/                # JSON fixtures served over HTTP to the frontend
├── backend/
│   ├── src/com/supify/         # Pure-Java CLI across 4 packages (13 .java files)
│   ├── bin/                    # Compiled .class files (gitignored)
│   ├── run.bat                 # Compile & launch Main
│   ├── test.bat                # Compile & run TestApp (7 checks)
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
    └── database-architecture.md# MySQL 8.0 ER diagram & 5-table schema flow
```

---

## 2. Prerequisites

| Layer | Required Software | Minimum Version | Notes |
| :--- | :--- | :--- | :--- |
| **Frontend (`src/`)** | Node.js & npm | Node 18+ | Serves Vite dev server and runs Vitest suite |
| **Backend (`backend/`)** | Java Development Kit (JDK) | JDK 17+ | `javac` and `java` available on `PATH`; no Maven/Gradle needed |
| **Database (`db/`)** | MySQL Server + `mysql` CLI | MySQL 8.0+ | Standalone 5-table relational schema & canonical seed scripts |

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

The backend CLI runs standalone with zero external JARs using `SupifyPlatform` and auto-saves changes into `backend/data/suppliers.csv`.

### Option A: One-Click Windows Batch Scripts

Inside `backend\`:
- Run **`run.bat`** to compile into `bin\` and start `com.supify.app.Main`.
- Run **`test.bat`** to compile and run the 7-check test suite (`com.supify.app.TestApp`).

### Option B: Manual Compile & Run Commands

From `backend\`:

```bat
if not exist bin mkdir bin
javac -d bin -sourcepath src src\com\supify\app\Main.java src\com\supify\app\TestApp.java
java -cp bin com.supify.app.Main
java -cp bin com.supify.app.TestApp
```

### Quick CLI Demo Flow

1. Launch `backend\run.bat` (auto-loads `backend/data/suppliers.csv`, or creates 5 sample suppliers on first run).
2. **Buyer Menu (`[1]`):** List suppliers (`[1]`), search/sort (`[2]`), inspect `SUP-1`'s 4-layer Claim -> Evidence -> VerificationCheck -> Attestation chain (`[3]`), or compare 2 suppliers (`[4]`).
3. **Supplier Menu (`[2]`):** Switch active supplier (`[1]`), **Add New Supplier** (`[2]`), **Update Active Supplier** (`[3]`), **Delete a Supplier** (`[4]`), submit a claim with PDF evidence (`[5]`), and watch active expiry countdowns (`[6]`, including `SUP-1`'s 30-second badge decaying via `ExpiryThread`).
4. **Verifier Menu (`[3]`):** View pending claims (`[1]`) and approve/reject a claim (`[2]`) to update trust score and `data/suppliers.csv`.
5. **Files & Monitor Menu (`[4]`):** View thread status (`[1]`), save & reload CSV (`[2]`), view `audit.log` (`[3]`), or reset back to the 5 default sample suppliers (`[4]`).

---

## 5. Setting Up MySQL 8.0 (`db/`)

### Step 1: Initialize 5-Table Schema & Seeds

Run **`db\setup_mysql.bat`** (from Explorer or terminal):
- Detects `mysql.exe`, prompts for MySQL username (default `root`) and password (`-p`).
- Loads in order:
  1. `db/schema/01_supify_schema.sql` (recreates `supify` database + 5 basic tables: `supplier`, `claim`, `evidence`, `verification_check`, `attestation`)
  2. `db/seeds/01_canonical_suppliers.sql` (seeds 5 suppliers, 12 claims, 12 evidence rows, 10 checks, and 10 attestations)

### Step 2: Querying the Database Directly

Connect with the MySQL client (`mysql -u root -p supify`) to inspect the tables (`SHOW TABLES;`, `SELECT * FROM supplier;`, or joins across `claim`, `evidence`, `verification_check`, and `attestation`).

---

## 6. Windows Troubleshooting

- **`javac` or `mysql` not recognized:** Add your JDK `bin` folder and MySQL `bin` folder to Windows `PATH`.
- **Stale `.class` files in `backend/bin/`:** Delete `backend/bin/` and re-run `backend\run.bat` for a clean rebuild.
