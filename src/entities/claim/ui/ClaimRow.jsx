export function ClaimRow({ claim }) {
  const date = claim.verifiedAt
    ? `Established ${new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(claim.verifiedAt))}`
    : 'Not yet independently established'

  return (
    <article className={`claim-row claim-row--${claim.state}`}>
      <div className="claim-row__state" aria-label={claim.state}>{claim.state === 'verified' ? '✓' : claim.state === 'under_review' ? '…' : '○'}</div>
      <div className="claim-row__content">
        <h4>{claim.label}</h4>
        <p>{claim.value}</p>
        <span>{claim.methodLabel} · {date}</span>
      </div>
      <div className="claim-row__evidence">{claim.evidenceCount} evidence item{claim.evidenceCount === 1 ? '' : 's'}</div>
    </article>
  )
}
