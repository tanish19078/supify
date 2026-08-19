const bandMeta = {
  unverified: { icon: '○', label: 'Unverified' },
  basic: { icon: '◐', label: 'Basic' },
  verified: { icon: '●', label: 'Verified' },
  audited: { icon: '◆', label: 'Audited' },
}

export function TrustBand({ trust, compact = false }) {
  const meta = bandMeta[trust.band]
  return (
    <div className={`trust-band trust-band--${trust.band} ${compact ? 'trust-band--compact' : ''}`}>
      <span aria-hidden="true">{meta.icon}</span>
      <span>{meta.label}</span>
      {!compact && <small>{trust.methodLabel} · {trust.lastChecked}</small>}
    </div>
  )
}
