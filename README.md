# Supify

**A supplier discovery platform where every "verified" badge shows its work.**

Supify is a supplier discovery and evidence-backed verification marketplace. Buyers search for suppliers, open a profile, and see not just *whether* a supplier is trusted but **how each claim was checked, when, by what method, and until when it stays valid**.

It exists because of one problem: a buyer cannot cheaply tell whether a supplier is what it claims to be. So most platforms show green ticks. Supify shows the evidence instead.

---

## What is built right now

1. **React Frontend (`src/`)** — interactive SPA powered by Vite, React, React Router, Tailwind CSS, and Zustand using HTTP fixtures in `public/data/*.json` ([Frontend architecture](docs/architecture.md)).
2. **Java Backend & CLI (`backend/`)** — 13-file pure-Java CLI across 4 packages (`model`, `dao`, `service`, `app`) built around one shared `SupifyPlatform`, implementing Supplier CRUD with `data/suppliers.csv` persistence, the 4-layer verification spine (`Claim -> Evidence -> VerificationCheck -> Attestation`), background `DecayMonitorThread`, Comparator search & sort, `FileHelper` CSV & audit log I/O, and checked `SupifyException` ([Backend architecture](docs/backend-architecture.md)).
3. **MySQL 8.0 Database (`db/`)** — simple 5-table schema (`supplier`, `claim`, `evidence`, `verification_check`, `attestation`), sample seed data, and `setup_mysql.bat` ([Database ER architecture](docs/database-architecture.md)).

| You can... | Where |
| --- | --- |
| Search suppliers; filters live in the URL and survive reload/back | `/search?q=&band=` |
| Open a supplier profile with capacity facts, a claim ledger, and a trust panel | `/suppliers/aravind-fasteners` |
| Inspect any claim to see its evidence on record — deep-linkable via `?claim=` | `/suppliers/kalyani-textiles?claim=gst_registration` |
| Shortlist suppliers and compare them side by side, shareable set | `/compare?ids=a,b,c` |
| Onboard as a supplier through a 4-step wizard that saves your draft locally at every field | `/supplier/onboarding/organisation` |
| Work a verifier queue filtered by state, open a task, and record a review decision | `/verify/queue` |
| Hit an unknown address, supplier, or task and get a designed 404 / not-found state | `/nonsense` |

Consoles are role-protected: buyer, supplier, and verifier. Switch roles with the avatar button in the header and try opening `/verify/queue` as a buyer.

## Try it

### Supplier workspace additions

Open `/supplier` for the preparation dashboard, `/supplier/onboarding` for the
validated company form, or `/supplier/portfolio` for the routed portfolio preview.
The workspace uses Context with `useReducer`, custom autosave hooks, and a small
Zustand checklist store. It includes loading/error/retry states, draft recovery,
evidence selection, and a local demo submission receipt.

Run `npm test` for focused Vitest/React Testing Library checks. Uploads and
submissions in the frontend remain local simulations; the portfolio is a private draft preview.

```bash
npm install
npm run dev
```

Open http://localhost:5173. Build for production with `npm run build`.

### Backend CLI (Java)

The pure-Java backend CLI (`backend/src/com/supify/`) runs with zero external JARs using `SupifyPlatform` and `loadSampleData()`:

- **Launch interactive CLI (`backend\run.bat`):** Compiles into `backend\bin\` and runs `com.supify.app.Main` (`-cp bin`) with four menus (**Buyer**, **Supplier**, **Verifier**, **Files & Monitor**), persistent Supplier CRUD (`data/suppliers.csv`), and live 30-second expiry updates.
- **Run tests (`backend\test.bat`):** Executes `com.supify.app.TestApp` (7 checks covering Supplier CRUD & CSV restart persistence, Comparator search/sorting, 4-layer spine & capping, `DecayMonitorThread`, File I/O, and checked `SupifyException`).

```bat
cd backend
run.bat
```

## A two-minute tour

1. **Search** — type "fasteners", then filter by trust band. Notice the URL changes as you go; copy it into a new tab and land in the same place.
2. **Open Aravind Fasteners** — the trust panel shows band, method ("Registry check"), last-checked date, pillar coverage bars, and a validity window.
3. **Click "Inspect" on a claim** — evidence records expand inline. The URL now ends in `?claim=iso_9001`; reload it and the same claim opens. Back button closes it.
4. **Add two suppliers to your shortlist**, then follow **Compare shortlist** — a side-by-side table of trust band, capacity, MOQ, lead time, and established-claim counts.
5. **Switch the avatar to Supplier** and open the workspace — fill step 1, jump straight to step 3, refresh the page: nothing was lost (draft persists in localStorage).
6. **Switch to Verifier** and open the queue — filter chips map to `?state=`. Open the dual-control task, look at the eleven pieces of evidence, record a decision. The UI says explicitly that the decision waits for ledger confirmation — it never pretends to have verified anything.
7. **Look at Kalyani Textiles** — its GST attestation expired on 2 Aug 2026, and the UI says so. **Deccan Polymers** has a revoked quality claim, rendered as revoked, not hidden.

## How it works

```
public/data/*.json          fixture "database" served over HTTP
        │  fetch + simulated latency
        ▼
shared/api/repository       the ONLY module that touches the network
        │  wire → domain mapping
        ▼
entities/                   domain objects + the only components allowed to render trust
        ▼
features/                   discovery · profile · comparison · onboarding · verification-review
        ▼
app/                        routing, role context, protected routes, 404
```

**Frontend Stack:** Vite · React (JavaScript) · React Router 6 · Tailwind CSS · Zustand (HTTP JSON fixtures).  
**Backend Stack:** Java 17+ (`backend/src/com/supify/`) · 4 packages (`model`, `dao`, `service`, `app`) · Threads · Comparator & Lambdas · File I/O · Checked Exceptions.  
**Database Stack:** MySQL 8.0 (`db/`) · 5 basic tables (`supplier`, `claim`, `evidence`, `verification_check`, `attestation`).

Dependencies point downward only. Features never import each other. All shareable state (query, filters, selected claim, comparison set, queue filter) lives in the URL; session state (drafts, shortlist) lives in localStorage.

### What is real vs simulated

| Real | Simulated (by design, for now) |
| --- | --- |
| Routing, URL state, protected consoles | Network latency (~450 ms per request) |
| fetch + async/await + error/retry states | The API itself — JSON files stand in for a future service |
| localStorage draft persistence | Authentication (role switching is a demo control) |
| Adversarial data: expired attestation, revoked claim, gated supplier, 11-evidence claim, dual-control task | Writes — review decisions are recorded in local UI state only |

When a real backend arrives, exactly one file changes: `src/shared/api/supplierRepository.js`. Everything above it consumes mapped domain shapes and never sees wire formats.

## Rules this codebase obeys

These come from the design docs and are visible in the running app:

- No boolean verification flags — verification is always `{ method, date, valid-until }`.
- Trust is never shown without its method and recency.
- Unknown, expired, and revoked are rendered explicitly — silence never looks like approval.
- Trust is never conveyed by colour alone (icon + text + colour).
- Nothing is computed about trust on the client; the client renders what the data layer provides.
- Every screen has loading, empty, error, and not-found states, not just the happy path.

## Documentation

- [`AGENTS.md`](AGENTS.md) — working rules, invariants, layer structure, and project status
- [`docs/setup.md`](docs/setup.md) — local setup, batch runners, and MySQL instructions
- [`docs/understanding.md`](docs/understanding.md) — problem framing, design axioms, domain and trust model
- [`docs/architecture.md`](docs/architecture.md) — frontend architecture: layers, state, contracts
- [`docs/backend-architecture.md`](docs/backend-architecture.md) — Java 4-package layout and class diagrams
- [`docs/database-architecture.md`](docs/database-architecture.md) — MySQL 8.0 5-table ER diagram and schema overview
- [`backend/README.md`](backend/README.md) — Java backend compilation, `.bat` runners, and CLI flows
- [`db/README.md`](db/README.md) — MySQL 5-table schema, seed data, and `setup_mysql.bat`
- [`docs/contract/supplier-api.md`](docs/contract/supplier-api.md) — the data contract the fixtures satisfy

## Status

- **Frontend:** Routing, Context/reducer state, Zustand dashboard, validated forms, and async workspace recovery flows running on HTTP fixtures.
- **Backend CLI:** 13-file pure-Java CLI across 4 packages (`model`, `dao`, `service`, `app`), `TestApp`, and `run.bat`/`test.bat` implemented in `backend/`.
- **Database:** Simple 5-table MySQL 8.0 schema, seed inserts, and `setup_mysql.bat` implemented in `db/`.
