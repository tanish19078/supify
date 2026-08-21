const stateMeta = {
  draft: { icon: '○' },
  submitted: { icon: '○' },
  under_review: { icon: '…' },
  needs_more_info: { icon: '?' },
  verified: { icon: '✓' },
  expiring_soon: { icon: '✓' },
  expired: { icon: '⌛', label: 'Expired' },
  rejected: { icon: '✕', label: 'Rejected' },
  revoked: { icon: '✕', label: 'Revoked' },
}

function formatDate(value) {
  return new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(value))
}

export function ClaimRow({ claim, isOpen = false, onToggle = null }) {
  const meta = stateMeta[claim.state] || { icon: '?', label: claim.state }
  const date = claim.verifiedAt
    ? `Established ${formatDate(claim.verifiedAt)}`
    : 'Not yet independently established'
  const validity = claim.validUntil
    ? `Attestation ${new Date(claim.validUntil).getTime() < Date.now() ? 'lapsed' : 'valid until'} ${formatDate(claim.validUntil)}`
    : null

  return (
    <article className={`claim-row claim-row--${claim.state}`}>
      <div className="claim-row__state" aria-label={claim.state}>{meta.icon}</div>
      <div className="claim-row__content">
        <h4>{claim.label}</h4>
        <p>{claim.value}</p>
        <span>{claim.methodLabel} · {date}{meta.label ? ` · ${meta.label}` : ''}</span>
        {isOpen && (
          <div>
            <p><strong>Evidence on record</strong></p>
            {claim.evidence.length === 0 && <p>No evidence attached yet</p>}
            {claim.evidence.map((item) => (
              <p key={item.label}>{item.label} · {item.kindLabel} · issued {formatDate(item.issuedOn)}</p>
            ))}
            {validity && <p>{validity}</p>}
          </div>
        )}
      </div>
      <div className="claim-row__evidence">
        {claim.evidenceCount} evidence item{claim.evidenceCount === 1 ? '' : 's'}
        {onToggle && (
          <button className="button button--quiet" onClick={onToggle} aria-expanded={isOpen}>
            {isOpen ? 'Hide' : 'Inspect'}
          </button>
        )}
      </div>
    </article>
  )
}
