import { useEffect, useState } from 'react'
import { TrustBand } from '../../../entities/trust/ui/TrustBand'
import { supplierRepository } from '../../../shared/api/supplierRepository'

export function VerificationQueue() {
  const [tasks, setTasks] = useState([])
  useEffect(() => { supplierRepository.getVerificationQueue().then(setTasks) }, [])

  return (
    <main className="page verifier-page">
      <header className="verifier-header"><div><p className="eyebrow">Verifier console · Queue</p><h1>Evidence review queue</h1></div><div className="queue-summary"><span><strong>{tasks.length}</strong> open tasks</span><span><strong>1</strong> due today</span></div></header>
      <section className="queue-card">
        <div className="queue-head"><span>Supplier & claim</span><span>Trust context</span><span>Task state</span><span>Ownership & SLA</span><span /></div>
        {tasks.map((task) => <article className="queue-row" key={task.id}>
          <div><strong>{task.supplier.tradeName}</strong><span>{task.claimLabel}</span><small>{task.id} · {task.evidence_count || 0} evidence item{task.evidence_count === 1 ? '' : 's'}</small></div>
          <TrustBand trust={task.supplier.trust} compact />
          <div><span className={`status status--${task.state}`}>{task.state.replace('_', ' ')}</span><small>Priority: {task.priority}</small></div>
          <div><strong>{task.assigned_to}</strong><span>{task.sla}</span></div>
          <button className="button button--small">Open review</button>
        </article>)}
      </section>
      <section className="verifier-note"><strong>Decision safeguard</strong><span>Attestations are never issued optimistically. A submitted decision remains pending until the platform confirms the append-only ledger entry.</span></section>
    </main>
  )
}
