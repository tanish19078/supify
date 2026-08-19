import { TrustBand } from './TrustBand'

export function TrustPanel({ trust }) {
  return (
    <section className="trust-panel" aria-label="Trust breakdown">
      <div className="trust-panel__topline">
        <div>
          <p className="eyebrow">Trust standing</p>
          <TrustBand trust={trust} />
        </div>
        <span className="trust-panel__expiry">Valid until {trust.validUntil ? formatDate(trust.validUntil) : 're-verification'}</span>
      </div>
      <div className="pillar-list">
        {trust.pillars.map((pillar) => (
          <div className="pillar" key={pillar.name}>
            <div><span>{pillar.name}</span><strong>{pillar.score}% established</strong></div>
            <div className="pillar__track"><span style={{ width: `${pillar.score}%` }} /></div>
          </div>
        ))}
      </div>
      {trust.gatesApplied.length > 0 && (
        <div className="gate-notice"><strong>Trust gate applied</strong><span>{trust.gatesApplied.join(' · ')}</span></div>
      )}
    </section>
  )
}

function formatDate(value) {
  return new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(value))
}
