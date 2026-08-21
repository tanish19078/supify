import { useEffect, useState } from 'react'
import { Link, useSearchParams, useParams } from 'react-router-dom'
import { ClaimRow } from '../../../entities/claim/ui/ClaimRow'
import { TrustPanel } from '../../../entities/trust/ui/TrustPanel'
import { supplierRepository } from '../../../shared/api/supplierRepository'

const SHORTLIST_KEY = 'supify-shortlist'

function readShortlist() {
  try {
    return JSON.parse(localStorage.getItem(SHORTLIST_KEY)) || []
  } catch {
    return []
  }
}

export function SupplierProfile() {
  const { supplierId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const [supplier, setSupplier] = useState(null)
  const [status, setStatus] = useState('loading')
  const [retryToken, setRetryToken] = useState(0)
  const [shortlist, setShortlist] = useState(readShortlist)

  const activeClaimKey = searchParams.get('claim')

  function toggleClaim(claimKey) {
    const updated = new URLSearchParams(searchParams)
    if (activeClaimKey === claimKey) updated.delete('claim')
    else updated.set('claim', claimKey)
    setSearchParams(updated)
  }

  useEffect(() => {
    let cancelled = false
    async function loadSupplier() {
      setStatus('loading')
      try {
        const result = await supplierRepository.getById(supplierId)
        if (cancelled) return
        setSupplier(result)
        setStatus(result ? 'ready' : 'missing')
      } catch {
        if (!cancelled) setStatus('error')
      }
    }
    loadSupplier()
    return () => {
      cancelled = true
    }
  }, [supplierId, retryToken])

  useEffect(() => {
    localStorage.setItem(SHORTLIST_KEY, JSON.stringify(shortlist))
  }, [shortlist])

  if (status === 'loading') {
    return <main className="page"><div className="loading-card">Loading supplier record…</div></main>
  }

  if (status === 'error') {
    return (
      <main className="page">
        <div className="empty-state">
          <span aria-hidden="true">⚠</span>
          <h2>Could not load this supplier record</h2>
          <p>The request failed. Check your connection and try again.</p>
          <button className="button" onClick={() => setRetryToken((token) => token + 1)}>Try again</button>
        </div>
      </main>
    )
  }

  if (status === 'missing') {
    return (
      <main className="page">
        <div className="empty-state">
          <span aria-hidden="true">⌕</span>
          <h2>No supplier record at this address</h2>
          <p>The supplier may be unlisted, or the link is out of date.</p>
          <Link className="button" to="/search">Back to discovery</Link>
        </div>
      </main>
    )
  }

  const isShortlisted = shortlist.includes(supplier.id)

  function toggleShortlist() {
    setShortlist((current) =>
      isShortlisted
        ? current.filter((id) => id !== supplier.id)
        : [...current, supplier.id],
    )
  }

  return (
    <main className="page profile-page">
      <Link className="back-link" to="/search">← Back to search</Link>
      <section className="profile-header">
        <div className="supplier-mark supplier-mark--large">{supplier.tradeName.slice(0, 1)}</div>
        <div className="profile-header__copy">
          <p className="eyebrow">{supplier.category} · {supplier.location}</p>
          <h1>{supplier.tradeName}</h1>
          <p>{supplier.legalName}</p>
        </div>
        <button className="button" onClick={toggleShortlist}>
          {isShortlisted ? 'On shortlist ✓' : 'Add to shortlist'}
        </button>
      </section>
      <section className="profile-grid">
        <div>
          <div className="facts-card">
            <div><span>Capacity</span><strong>{supplier.capacity}</strong></div>
            <div><span>Minimum order</span><strong>{supplier.moq}</strong></div>
            <div><span>Typical lead time</span><strong>{supplier.leadTime}</strong></div>
          </div>
          {shortlist.length > 0 && (
            <Link className="button button--quiet" to={`/compare?ids=${shortlist.join(',')}`}>
              Compare shortlist ({shortlist.length}) →
            </Link>
          )}
          <section className="claims-section">
            <div className="section-heading"><div><p className="eyebrow">Claim ledger</p><h2>What this supplier has asserted</h2></div><span>Method, date, and evidence shown for each claim</span></div>
            {supplier.claims.map((claim) => (
              <ClaimRow
                key={claim.key}
                claim={claim}
                isOpen={activeClaimKey === claim.key}
                onToggle={() => toggleClaim(claim.key)}
              />
            ))}
          </section>
        </div>
        <aside><TrustPanel trust={supplier.trust} /></aside>
      </section>
    </main>
  )
}
