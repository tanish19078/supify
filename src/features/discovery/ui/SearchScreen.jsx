import { useEffect, useState } from 'react'
import { SupplierCard } from '../../../entities/supplier/ui/SupplierCard'
import { supplierRepository } from '../../../shared/api/supplierRepository'

export function SearchScreen({ onOpenSupplier }) {
  const [params, setParams] = useState(() => new URLSearchParams(window.location.search))
  const [suppliers, setSuppliers] = useState([])

  const query = params.get('q') || ''
  const band = params.get('band') || ''

  useEffect(() => {
    supplierRepository.search({ query, band }).then(setSuppliers)
  }, [query, band])

  function updateParams(next) {
    const updated = new URLSearchParams(params)
    Object.entries(next).forEach(([key, value]) => value ? updated.set(key, value) : updated.delete(key))
    const href = `/search${updated.toString() ? `?${updated}` : ''}`
    window.history.pushState({}, '', href)
    setParams(updated)
  }

  return (
    <main className="page discovery-page">
      <section className="discovery-hero">
        <p className="eyebrow">Buyer console · Discovery</p>
        <h1>Find suppliers you can <em>actually</em> assess.</h1>
        <p>Search finds candidates. Evidence explains what has been established, how, and until when.</p>
        <div className="search-box">
          <label className="sr-only" htmlFor="supplier-search">Search suppliers</label>
          <input id="supplier-search" value={query} onChange={(event) => updateParams({ q: event.target.value })} placeholder="Search fasteners, machining, locations…" />
          <span aria-hidden="true">⌕</span>
        </div>
      </section>

      <section className="search-layout">
        <aside className="filter-panel">
          <div className="filter-panel__heading"><span>Filters</span><button onClick={() => updateParams({ band: '' })}>Clear</button></div>
          <fieldset>
            <legend>Trust standing</legend>
            {['unverified', 'basic', 'verified', 'audited'].map((value) => (
              <label className="radio-row" key={value}>
                <input type="radio" name="band" checked={band === value} onChange={() => updateParams({ band: value })} />
                <span>{value}</span>
              </label>
            ))}
            <label className="radio-row"><input type="radio" name="band" checked={!band} onChange={() => updateParams({ band: '' })} /><span>All suppliers</span></label>
          </fieldset>
          <div className="filter-note"><strong>Visible ranking</strong><span>Relevance is currently prioritised. Trust is shown openly on every result.</span></div>
        </aside>

        <div className="results-area">
          <div className="results-toolbar"><span>{suppliers.length} supplier{suppliers.length === 1 ? '' : 's'} found</span><span>Trust details are always inspectable</span></div>
          <div className="results-list">
            {suppliers.length ? suppliers.map((supplier) => <SupplierCard key={supplier.id} supplier={supplier} onOpen={onOpenSupplier} />) : (
              <div className="empty-state"><span>⌕</span><h2>No suppliers match these filters</h2><p>Try removing a filter or using a broader category term.</p></div>
            )}
          </div>
        </div>
      </section>
    </main>
  )
}
