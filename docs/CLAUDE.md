# CLAUDE.md

Working agreement for this repository. Read this first, every session.

---

## What this is

A **supplier discovery and verification platform**. Buyers find suppliers; suppliers prove they are
what they claim; trained verifiers turn submitted evidence into time-bounded attestations.

The product exists to solve one problem: *a buyer cannot cheaply observe whether a supplier is what
they claim to be.* Every feature must reduce either the cost of **finding** a supplier or the cost of
**trusting** one. Anything else is cut. This is called the **Asymmetry Razor** and it is the
scope-control test for the whole project.

---

## Document map

| Document | Contains | Read when |
| --- | --- | --- |
| `CLAUDE.md` (this) | Working rules, invariants, conventions | Always |
| `docs/understanding.md` | Problem framing, design axioms, domain model, trust model, lifecycles | Before any design or modelling decision |
| `docs/architecture.md` | Frontend architecture: layers, state, contracts, components | Before writing any code |

**Do not duplicate content between these.** If a rule needs to change, change it in one place and
link. Drift between documents is worse than a missing document.

---

## Current status

| | State |
| --- | --- |
| Problem framing | Settled — `docs/understanding.md` |
| Domain & trust model | Settled — `docs/understanding.md` §3–§5 |
| Frontend architecture | Settled — `docs/architecture.md` |
| **Stack** | **Chosen (Eval-1 scope):** Vite + React (JavaScript) + react-router-dom. Fixtures only, no backend/DB. Restricted to course topics through lecture 42. |
| Database | Deferred. Modelled in `understanding.md` §3, not being built yet. |
| Backend | Deferred. Frontend builds against a contract + fixtures. |
| Code | Eval-1 scope implemented — Vite + React + react-router-dom, fixtures over HTTP, four consoles, comparison and claim inspector. Backend still deferred. |

Anything stack-dependent is marked `TBD` in the architecture doc. **Do not silently resolve a
`TBD`.** Raise it.

---

## Non-negotiable invariants

These derive from the ten axioms in `understanding.md` §2. They are listed here as concrete *code*
rules because these are the ones most easily violated by accident — each is the shortcut that is
genuinely faster right up until the moment it isn't.

### On trust and verification

1. **Never add a boolean verification flag.** No `isVerified`, `hasGST`, `isTrusted`. Verification
   state is always `{ method, verifiedAt, validUntil, attestor, evidence }`. A boolean cannot express
   *how*, *when*, or *until when*, and those are the entire point.

2. **Never compute a trust score on the client.** The server computes; the client renders. A score
   computed in the browser is a score that can be reverse-engineered and forged.

3. **Never render trust without its method and date.** "Verified ✓" alone is a lie of omission.
   "Verified — registry check, 4 days ago" is information. Always the triple: state, method, recency.

4. **Never render an empty or unknown state as neutral.** A blank field reads as tacit approval.
   Unknown is an explicit, designed, rendered state.

5. **Never convey trust by colour alone.** Always text + icon + colour. This is both an
   accessibility requirement and an information-integrity one.

6. **Never let a supplier-facing code path write to attestation or verification state.** The supplier
   asserts; the platform attests. Separate concerns, separate writes, no exceptions.

### On structure

7. **Never hardcode a claim type.** No `switch (claimType)` anywhere. Claim types are data from a
   registry, and the UI is generated from their descriptors. Adding a new certification must be a
   data change, not a code change. See `architecture.md` §7.

8. **Never import across features.** `features/discovery` may not import from
   `features/verification-review`. Cross-feature composition happens at the `app` layer. Dependencies
   point downward only: `app → features → entities → shared`.

9. **Never render trust outside the entity components.** `TrustBand`, `ClaimRow`, `MethodChip` and
   family live in `entities/` and are the *only* way trust is displayed. Duplicating them per feature
   guarantees drift, and drift means two actors see different things about the same claim.

10. **Never consume wire shapes directly in the UI.** The API boundary maps wire → domain types. The
    backend does not exist yet and its shapes *will* change.

### On state

11. **Anything shareable belongs in the URL.** Search queries, filters, sort, pagination, active tab,
    selected claim, comparison set. If a user might bookmark it, send it to a colleague, or reach it
    with the back button, it is URL state — not component state. This is the most frequently broken
    rule in the list because component state is always the easier path.

12. **Never lose a supplier's work.** Onboarding spans days on unreliable connections. Persist
    per-step, locally and immediately.

---

## Layer structure

```
  app        shell, routing, providers, role-based composition, cross-feature wiring
   ↓
  features   discovery · supplier-onboarding · verification-review · comparison · trust · rfq
   ↓
  entities   supplier · claim · evidence · attestation · organisation · user
   ↓
  shared     ui primitives · api client · formatting · validation · hooks · config
```

Dependencies point **downward only**. Never upward, never sideways between features.
Each feature exposes a single public entry point; reaching into a feature's internals is a violation.

**"Where does this go?"** — decision procedure:

- Used by more than one feature, and knows nothing about the domain → `shared/ui`
- Used by more than one feature, and *does* know about the domain → `entities/<thing>`
- Used by exactly one feature → that feature
- Wires two features together → `app`

When genuinely unsure, put it in the feature. Promoting later is easy; untangling a premature
abstraction is not.

---

## State: where things live

| Kind | Examples | Home |
| --- | --- | --- |
| Server | supplier records, queues, quotes | remote cache with explicit staleness |
| **URL** | query, filters, sort, page, tab, selected claim | **the URL** — see invariant 11 |
| Session | shortlist, comparison tray, wizard drafts | client store + local persistence |
| Ephemeral | modal open, hover, focus, disclosure | component-local |
| Form | draft values, validation, dirty state | form scope, autosaved on long flows |

Full reasoning in `architecture.md` §5.

---

## Definition of done

A surface is not finished until **all** of these hold:

- [ ] All seven render states exist: idle, loading, empty, partial, error, stale, **pending-external**
- [ ] Renders correctly against the **adversarial fixture** — the supplier with expired attestations,
      a revoked claim, an applied gate, a 90-character legal name, and 14 claims
- [ ] Trust rendered exclusively through entity components
- [ ] Shareable state is in the URL and survives reload and back-navigation
- [ ] Fully keyboard operable (mandatory everywhere; non-negotiable in the verifier console)
- [ ] No information conveyed by colour alone
- [ ] No cross-feature imports
- [ ] Loading states match final layout — no layout shift on content arrival

**"Pending-external"** is specific to this product: a third-party check in flight is neither loading
nor complete. Rendering it as either one is inaccurate.

---

## Fixtures

Fixtures must be **adversarial toward our own UI**. Seed data full of tidy happy paths is how a demo
passes and the real product breaks.

Every fixture set must include: an expired attestation, a revoked claim, a gated supplier, a
supplier mid-dual-control, a zero-result search, a claim with eleven pieces of evidence, a supplier
with only self-declared claims, and a name long enough to break every layout that assumes it won't.

---

## Naming

| Thing | Convention | Example |
| --- | --- | --- |
| Domain types | `PascalCase`, singular, no prefixes | `Claim`, `Attestation` |
| Wire types | suffixed, quarantined at the boundary | `ClaimWire`, `SupplierWire` |
| Components | `PascalCase`, named for what they *are* | `TrustBand`, `ClaimRow` |
| Hooks | `use` + noun phrase | `useSupplierSearch` |
| Booleans | `is` / `has` / `can` — **never for verification state** | `isExpanded`, `canEscalate` |
| Route params | `kebab-case` | `/suppliers/:id/claims/:claim-id` |
| Files | match the primary export | `TrustBand.tsx` |

Use the vocabulary from `understanding.md` Appendix B exactly. *Claim*, *evidence*, *check*,
*attestation*, *method*, *pillar*, *band*, *gate* are terms of art with precise meanings. Using them
loosely in code makes the model incoherent — a "verification" variable that is sometimes a check and
sometimes an attestation is a bug waiting to happen.

---

## Stop and ask

Raise rather than assume when you hit:

- A stack decision (nothing is chosen yet)
- Anything requiring a new claim type semantic that the registry cannot express
- A trade-off between explainability and any other property — **explainability wins**, but say so
  out loud rather than deciding quietly
- Any of the twelve invariants appearing to need an exception
- Any of the seven open questions in `understanding.md` §12, especially jurisdiction and
  vertical-vs-horizontal — both cascade widely

---

## Working style

- Terminology is load-bearing. Precision in naming is not pedantry here.
- Prefer deleting to adding. The razor applies to code as well as features.
- If a decision is non-obvious, record *why* — not just what. The reasoning is what survives contact
  with a changed requirement.
- Explainability outranks elegance, everywhere. A model nobody can act on has no effect.
