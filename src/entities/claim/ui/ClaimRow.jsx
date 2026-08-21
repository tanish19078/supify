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

export function ClaimRow({ claim }) {
  const meta = stateMeta[claim.state] || { icon: '?', label: claim.state }
  const date = claim.verifiedAt
    ? `Established ${new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(claim.verifiedAt))}`
    : 'Not yet independently established'

  return (
    <article className={`claim-row claim-row--${claim.state}`}>
      <div className="claim-row__state" aria-label={claim.state}>{meta.icon}</div>
      <div className="claim-row__content">
        <h4>{claim.label}</h4>
        <p>{claim.value}</p>
        <span>{claim.methodLabel} · {date}{meta.label ? ` · ${meta.label}` : ''}</span>
      </div>
      <div className="claim-row__evidence">{claim.evidenceCount} evidence item{claim.evidenceCount === 1 ? '' : 's'}</div>
    </article>
  )
}
