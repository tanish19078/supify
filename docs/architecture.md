# ARCHITECTURE.md — Frontend

**Project:** Supplier Discovery & Verification Platform
**Scope:** frontend only. Persistence is deferred; see `understanding.md` §3 for the domain model
this architecture is shaped around.
**Status:** v0.1. Stack not chosen — stack-dependent points are marked `TBD`.

> Notation: type sketches are written in TypeScript syntax because it is the clearest available way
> to express a domain model on paper. That is a **notation choice, not a stack commitment.**

---

## 1. What this document is for

`understanding.md` answers *what the product is and why*. This document answers *how the frontend is
organised so that stays true as the codebase grows*.

Read it before writing code. The rules here are not style preferences — each one exists to prevent a
specific failure that this particular product is prone to.

**Reading path:** §2 (goals) → §3 (layers) → §5 (state) → §6 (data boundary) are the load-bearing
core. §7–§9 are the parts unique to this domain. §10 onward is supporting detail.

---

## 2. What this architecture must make easy — and hard

Architecture is a set of deliberate biases. Stating them up front makes the rest of the document
derivable rather than arbitrary.

### Must be easy

| Requirement | Where addressed |
| --- | --- |
| Add a new verifiable claim type without touching page code | §7 |
| Render trust identically in all three consoles | §9.3 |
| Swap fixtures for a real API without touching UI | §6 |
| Share a filtered search result as a link | §5.3 |
| Ship a console without shipping the other two to the browser | §13.2 |
| Build a screen with all its states, not just the happy one | §8 |

### Must be hard

| Anti-requirement | Guarded by |
| --- | --- |
| Rendering a trust badge without its method and date | §9.3 — the component won't accept it |
| Computing trust in a component | §6.2 — the domain type has no inputs to compute from |
| Putting a shareable filter in local state | §5.3 |
| Coupling two features together | §3.2 |
| Letting a supplier-facing path write verification state | §11.2 |
| Shipping a screen that only works on tidy data | §14.3 |

The second table matters more than the first. Making the right thing easy is common; making the
wrong thing *structurally awkward* is what actually holds a codebase together over months.

---

## 3. The layer model

### 3.1 Four layers

```
┌──────────────────────────────────────────────────────────────────────┐
│  app          Routing, providers, three console shells, cross-feature │
│               wiring, auth boundary, error boundaries                 │
├──────────────────────────────────────────────────────────────────────┤
│  features     Self-contained product capabilities. Own their screens, │
│               orchestration, and feature-local state.                 │
│               discovery · supplier-onboarding · verification-review · │
│               comparison · rfq                                        │
├──────────────────────────────────────────────────────────────────────┤
│  entities     Domain objects + the canonical way each one renders.    │
│               supplier · claim · evidence · attestation · trust ·     │
│               organisation · user                                     │
├──────────────────────────────────────────────────────────────────────┤
│  shared       Domain-ignorant infrastructure. UI primitives, API      │
│               client, formatting, validation, hooks, config.          │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.2 The dependency rule

**Dependencies point downward only.**

```
  app  →  features  →  entities  →  shared
```

- A feature may import from `entities` and `shared`. ✅
- A feature may **not** import from another feature. ❌
- `entities` may not import from `features`. ❌
- `shared` may not import from anything above it. ❌

**Enforce this with a lint boundary rule, not with reviewer memory.** Architectural constraints that
depend on someone remembering them decay on a predictable schedule. `TBD` — exact tooling follows the
stack decision.

**When two features need each other**, you have three legitimate options and one illegitimate one:

| Situation | Solution |
| --- | --- |
| They share a domain concept | Push it down into `entities` |
| They share infrastructure | Push it down into `shared` |
| One needs to *navigate to* or *compose with* the other | Wire them at the `app` layer |
| ~~Import directly~~ | ❌ Never |

*Worked example.* The verifier console needs to show a supplier card. The buyer console also shows a
supplier card. Wrong answer: `verification-review` imports from `discovery`. Right answer:
`SupplierCard` is an **entity component** in `entities/supplier`, and both features consume it.

### 3.3 Feature encapsulation

Every feature exposes exactly one public entry point. Reaching past it is a violation.

```
features/discovery/
  ui/           screens and feature-specific components
  model/        feature-local state, state machines
  api/          queries and mutations owned by this feature
  lib/          pure helpers
  index.ts      ← THE PUBLIC API. Only this may be imported from outside.
```

```ts
// features/discovery/index.ts
export { SearchScreen } from './ui/SearchScreen'
export { useSupplierSearch } from './model/useSupplierSearch'
// Everything else is private.
```

This is what keeps a feature refactorable. If any file can be imported from anywhere, every file is
public API and nothing can be changed safely.

### 3.4 "Where does this go?" — decision procedure

```
Does it know anything about our domain (claims, trust, suppliers)?
├─ NO  → shared/
│         ├─ visual?           → shared/ui
│         ├─ data fetching?    → shared/api
│         └─ pure function?    → shared/lib
│
└─ YES → Is it used by more than one feature?
          ├─ YES → entities/<the-thing-it-is-about>
          └─ NO  → Does it wire two features together?
                    ├─ YES → app/
                    └─ NO  → features/<that-feature>
```

**When genuinely unsure, put it in the feature.** Promoting code later is cheap. Untangling a
premature abstraction that three features now depend on is not.

---

## 4. Module map

### 4.1 Features

| Feature | Owns | Primary console |
| --- | --- | --- |
| `discovery` | Search, facets, results, ranking controls | Buyer |
| `comparison` | Shortlist, comparison table, difference highlighting | Buyer |
| `supplier-onboarding` | Wizard, claim submission, evidence upload, trust ladder | Supplier |
| `supplier-profile` | Public profile, profile preview, claim drawer | Buyer + Supplier |
| `verification-review` | Queue, review workspace, decisions, escalation | Verifier |
| `rfq` | RFQ composer, invitations, quote inbox *(later phase)* | Buyer + Supplier |
| `notifications` | Cross-console async transition surfacing | All |

### 4.2 Entities

| Entity | Domain types | Canonical components |
| --- | --- | --- |
| `supplier` | `Supplier`, `SupplierSummary` | `SupplierCard`, `SupplierHeader`, `CapabilityList` |
| `claim` | `Claim`, `ClaimType`, `ClaimState` | `ClaimRow`, `ClaimDrawer`, `ClaimStateBadge` |
| `evidence` | `Evidence`, `EvidenceKind` | `EvidenceThumb`, `EvidenceViewer` |
| `attestation` | `Attestation`, `Method` | `MethodChip`, `AttestationTrail`, `FreshnessIndicator` |
| `trust` | `TrustProfile`, `Band`, `Pillar`, `Gate` | `TrustBand`, `TrustPanel`, `PillarBars`, `GateNotice` |
| `organisation` | `Organisation`, `Membership` | `OrgBadge` |
| `user` | `User`, `Capability` | `UserChip` |

`trust` is an entity rather than a feature deliberately. It is a **domain object rendered
everywhere**, not a capability a user exercises. Making it a feature would immediately force
cross-feature imports and break §3.2.

---

## 5. State architecture

The single highest-leverage decision in a frontend this size. Most frontends of this scale rot for
one reason: everything lands in one global store, ownership becomes unknowable, and nobody can tell
what invalidates what.

### 5.1 Five kinds of state

| # | Kind | Owns | Key property | Examples |
| --- | --- | --- | --- | --- |
| 1 | **Server** | remote cache | Has an age. Never "owned". Invalidated by mutations. | supplier records, verifier queue, quotes |
| 2 | **URL** | the address bar | Shareable, bookmarkable, back-button correct | filters, sort, page, active tab, selected claim |
| 3 | **Session** | client store + local persistence | Survives navigation and reload | shortlist, comparison tray, wizard drafts |
| 4 | **Ephemeral** | the component | Dies with the component. Never global. | modal open, hover, focus, disclosure |
| 5 | **Form** | form scope | Autosaved on long flows. Never lost. | draft values, validation, dirty tracking |

### 5.2 Decision procedure

```
Does it come from the server?
├─ YES → Server state. Cache it. Give it a staleness policy. Never copy into local state.
│
└─ NO  → Would a user want to share, bookmark, or back-button to it?
          ├─ YES → URL state. Non-negotiable.
          │
          └─ NO  → Must it survive a page reload?
                    ├─ YES → Session state (persisted)
                    │
                    └─ NO  → Is it a field the user is editing?
                              ├─ YES → Form state
                              └─ NO  → Ephemeral. Keep it local.
```

**The most common mistake** is copying server state into local state to "make it editable." Don't.
Server state plus a form draft are two separate things; collapsing them is how you get stale reads
and lost edits simultaneously.

### 5.3 The URL contract

For a discovery product this is architecture, not routing trivia.

> **Rule:** any state that changes what is on screen in a way a user might want to share, bookmark,
> or return to belongs in the URL.

A buyer who filters to *"verified fastener suppliers in Ludhiana with capacity above 50,000"* and
cannot send that to a colleague has been handed a broken tool. This is the rule broken most often,
because component state is always the path of least resistance for exactly the filters that most need
to be shareable.

**Search URL schema:**

```
/search
  ?q=            free text
  &cat=          category slug (repeatable)
  &country=      ISO-2
  &region=       region / state
  &band=         unverified | basic | verified | audited (repeatable)
  &capacity_min= number
  &moq_max=      number
  &lead_max=     days
  &certs=        claim type key (repeatable)
  &sort=         relevance | trust | capacity | recency
  &blend=        0..100   ← relevance-vs-trust weighting (axiom A7)
  &page=         number
```

`blend` is in the URL because it changes results, and axiom **A7** requires that trust-weighting be
visible and buyer-controlled. Ranking that cannot be inspected is indistinguishable from ranking that
has been sold.

**Deep links that must work:**

```
/suppliers/:id                          profile
/suppliers/:id/claims/:claimKey         claim drawer, directly linkable
/compare?ids=a,b,c                      comparison set
/supplier/onboarding/:step              resumable wizard position
/verify/queue?state=&assigned=&sla=     filtered queue
/verify/tasks/:taskId                   review workspace
```

`/suppliers/:id/claims/:claimKey` is worth calling out. A buyer must be able to send a colleague
*this exact claim and its evidence* — that is the concrete expression of axiom **A1**
(decomposability). If the claim drawer is modal-only state, the deepest and most important level of
trust disclosure becomes unshareable.

### 5.4 Session state and persistence

| What | Persisted | Synced to server | Why |
| --- | --- | --- | --- |
| Shortlist | yes | when authenticated | Buyer's working set spans sessions |
| Comparison tray | yes | no | Transient but must survive navigation |
| Wizard drafts | **immediately** | on reconnect | §6.2 of `understanding.md` — onboarding spans days |
| Queue preferences | yes | yes | Verifier ergonomics |

**Wizard drafts persist locally on every field change, not on step submit.** Suppliers gather
documents over days, on phones, on unreliable connections. Losing their work loses the supplier — and
supply-side liquidity is what the whole platform depends on.

---

## 6. The data and contract boundary

Backend arrives later. That makes this section load-bearing rather than theoretical.

### 6.1 The boundary

```
  fixtures / API  ──wire shapes──▶  ADAPTER  ──domain types──▶  entities → features → UI
                                       │
                                  validate, map,
                                  normalise, default
```

**The UI never touches a wire shape.** Domain types are defined by us, from `understanding.md` §3. A
thin adapter translates at the boundary.

```ts
// shared/api/wire/claim.ts        — mirrors the server exactly, ugly is fine
type ClaimWire = {
  id: string
  claim_type_key: string
  value: unknown
  state: string
  attestation?: { method_achieved: string; verified_at: string; valid_until: string | null }
}

// entities/claim/model/types.ts   — what the UI actually uses
type Claim = {
  id: ClaimId
  type: ClaimType                  // resolved from the registry, not a bare string
  value: ClaimValue
  state: ClaimState                // union, not string
  attestation: Attestation | null
  freshness: Freshness             // derived at the boundary, never in a component
}

// entities/claim/api/adapter.ts
function toClaim(wire: ClaimWire, registry: ClaimTypeRegistry): Claim { … }
```

### 6.2 Why this earns its keep

1. **The backend will change shape, repeatedly.** It does not exist yet. The adapter absorbs churn
   that would otherwise ripple into every component.
2. **Development proceeds today** against fixtures satisfying the same contract.
3. **The contract document becomes the backend's specification.** One artefact, two consumers.
4. **Derived values are computed once, at the edge.** `freshness` is computed in the adapter, not in
   `ClaimRow`. So components stay presentational, and — importantly — a component *has no inputs from
   which to compute a trust score*, which is how invariant 2 becomes structurally enforced rather
   than merely stated.

### 6.3 Contract-first workflow

```
1. Write the contract         →  docs/contract/*.md — endpoints, shapes, errors, pagination
2. Write fixtures             →  satisfying that contract, including adversarial cases (§14.3)
3. Write the adapter          →  wire → domain
4. Build UI                   →  against domain types only
5. Backend implements the contract later; only the fixture source is swapped.
```

Steps 1–2 are also the backend team's brief. Writing them well is not overhead — it is the
deliverable that lets two halves of the project proceed independently.

### 6.4 The verification adapter

Third-party checks sit behind one interface with swappable implementations:

```ts
interface VerificationAdapter {
  check(claimType: ClaimTypeKey, value: ClaimValue): Promise<CheckResult>
}
```

`SimulatedAdapter` now, `LiveAdapter` later — a one-file substitution by construction.

The simulation is **engineered, not faked**. It must be deterministic (same input → same outcome,
seeded from the input), exhibit realistic latency with variance, and **inject failures** — timeouts,
`inconclusive`, provider-down, malformed responses.

The reason is architectural rather than cosmetic: because failure injection exists from day one, the
degraded and async UI states get built *because they occur in development*, rather than being
retrofitted after a live integration exposes them. Most projects discover their error states in
production. We can discover ours in fixtures.

---

## 7. Registry-driven rendering

This is the mechanism that makes the platform extensible, and it is the architectural rule most
likely to be violated under deadline pressure — because hardcoding the first three claim types is
genuinely faster. It is faster right up until the fourth.

### 7.1 The problem it solves

Without this, every new certification touches: the onboarding wizard, the verifier review panel, the
profile display, the search facets, and the validation logic. Five files, two consoles, per
certification. That is how a system ossifies.

### 7.2 The descriptor

Claim types are **data**, loaded from the registry. The UI is generated from their descriptors.

```ts
type ClaimTypeDescriptor = {
  key: ClaimTypeKey                 // 'gst_registration', 'iso_9001'
  label: string
  helpText: string                  // shown to supplier: what this is, WHY it matters
  pillar: Pillar
  valueSchema: JSONSchema           // → form fields + validation
  acceptedEvidence: EvidenceKind[]  // → upload affordances
  maxMethod: Method                 // ceiling; some claims never exceed self_declared
  appliesWhen: Predicate            // → whether to show it at all
  sortOrder: number

  render?: {                        // escape hatch — see §7.4
    value?: Component<{ value: ClaimValue }>
    summary?: Component<{ claim: Claim }>
  }
}
```

`helpText` is not filler. `understanding.md` §6.2 requires every request of a supplier to justify
itself inline — *"verified suppliers appear in filtered searches, which is where most buyers start."*
A supplier who cannot see the payoff rationally declines the work. The copy is part of the descriptor
because it is part of the design.

### 7.3 The render pipeline

```
ClaimTypeDescriptor
   │
   ├─▶ valueSchema      ──▶  FormFields + validation      (onboarding wizard)
   ├─▶ acceptedEvidence ──▶  EvidenceSlots                (onboarding wizard)
   ├─▶ appliesWhen      ──▶  visibility                   (all consoles)
   ├─▶ pillar + label   ──▶  ClaimRow                     (profile, ladder, dossier)
   └─▶ maxMethod        ──▶  achievable ceiling display   (trust ladder)
```

Adding *"BIS certification"* = insert one registry row. No new screen. No page code deployed.

### 7.4 The escape hatch

Some claim values are genuinely unusual — a capacity claim spanning multiple categories does not
render meaningfully as a generic key-value list.

`render.value` allows a custom renderer for that claim type. **The escape hatch is per-claim-type and
lives with the descriptor** — so the exception stays contained. What it must never become is a
`switch (claimType)` inside a shared component, which is the same coupling wearing a disguise.

---

## 8. Async model and render states

Verification is asynchronous by nature (axiom **A9**: every async process must be visible). The UI
is built around state machines, not request/response.

### 8.1 Seven render states

Every remote-backed surface has all seven. This is enforced at the type level so it cannot be
forgotten:

```ts
type Async<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success';  data: T; isStale: boolean }
  | { status: 'empty' }
  | { status: 'partial';  data: Partial<T>; missing: string[] }
  | { status: 'error';    error: AppError; retry: () => void }
  | { status: 'pending_external'; since: Date; provider: string; expectedBy?: Date }
```

| State | Meaning | Typical rendering |
| --- | --- | --- |
| `idle` | Not requested yet | Nothing, or a prompt |
| `loading` | In flight, nothing to show | Skeleton **matching final layout** |
| `success` | Data present | Content; if `isStale`, a subtle freshness marker |
| `empty` | Succeeded, nothing found | Designed empty state with a next action |
| `partial` | Some data, some unavailable | Content + explicit gaps — **never blank** (axiom A4) |
| `error` | Failed | Cause + recovery affordance |
| `pending_external` | Third-party check in flight | "Checking with registry — started 2 min ago" |

**`pending_external` is specific to this product** and is the state most likely to be modelled
incorrectly. A registry check in flight is neither *loading* (the page loaded fine) nor *done*.
Collapsing it into either is inaccurate, and inaccuracy about verification state is the one category
of error this product cannot afford.

**`partial` matters for the same reason.** A supplier profile where three claims loaded and one
failed must show the gap explicitly. Silently rendering three claims implies the fourth doesn't
exist — absence reading as adequacy, which is exactly axiom **A4**.

### 8.2 Optimistic update policy

| Action | Optimistic? | Reason |
| --- | --- | --- |
| Add to shortlist | ✅ Yes | Trivially reversible, high frequency |
| Reorder comparison | ✅ Yes | Pure UI |
| Save wizard draft | ✅ Yes | Local-first by design |
| Submit a claim | ⚠️ Partial | Show `submitted`, but never `verified` |
| **Issue an attestation** | ❌ **Never** | A verification decision is shown as made only after the server confirms it |
| Revoke an attestation | ❌ Never | Same |

The asymmetric failure cost from `understanding.md` §1.3 applies at the level of micro-interactions
too. A shortlist that briefly lies costs nothing. A verification state that briefly lies is the exact
failure the platform exists to prevent.

### 8.3 State machines in the UI

The claim lifecycle (`understanding.md` §5.1) is modelled explicitly, not as a status string with
`if` branches scattered across components:

```ts
type ClaimState =
  | 'draft' | 'submitted' | 'under_review' | 'needs_more_info'
  | 'verified' | 'expiring_soon' | 'expired' | 'rejected' | 'revoked' | 'withdrawn'

// Available actions are derived from state + capability — never hand-branched in a component.
function availableActions(claim: Claim, actor: Capability[]): ClaimAction[]
```

`expiring_soon` is derived, not stored — it is `verified` within the notification window. It exists
in the UI type because it is a distinct visual state and a distinct notification trigger.

---

## 9. Component architecture

### 9.1 Three tiers

| Tier | Knows about domain? | Location | Example |
| --- | --- | --- | --- |
| **Primitives** | No | `shared/ui` | `Button`, `Dialog`, `Table`, `Field` |
| **Entity components** | Yes | `entities/*` | `TrustBand`, `ClaimRow`, `SupplierCard` |
| **Feature compositions** | Yes, plus workflow | `features/*` | `SearchResults`, `ReviewWorkspace` |

Primitives must remain domain-ignorant. The moment `Button` accepts a `trustBand` prop, the design
system has been fused to the domain and can no longer be reasoned about — or replaced — independently.

### 9.2 Composition over configuration

Prefer composition. A component with fourteen boolean props is a component that has absorbed four
different use cases and now serves none of them well.

```tsx
// ❌ configuration
<SupplierCard supplier={s} showTrust showCapacity compact hideActions inQueue />

// ✅ composition
<SupplierCard supplier={s}>
  <SupplierCard.Trust />
  <SupplierCard.Capacity />
</SupplierCard>
```

### 9.3 The trust component family

The most reused and most correctness-critical components in the system. They live in
`entities/trust` and `entities/claim`, and they are the **only** way trust is ever rendered.

| Component | Renders | Enforces |
| --- | --- | --- |
| `TrustBand` | Band pill: text + icon + colour | Never colour alone |
| `PillarBars` | Per-pillar coverage | Decomposability |
| `TrustPanel` | Full breakdown + gates + last-checked | Axiom A1 |
| `GateNotice` | *Why* a supplier is capped | Explainability of ceilings |
| `ClaimRow` | One claim: label, method, freshness, state | Always shows method + date |
| `MethodChip` | Position on the method ladder | Axiom A2 |
| `FreshnessIndicator` | fresh / aging / expiring / expired | Axiom A3 |
| `AttestationTrail` | Append-only history | Axiom A5 |
| `EvidenceViewer` | In-app, zoomable, keyboard navigable | Never a raw download link |

**Enforcement by type signature.** The components make the wrong thing impossible to express:

```ts
// ❌ this shape must not exist anywhere
type BadProps = { verified: boolean }

// ✅ method and recency are structurally required
type TrustBandProps = {
  band: Band
  verifiedAt: Date
  method: Method
  gatesApplied?: Gate[]
}
```

You cannot render `<TrustBand>` without supplying a method and a date. Invariant 3 is not a rule
someone has to remember — it is a compile error.

This is the general principle worth internalising: **the best enforcement of an architectural rule is
a type that makes violating it awkward.** Documentation is the fallback, not the mechanism.

### 9.4 Density

The three consoles have genuinely different density needs. A verifier processing a hundred tasks a
day needs compact rows; a buyer evaluating a supplier needs breathing room.

Density is a **token set**, not a prop threaded through every component: `--density-comfortable`
(buyer, supplier) and `--density-compact` (verifier), applied at the console shell.

---

## 10. Routing

### 10.1 Three consoles, three shells

```
app/
  routes/
    (public)/          landing, public supplier profiles (pending §12 Q5)
    (buyer)/           search, profile, compare, shortlist, rfq
    (supplier)/        onboarding, ladder, claims, locker, enquiries
    (verifier)/        queue, review workspace, dossier, metrics
```

Each console gets its own shell: own navigation, own density tokens, own keyboard model. They are not
one layout with a role flag — `understanding.md` §6 establishes that they differ in *interaction
model*, and layout follows from that.

### 10.2 Console-level code splitting

Split at the console boundary. A buyer must never download the verifier console's code. This is the
single largest bundle win available and it comes free from the route-group structure above.

---

## 11. Permissions and role-scoped rendering

### 11.1 Capabilities, not roles

Check **capabilities**, not role strings:

```ts
// ❌
if (user.role === 'verifier') { … }

// ✅
if (can('claim.decide', { task, user })) { … }
```

Role checks cannot express the constraints this domain actually has:

- A verifier **may not decide a claim for a supplier they hold a membership with** — conflict of
  interest, not a role question.
- High-weight claims require **dual control**; the second reviewer must not see the first decision.
- An escalated task is decidable only by a senior verifier.

None of these are expressible as `role === x`. All are natural as capability predicates.

### 11.2 Rendering is not security

> **Hiding a control is convenience. Authorisation is the server's job. Always.**

Role-scoped rendering exists so users are not shown things they cannot use. It is not a security
boundary and must never be treated as one. The corresponding invariant — no supplier-facing path
writes verification state — is enforced server-side and in the schema (`understanding.md` §3.7); the
frontend simply does not offer the affordance.

---

## 12. Error architecture

### 12.1 Taxonomy

Errors are typed, because different causes need different recoveries:

| Kind | Cause | Recovery offered |
| --- | --- | --- |
| `network` | Offline, timeout | Retry; queue if it was a write |
| `auth` | Session expired | Re-authenticate, preserve intent |
| `forbidden` | Lacks capability | Explain; offer escalation path |
| `validation` | Bad input | Field-level messages |
| `conflict` | State moved underneath (claim superseded) | Show current state, offer reload |
| `not_found` | Deleted or wrong link | Navigate somewhere useful |
| `provider_unavailable` | Third-party check adapter down | **Not a failure of our system** — say so |
| `unexpected` | Everything else | Report with request id |

`conflict` is common here and easy to overlook. A verifier opens a task; the supplier withdraws the
claim; the verifier submits a decision. That must fail cleanly and explain itself — not throw a
generic error at someone who did nothing wrong.

`provider_unavailable` deserves distinct handling because it is not our fault and — importantly —
not the supplier's. Rendering a registry outage as a verification failure would mislabel a supplier
through no action of theirs.

### 12.2 Boundaries

Three levels, so a failure never takes down more than it must:

```
Route boundary    → console remains navigable
Feature boundary  → other features on the page keep working
Widget boundary   → e.g. EvidenceViewer crashing must NOT kill the decision panel
```

That last one is concrete: a verifier who cannot render one PDF should still be able to request more
information or escalate. Losing the whole workspace to one bad file is an availability bug in the
console with the highest correctness stakes.

---

## 13. Performance architecture

### 13.1 Budgets

| Surface | Target | Why |
| --- | --- | --- |
| Search first paint | < 1.5 s | Latency reads as an empty corpus |
| Filter application | < 300 ms | Faceting must feel like manipulation, not querying |
| Profile trust panel | < 1 s | It is why the page was opened |
| Queue → workspace | < 500 ms | Multiplied by hundreds of daily transitions |
| Evidence first render | < 2 s | The reviewer's blocking step |

### 13.2 Levers

- **Console-level code splitting** (§10.2) — the biggest single win
- **List virtualisation** — DOM size must not scale with result count
- **Prefetch on intent** — hover/focus on a result prefetches the profile
- **Image budgets** — supplier logos and factory photos are the obvious payload risk
- **Search reads the projection**, never the full ledger join
- **Skeletons matching final layout** — layout shift on a comparison table destroys the scanning the
  surface exists for

---

## 14. Testing architecture

### 14.1 What to test, by layer

| Layer | Test kind | Focus |
| --- | --- | --- |
| `shared/lib`, adapters | Unit | Pure logic: freshness, formatting, gate explanation, wire→domain mapping |
| `entities/*` | Component + a11y | Renders correctly against adversarial fixtures; keyboard operable |
| `features/*` | Integration | Full flows against the mock contract |
| Critical paths | E2E | Onboarding completion · verify decision · search→compare→shortlist |
| Contract | Schema validation | Fixtures validate against the contract — catches mock drift |

### 14.2 Contract tests earn their place

Because the backend arrives later, fixtures *will* drift from the contract silently. Validating
fixtures against the contract schema in CI is what turns "the mock is out of date" from a
demo-day discovery into a build failure.

### 14.3 The adversarial fixture

**Test against ugly data by default.** Every fixture set includes:

- a supplier with an expired attestation
- a supplier with a revoked claim
- a gated supplier (identity gate applied)
- a supplier mid-dual-control
- a supplier with only self-declared claims
- a claim with eleven pieces of evidence
- a zero-result search
- a 90-character legal name in a script that breaks your assumptions

Seed data full of tidy happy paths is how a demo passes and the real product breaks. If a component
has only ever been rendered against `Acme Corp` with three clean claims, it has not been tested.

---

## 15. Design tokens

### 15.1 Semantic, never literal

```css
/* ❌ */  --green-500
/* ✅ */  --trust-verified
```

Trust colours are referenced by **meaning**, never by hue. Three reasons:

1. Band → colour mapping lives in exactly one place.
2. Bands need colour + icon + text together (§9.3); coupling them at the token level makes that
   natural rather than a thing to remember.
3. Rebranding or a dark theme touches one file.

### 15.2 Scales needed

| Scale | Values |
| --- | --- |
| Trust bands | `unverified` · `basic` · `verified` · `audited` |
| Method ladder | `self-declared` → `document` → `third-party` → `audit` → `site-visit` |
| Freshness | `fresh` · `aging` · `expiring` · `expired` |
| Claim state | `draft` · `pending` · `verified` · `rejected` · `revoked` |
| Density | `comfortable` (buyer, supplier) · `compact` (verifier) |
| Plus | type scale, spacing, radius, elevation, motion |

The method ladder needs a **visually ordered** scale — a viewer should be able to perceive
`third-party` as stronger than `document` without reading the label. That is axiom **A2** expressed
in visual design rather than in prose.

---

## 16. Open architectural questions

| # | Question | Blocks |
| --- | --- | --- |
| A1 | **Stack** — framework, routing, data layer, styling | Everything marked `TBD` |
| A2 | Search: client-side filtering over a fetched set, or server-driven query per interaction? | §5.3, §13 budgets |
| A3 | Are public supplier profiles server-rendered for SEO? | Depends on `understanding.md` §12 Q5 |
| A4 | Real-time updates for the verifier queue, or poll-on-focus? | §5.1, queue UX |
| A5 | Evidence rendering: native browser viewers, or an embedded document renderer? | §9.3, §13 |
| A6 | Offline capability for supplier onboarding — how far? | §5.4 |

**A1 blocks the least and is tempting to answer first.** Resist that. §§3–9 hold under any modern
stack, and choosing the stack after the structure is settled means the stack serves the architecture
rather than dictating it. See §16 of `understanding.md` for the same argument applied to the product.
