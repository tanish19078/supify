import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { TrustBand } from '../../../entities/trust/ui/TrustBand'
import { supplierRepository } from '../../../shared/api/supplierRepository'

const stateFilters = ['queued', 'in_review', 'awaiting_supplier', 'escalated']

export function VerificationQueue() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [tasks, setTasks] = useState(null)
  const [hasError, setHasError] = useState(false)
  const [retryToken, setRetryToken] = useState(0)

  const stateFilter = searchParams.get('state') || ''

  function updateStateFilter(next) {
    const updated = new URLSearchParams(searchParams)
    if (next) updated.set('state', next)
    else updated.delete('state')
    setSearchParams(updated, { replace: true })
  }

  useEffect(() => {
    let cancelled = false
    async function loadTasks() {
      setHasError(false)
      try {
        const result = await supplierRepository.getVerificationQueue()
        if (!cancelled) setTasks(result)
      } catch {
        if (!cancelled) setHasError(true)
      }
    }
    loadTasks()
    return () => {
      cancelled = true
    }
  }, [retryToken])

  if (hasError) {
    return (
      <main className="page verifier-page">
        <div className="empty-state">
          <span aria-hidden="true">⚠</span>
          <h2>Could not load the queue</h2>
          <p>The request failed. Check your connection and try again.</p>
          <button className="button" onClick={() => setRetryToken((token) => token + 1)}>Try again</button>
        </div>
      </main>
    )
  }

  const taskList = tasks || []
  const visibleTasks = stateFilter
    ? taskList.filter((task) => task.state === stateFilter)
    : taskList
  const dueToday = taskList.filter((task) => task.sla.startsWith('Due')).length

  return (
    <main className="page verifier-page">
      <header className="verifier-header">
        <div><p className="eyebrow">Verifier console · Queue</p><h1>Evidence review queue</h1></div>
        <div className="queue-summary">
          <span><strong>{visibleTasks.length}</strong> shown</span>
          <span><strong>{dueToday}</strong> due today</span>
        </div>
      </header>
      <div className="queue-filter" role="group" aria-label="Filter by task state">
        <button className={`status${stateFilter === '' ? ' active' : ''}`} onClick={() => updateStateFilter('')}>All</button>
        {stateFilters.map((value) => (
          <button key={value} className={`status status--${value}${stateFilter === value ? ' active' : ''}`} onClick={() => updateStateFilter(value)}>
            {value.replace('_', ' ')}
          </button>
        ))}
      </div>
      <section className="queue-card">
        <div className="queue-head"><span>Supplier & claim</span><span>Trust context</span><span>Task state</span><span>Ownership & SLA</span><span /></div>
        {visibleTasks.length === 0 && (
          <div className="queue-row"><div><strong>No tasks in this state</strong><span>Clear the filter to see the full queue.</span></div></div>
        )}
        {visibleTasks.map((task) => (
          <article className="queue-row" key={task.id}>
            <div><strong>{task.supplier.tradeName}</strong><span>{task.claimLabel}</span><small>{task.id} · {task.evidenceCount} evidence item{task.evidenceCount === 1 ? '' : 's'}</small></div>
            <TrustBand trust={task.supplier.trust} compact />
            <div><span className={`status status--${task.state}`}>{task.state.replace('_', ' ')}</span><small>Priority: {task.priority}</small>{task.requiresDual && <small>Dual control required</small>}</div>
            <div><strong>{task.assignedTo}</strong><span>{task.sla}</span></div>
            <Link className="button button--small" to={`/verify/tasks/${task.id}`}>Open review</Link>
          </article>
        ))}
      </section>
      <section className="verifier-note"><strong>Decision safeguard</strong><span>Attestations are never issued optimistically. A submitted decision remains pending until the platform confirms the append-only ledger entry.</span></section>
    </main>
  )
}
