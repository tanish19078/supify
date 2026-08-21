import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ClaimRow } from '../../../entities/claim/ui/ClaimRow'
import { TrustBand } from '../../../entities/trust/ui/TrustBand'
import { supplierRepository } from '../../../shared/api/supplierRepository'

const decisions = [
  { key: 'needs_more_info', label: 'Request more info', copy: 'The supplier has been asked for additional evidence.' },
  { key: 'rejected', label: 'Reject claim', copy: 'The claim has been marked as not established.' },
  { key: 'attested', label: 'Attest', copy: 'An attestation has been proposed from the reviewed evidence.' },
]

export function VerificationTaskDetail() {
  const { taskId } = useParams()
  const [task, setTask] = useState(null)
  const [status, setStatus] = useState('loading')
  const [retryToken, setRetryToken] = useState(0)
  const [decision, setDecision] = useState(null)

  useEffect(() => {
    let cancelled = false
    async function loadTask() {
      setStatus('loading')
      setDecision(null)
      try {
        const result = await supplierRepository.getVerificationTask(taskId)
        if (cancelled) return
        setTask(result)
        setStatus(result ? 'ready' : 'missing')
      } catch {
        if (!cancelled) setStatus('error')
      }
    }
    loadTask()
    return () => {
      cancelled = true
    }
  }, [taskId, retryToken])

  if (status === 'loading') {
    return <main className="page verifier-page"><div className="loading-card">Loading review task…</div></main>
  }

  if (status === 'error') {
    return (
      <main className="page verifier-page">
        <div className="empty-state">
          <span aria-hidden="true">⚠</span>
          <h2>Could not load this review task</h2>
          <p>The request failed. Check your connection and try again.</p>
          <button className="button" onClick={() => setRetryToken((token) => token + 1)}>Try again</button>
        </div>
      </main>
    )
  }

  if (status === 'missing') {
    return (
      <main className="page verifier-page">
        <div className="empty-state">
          <span aria-hidden="true">⊘</span>
          <h2>No such review task</h2>
          <p>The task may have been completed or reassigned.</p>
          <Link className="button" to="/verify/queue">Back to queue</Link>
        </div>
      </main>
    )
  }

  const claim = task.supplier.claims.find((entry) => entry.key === task.claimKey)
  const chosen = decisions.find((option) => option.key === decision)

  return (
    <main className="page verifier-page">
      <Link className="back-link" to="/verify/queue">← Back to queue</Link>
      <section className="profile-header">
        <div className="supplier-mark supplier-mark--large">{task.supplier.tradeName.slice(0, 1)}</div>
        <div className="profile-header__copy">
          <p className="eyebrow">{task.id} · {task.supplier.category}</p>
          <h1>{task.claimLabel}</h1>
          <p>{task.supplier.legalName}</p>
        </div>
        <TrustBand trust={task.supplier.trust} compact />
      </section>

      <section className="profile-grid">
        <div>
          <div className="facts-card">
            <div><span>Assigned to</span><strong>{task.assignedTo}</strong></div>
            <div><span>Priority</span><strong>{task.priority}</strong></div>
            <div><span>SLA</span><strong>{task.sla}</strong></div>
          </div>
          <section className="claims-section">
            <div className="section-heading"><div><p className="eyebrow">Claim under review</p><h2>What the supplier asserts</h2></div><span>Your decision must rest on the attached evidence, not the assertion</span></div>
            {claim && <ClaimRow claim={claim} />}
          </section>
        </div>
        <aside>
          {chosen ? (
            <section className="verifier-note" aria-live="polite">
              <strong>Decision recorded</strong>
              <span>{chosen.copy} This task now waits for the platform to confirm the append-only ledger entry before any attestation changes.</span>
            </section>
          ) : (
            <section aria-label="Review decision">
              <p className="eyebrow">Reviewer decision</p>
              <div className="pillar-list">
                {decisions.map((option) => (
                  <button
                    key={option.key}
                    className={option.key === 'attested' ? 'button' : 'button button--quiet'}
                    onClick={() => setDecision(option.key)}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
              <div className="verifier-note"><strong>No optimistic attestations</strong><span>A decision is a proposal. The ledger confirmation is what changes trust state.</span></div>
            </section>
          )}
        </aside>
      </section>
    </main>
  )
}
