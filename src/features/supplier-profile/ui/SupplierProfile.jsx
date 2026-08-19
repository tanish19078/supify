import { useEffect, useState } from 'react'
import { ClaimRow } from '../../../entities/claim/ui/ClaimRow'
import { TrustPanel } from '../../../entities/trust/ui/TrustPanel'
import { supplierRepository } from '../../../shared/api/supplierRepository'

export function SupplierProfile({ supplierId, onBack }) {
  const [supplier, setSupplier] = useState(null)

  useEffect(() => { supplierRepository.getById(supplierId).then(setSupplier) }, [supplierId])

  if (!supplier) return <main className="page"><div className="loading-card">Loading supplier record…</div></main>

  return (
    <main className="page profile-page">
      <button className="back-link" onClick={onBack}>← Back to search</button>
      <section className="profile-header">
        <div className="supplier-mark supplier-mark--large">{supplier.tradeName.slice(0, 1)}</div>
        <div className="profile-header__copy">
          <p className="eyebrow">{supplier.category} · {supplier.location}</p>
          <h1>{supplier.tradeName}</h1>
          <p>{supplier.legalName}</p>
        </div>
        <button className="button">Add to shortlist</button>
      </section>
      <section className="profile-grid">
        <div>
          <div className="facts-card">
            <div><span>Capacity</span><strong>{supplier.capacity}</strong></div>
            <div><span>Minimum order</span><strong>{supplier.moq}</strong></div>
            <div><span>Typical lead time</span><strong>{supplier.leadTime}</strong></div>
          </div>
          <section className="claims-section">
            <div className="section-heading"><div><p className="eyebrow">Claim ledger</p><h2>What this supplier has asserted</h2></div><span>Method, date, and evidence shown for each claim</span></div>
            {supplier.claims.map((claim) => <ClaimRow key={claim.key} claim={claim} />)}
          </section>
        </div>
        <aside><TrustPanel trust={supplier.trust} /></aside>
      </section>
    </main>
  )
}
