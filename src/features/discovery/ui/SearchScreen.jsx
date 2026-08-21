import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SupplierCard } from '../../../entities/supplier/ui/SupplierCard'
import { supplierRepository } from '../../../shared/api/supplierRepository'

const bandOptions = ['unverified', 'basic', 'verified', 'audited']

export function SearchScreen() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [suppliers, setSuppliers] = useState(null)
  const [hasError, setHasError] = useState(false)
  const [retryToken, setRetryToken] = useState(0)
  const searchInputRef = useRef(null)

  const query = searchParams.get('q') || ''
  const band = searchParams.get('band') || ''

  useEffect(() => {
    searchInputRef.current.focus()
  }, [])

  useEffect(() => {
    let cancelled = false
    async function loadResults() {
      setHasError(false)
      setSuppliers(null)
      try {
        const results = await supplierRepository.search({ query, band })
        if (!cancelled) setSuppliers(results)
      } catch {
        if (!cancelled) setHasError(true)
      }
    }
    loadResults()
    return () => {
      cancelled = true
    }
  }, [query, band, retryToken])

  function updateParams(next) {
    const updated = new URLSearchParams(searchParams)
    Object.entries(next).forEach(([key, value]) => {
      if (value) updated.set(key, value)
      else updated.delete(key)
    })
    setSearchParams(updated, { replace: true })
  }

  return (
    <main className="page discovery-page">
      <section className="discovery-hero">
        <p className="eyebrow">Buyer console · Discovery</p>
        <h1>Find suppliers you can <em>actually</em> assess.</h1>
        <p>Search finds candidates. Evidence explains what has been established, how, and until when.</p>
        <div className="search-box">
          <label className="sr-only" htmlFor="supplier-search">Search suppliers</label>
          <input
            id="supplier-search"
            ref={searchInputRef}
            value={query}
            onChange={(event) => updateParams({ q: event.target.value })}
            placeholder="Search fasteners, machining, locations…"
          />
          <span aria-hidden="true">⌕</span>
        </div>
      </section>

      <section className="search-layout">
        <aside className="filter-panel">
          <div className="filter-panel__heading"><span>Filters</span><button onClick={() => updateParams({ band: '' })}>Clear</button></div>
          <fieldset>
            <legend>Trust standing</legend>
            {bandOptions.map((value) => (
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
          <div className="results-toolbar">
            <span>{suppliers ? `${suppliers.length} supplier${suppliers.length === 1 ? '' : 's'} found` : 'Searching…'}</span>
            <span>Trust details are always inspectable</span>
          </div>
          <div className="results-list">
            {hasError ? (
              <div className="empty-state">
                <span aria-hidden="true">⚠</span>
                <h2>Could not load suppliers</h2>
                <p>The request failed. Check your connection and try again.</p>
                <button className="button" onClick={() => setRetryToken((token) => token + 1)}>Try again</button>
              </div>
            ) : suppliers === null ? null : suppliers.length ? suppliers.map((supplier) => (
              <SupplierCard key={supplier.id} supplier={supplier} />
            )) : (
              <div className="empty-state"><span aria-hidden="true">⌕</span><h2>No suppliers match these filters</h2><p>Try removing a filter or using a broader category term.</p></div>
            )}
          </div>
        </div>
      </section>
    </main>
  )
}
