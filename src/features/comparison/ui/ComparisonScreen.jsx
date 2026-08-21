import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { TrustBand } from '../../../entities/trust/ui/TrustBand'
import { supplierRepository } from '../../../shared/api/supplierRepository'

function SupplierPick({ supplier, checked, onToggle }) {
  return (
    <label className="radio-row">
      <input type="checkbox" checked={checked} onChange={() => onToggle(supplier.id)} />
      <span>{supplier.tradeName}</span>
    </label>
  )
}

export function ComparisonScreen() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [suppliers, setSuppliers] = useState(null)
  const [hasError, setHasError] = useState(false)
  const [retryToken, setRetryToken] = useState(0)

  useEffect(() => {
    let cancelled = false
    async function loadSuppliers() {
      setHasError(false)
      try {
        const results = await supplierRepository.search({})
        if (!cancelled) setSuppliers(results)
      } catch {
        if (!cancelled) setHasError(true)
      }
    }
    loadSuppliers()
    return () => {
      cancelled = true
    }
  }, [retryToken])

  const idsParam = searchParams.get('ids') || ''

  const selectedIds = useMemo(
    () => new Set(idsParam.split(',').filter(Boolean)),
    [idsParam],
  )

  const selected = useMemo(
    () => (suppliers || []).filter((supplier) => selectedIds.has(supplier.id)),
    [suppliers, selectedIds],
  )

  const toggleId = useCallback((supplierId) => {
    const next = new Set(selectedIds)
    if (next.has(supplierId)) next.delete(supplierId)
    else next.add(supplierId)
    const updated = new URLSearchParams(searchParams)
    if (next.size > 0) updated.set('ids', [...next].join(','))
    else updated.delete('ids')
    setSearchParams(updated)
  }, [searchParams, selectedIds, setSearchParams])

  function retry() {
    setRetryToken((token) => token + 1)
  }

  if (hasError) {
    return (
      <main className="page">
        <div className="empty-state">
          <span aria-hidden="true">⚠</span>
          <h2>Could not load suppliers</h2>
          <p>The request failed. Check your connection and try again.</p>
          <button className="button" onClick={retry}>Try again</button>
        </div>
      </main>
    )
  }

  const tableColumns = { gridTemplateColumns: `150px repeat(${selected.length || 1}, minmax(150px, 1fr))` }
  const rows = [
    { label: 'Trust band', render: (s) => <TrustBand trust={s.trust} compact /> },
    { label: 'Location', render: (s) => s.location },
    { label: 'Category', render: (s) => s.category },
    { label: 'Capacity', render: (s) => s.capacity },
    { label: 'Minimum order', render: (s) => s.moq },
    { label: 'Lead time', render: (s) => s.leadTime },
    { label: 'Established claims', render: (s) => `${s.claims.filter((c) => c.state === 'verified').length} of ${s.claims.length}` },
  ]

  return (
    <main className="page discovery-page">
      <section className="discovery-hero">
        <p className="eyebrow">Buyer console · Comparison</p>
        <h1>Compare candidates <em>side by side</em>.</h1>
        <p>Pick suppliers on the left. The comparison set lives in the URL, so you can send it to a colleague.</p>
      </section>

      <section className="search-layout">
        <aside className="filter-panel">
          <div className="filter-panel__heading"><span>Suppliers</span><span>{selected.length} selected</span></div>
          <fieldset>
            <legend>Toggle candidates</legend>
            {(suppliers || []).map((supplier) => (
              <SupplierPick key={supplier.id} supplier={supplier} checked={selectedIds.has(supplier.id)} onToggle={toggleId} />
            ))}
            {suppliers === null && <p className="muted">Loading…</p>}
          </fieldset>
        </aside>

        <div className="results-area">
          {selected.length === 0 ? (
            <div className="empty-state">
              <span aria-hidden="true">⇄</span>
              <h2>Nothing selected yet</h2>
              <p>Choose two or three suppliers from the list to compare them here.</p>
            </div>
          ) : (
            <section className="queue-card">
              <div className="queue-head" style={tableColumns}>
                <span>Field</span>
                {selected.map((supplier) => <span key={supplier.id}>{supplier.tradeName}</span>)}
              </div>
              {rows.map((row) => (
                <div className="queue-row" style={tableColumns} key={row.label}>
                  <div><strong>{row.label}</strong></div>
                  {selected.map((supplier) => <div key={supplier.id}>{row.render(supplier)}</div>)}
                </div>
              ))}
            </section>
          )}
        </div>
      </section>
    </main>
  )
}
