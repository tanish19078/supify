# Supplier data contract (database/API later)

The React app currently reads fixtures through `src/shared/api/supplierRepository.js`.
That module is the only place that should change when a real API and relational database are added.

## Planned reads

| Method | Purpose | Current implementation |
| --- | --- | --- |
| `search(filters)` | Search projection for the buyer discovery surface | Filters fixture records |
| `getById(supplierId)` | Supplier profile and claim ledger | Returns one fixture record |
| `getVerificationQueue()` | Cross-supplier verifier work queue | Returns fixture task rows |

The eventual database must retain the ledger described in `docs/understanding.md`: claims, evidence,
verification checks, and attestations are separate records. A trust snapshot is a server-computed,
rebuildable read projection; the React client only renders it.

## Replacement seam

Create an API-backed implementation of `SupplierRepository` and replace the exported instance in
`src/shared/api/supplierRepository.js`. Keep the wire-to-domain mapping in
`src/entities/supplier/model/adapter.js`; feature and entity components must continue to consume only
the mapped domain shape.
