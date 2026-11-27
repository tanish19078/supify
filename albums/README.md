# Supify — Visual Albums (Diagrams, CLI Demo, Database & Frontend UI)

Organized gallery of all architecture diagrams, pure-Java 13-file CLI demo captures, MySQL 8.0 5-table outputs, and React frontend screenshots.

---

## 1. Architecture Diagrams (`albums/diagrams/`)

### Pure-Java UML Class Diagram (4 Packages · 13 Classes · 4-Member Architecture)
![Supify UML Class Diagram](diagrams/supify_uml_class_diagram.png)

### MySQL 8.0 ER Diagram (5 Relational Tables = 1:1 with Java Models)
![Supify MySQL 8.0 ER Diagram](diagrams/supify_er_diagram.png)

---

## 2. Pure-Java Interactive CLI Demo (`albums/cli-demo/`)

1. **Buyer Discovery (`[1]`):** [01_buyer_search_compare.png](cli-demo/01_buyer_search_compare.png) — 5 seeded suppliers ranked by score, lambda `Comparator` search (`Pune`), and side-by-side comparison.
2. **4-Layer Verification Spine (`[1] → [3]`):** [02_four_layer_spine.png](cli-demo/02_four_layer_spine.png) — `Claim → Evidence → VerificationCheck → Attestation` with live `30s` TTL countdown on `SUP-1`.
3. **Supplier CRUD, Byte-Stream Proof Copy & Verifier Approval (`[2]` → `[3]`):** [03_supplier_to_verifier.png](cli-demo/03_supplier_to_verifier.png) — Adds `SUP-6`, copies local PDF proof into `data/proofs/` via `FileInputStream`/`FileOutputStream`, submits `SUBMITTED` claim, and approves in Verifier Menu.
4. **Files & Expiry Monitor (`[4]`):** [04_files_and_expiry_monitor.png](cli-demo/04_files_and_expiry_monitor.png) — `main` + `ExpiryThread` (`daemon=true`), `data/suppliers.csv` save/reload, `data/audit.log` trail, and catalog reset to 5 default suppliers.
5. **Automated 7-Check Suite (`backend\test.bat`):** [05_automated_test_suite.png](cli-demo/05_automated_test_suite.png) — `TestApp.java` verifying CRUD & CSV restart persistence, Comparator search, 4-layer spine, capping, `ExpiryThread` decay, File I/O, and `SupifyException` guards (`7 PASSED, 0 FAILED`).

---

## 3. MySQL 8.0 5-Table Database Outputs (`albums/database/`)

### Standalone 5-Table Schema & 49-Row Verification Spine Join
![MySQL 8.0 5-Table Entries](database/db_live_mysql_entries.png)

- [Verifier Queue SQL Output](database/02_uc01_verifier_queue_output.png)
- [Supplier Ledger SQL Output](database/03_uc02_supplier_ledger_output.png)
- [Faceted Search SQL Output](database/04_uc03_search_faceted_output.png)

---

## 4. Frontend UI Screenshots (`albums/frontend-ui/`)

- [01_landing_hero.png](frontend-ui/01_landing_hero.png) · [02_discovery_search.png](frontend-ui/02_discovery_search.png) · [03_comparison_matrix.png](frontend-ui/03_comparison_matrix.png) · [04_supplier_workspace_dashboard.png](frontend-ui/04_supplier_workspace_dashboard.png)
- [05_onboarding_form_step.png](frontend-ui/05_onboarding_form_step.png) · [06_portfolio_about.png](frontend-ui/06_portfolio_about.png) · [07_verification_engine_run.png](frontend-ui/07_verification_engine_run.png) · [08_protected_route_verifier_guard.png](frontend-ui/08_protected_route_verifier_guard.png)
- [09_supplier_profile_claims.png](frontend-ui/09_supplier_profile_claims.png) · [10_verifier_evidence_queue.png](frontend-ui/10_verifier_evidence_queue.png) · [11_error_boundary_fallback.png](frontend-ui/11_error_boundary_fallback.png)
