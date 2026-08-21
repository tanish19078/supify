import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { TrustBand } from '../../../entities/trust/ui/TrustBand'
import { supplierRepository } from '../../../shared/api/supplierRepository'

export function VerificationQueue() {
  const [tasks, setTasks] = useState(null)
  const [hasError, setHasError] = useState(false)
  const [retryToken, setRetryToken] = useState(0)

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
  const dueToday = taskList.filter((task) => task.sla.startsWith('Due')).length

  return (
    <main className="page verifier-page">
      <header className="verifier-header">
        <div><p className="eyebrow">Verifier console · Queue</p><h1>Evidence review queue</h1></div>
        <div className="queue-summary">
          <span><strong>{taskList.length}</strong> open tasks</span>
          <span><strong>{dueToday}</strong> due today</span>
        </div>
      </header>
      <section className="queue-card">
        <div className="queue-head"><span>Supplier & claim</span><span>Trust context</span><span>Task state</span><span>Ownership & SLA</span><span /></div>
        {taskList.map((task) => (
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
