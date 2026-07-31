# Supify

**Supplier discovery without blind trust. Verification without theatre.**

Supify is a supplier discovery and verification platform built around one uncomfortable truth:

> Buyers do not lose money because supplier directories are hard to browse.  
> Buyers lose money because supplier claims are cheap to make and expensive to verify.

Every marketplace says it has trusted suppliers. Supify is designed to show *why* a supplier should be trusted, *how* each claim was checked, *when* that trust expires, and *who* attested to it.

No mystery scores. No permanent green ticks. No self-declared badges dressed up as verification.

## The Thesis

The supplier market has an information asymmetry problem.

Suppliers know whether their factory exists, certifications are current, capacity is real, and documents are legitimate. Buyers usually do not. Independent verification is slow and expensive, so buyers price in uncertainty or avoid the supplier entirely.

Supify exists to collapse that uncertainty.

It turns supplier claims into evidenced, time-bounded, inspectable attestations so buyers can make decisions with confidence instead of vibes.

## The Razor

Every feature must pass the **Asymmetry Razor**:

> Does this reduce the buyer's cost of finding suppliers, or the buyer's cost of trusting them?

If the answer is no, it does not belong here.

This keeps Supify from becoming a noisy B2B social network wearing a marketplace jacket. The platform has one job: make credible suppliers easier to find and easier to trust.

## What Supify Is

Supify is three systems sharing one truth:

- **Discovery engine:** find relevant suppliers by category, geography, capability, trust level, and evidence strength.
- **Verification ledger:** track claims, evidence, checks, attestations, expiry, reversals, and audit history.
- **Role-based consoles:** give buyers, suppliers, and verifiers different views over the same underlying record.

The core promise is simple:

> A buyer should never have to guess whether "verified" means anything.

## What Supify Refuses To Be

- A static supplier directory with stale listings.
- A badge farm where suppliers self-certify their way into trust.
- A black-box trust score nobody can explain.
- A marketplace that quietly buries unverified suppliers without saying why.
- A verification workflow where documents disappear into a silent queue.
- A system where "verified in 2019" still looks safe in 2026.

Trust is only useful if it is specific, current, and inspectable.

## The Trust Model

Supify treats suppliers as entities that make claims.

Those claims need evidence.

That evidence needs checks.

Those checks produce attestations.

```text
CLAIM
  "We are ISO 9001 certified"

EVIDENCE
  certificate PDF, registry response, audit document

VERIFICATION_CHECK
  platform or third-party review of that evidence

ATTESTATION
  verified, rejected, inconclusive, expired, or superseded
```

The supplier asserts. The platform attests.

That boundary is the whole game.

## Evidence Strength

Not all trust signals are equal.

Supify orders evidence by strength:

```text
self-declared
  < document provided
  < third-party checked
  < human audited
  < site verified
```

The UI must always show the method, not just the outcome.

"Verified" is too vague.

"Verified through GST registry check, valid until 10 Oct 2026" is useful.

## The Ledger

Supify's verification system is append-only.

Decisions are not edited in place. If something changes, a new record supersedes the old one. That gives the platform:

- A complete audit trail.
- Dispute resolution with real history.
- Reproducible trust computation.
- Reversal tracking.
- Time-aware verification decay.

Trust can fall without anyone doing anything wrong. Certificates expire. Registrations lapse. Capacity claims age. Supify treats that decay as a first-class product behavior, not a cleanup script.

## Product Surfaces

### Buyer Console

For buyers, Supify is a search and confidence machine.

- Trust-aware supplier search.
- Faceted filtering by category, geography, capability, and verification state.
- Supplier profiles with decomposable trust panels.
- Claim drawers showing evidence, methods, timestamps, and attestors.
- Comparison tables that separate relevance from trust.
- Shortlists and RFQs for moving from discovery to action.

### Supplier Console

For suppliers, Supify is an onboarding and proof-building workflow.

- Progressive profile setup.
- Claim submission.
- Evidence uploads.
- Verification status tracking.
- Trust ladder showing what improves credibility.
- Document locker.
- Re-verification prompts before trust decays.

### Verifier Console

For verifiers, Supify is an evidence review cockpit.

- SLA-aware work queue.
- Claim review workspace.
- Evidence viewer.
- Decision panel with rationale.
- Escalation path.
- Supplier dossier.
- Fraud and duplicate signals.
- Accuracy metrics such as reversal rate.

## Architecture Vibes, But Serious

Supify is stack-agnostic by design. The architecture is described as capabilities and invariants first, tooling second.

The intended shape:

```text
Relational Core
  the source of truth: organisations, suppliers, claims, evidence, checks, attestations

Search Projection
  denormalized, disposable, rebuilt from the core

API / Contract Boundary
  versioned shapes, adapters, fixtures, error taxonomy

Frontend Entity Layer
  canonical rendering for suppliers, claims, evidence, and trust

Feature Consoles
  buyer, supplier, verifier projections over one ledger
```

The database does not store a supplier as a pile of boolean badges.

It stores a living record of assertions, evidence, checks, and platform positions over time.

## Frontend Doctrine

The interface exists to make uncertainty visible.

- Trust is never conveyed by color alone.
- Unknown is rendered as unknown.
- Expired is rendered as expired.
- Pending verification is rendered as pending, with owner and next step.
- Search filters live in the URL so discovery state is shareable.
- Long result sets and queues are virtualized.
- Skeletons match final layout.
- Optimistic UI is allowed for cheap reversals, never for verification decisions.
- Evidence viewing is in-app, zoomable, and keyboard navigable.

If the UI hides ambiguity, the product has failed.

## Build Plan

The build is phased so each stage proves part of the thesis.

| Phase | Delivers | Proof |
| --- | --- | --- |
| **P0** | Schema, claim registry, fixtures, contracts, primitives | A supplier can be represented as a real ledger |
| **P1** | Search, filters, profiles, trust rendering, comparison | A buyer can discover and compare suppliers |
| **P2** | Supplier onboarding, claims, evidence uploads | A supplier can submit proof |
| **P3** | Verifier queue, review, decisions, attestations | Evidence becomes platform-backed trust |
| **P4** | Expiry, decay, re-verification, notifications | Trust changes honestly over time |
| **P5** | Flags, duplicate detection, escalation, revocation | Bad signals can be investigated and corrected |
| **P6** | RFQs, invitations, quotes, awards | Discovery converts into commerce |
| **P7** | Outcome-derived reputation | Real transaction outcomes improve trust |

P0 through P3 are the core arc:

```text
unverified supplier claim
  -> evidence
  -> verification
  -> time-bounded public trust
```

That is the product.

## Non-Negotiables

- No opaque trust scores.
- No supplier-authored verification state.
- No permanent verification.
- No silent async queues.
- No claim types hardcoded across the product.
- No client-side trust computation.
- No profile state that implies approval by omission.
- No divergence between buyer, supplier, and verifier truth.

## Performance Targets

- Search first paint: under 1.5 seconds.
- Filter application: under 300 milliseconds.
- Profile trust panel: under 1 second.
- Verifier queue to workspace: under 500 milliseconds.
- Evidence document first render: under 2 seconds.

Discovery should feel instant. Verification should feel accountable.

## Open Strategic Bets

The architecture is ready for these decisions, but the product narrative should choose deliberately:

- India-first or multi-country?
- Horizontal marketplace or vertical-specific supplier network?
- Custom category tree or mapped external taxonomy?
- Transactional marketplace or lead-generation platform?
- Public supplier trust pages or gated buyer-only profiles?
- Platform verifiers or accredited third-party agencies?
- Free verification or supplier-paid verification?

## Why This Matters

Most supplier platforms help buyers find more options.

Supify is designed to help buyers find options they can actually believe.

That difference is the product.

## Full Rationale

This README is the showpiece. The deeper architectural reasoning lives in [docs/understanding.md](docs/understanding.md).
