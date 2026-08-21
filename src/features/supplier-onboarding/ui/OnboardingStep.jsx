import { Navigate, useNavigate, useOutletContext, useParams } from 'react-router-dom'
import { onboardingSteps } from '../model/steps'

export function OnboardingStep() {
  const { step } = useParams()
  const navigate = useNavigate()
  const { draft, updateField } = useOutletContext()

  const index = onboardingSteps.findIndex((entry) => entry.key === step)
  if (index === -1) return <Navigate to="/supplier/onboarding" replace />

  const current = onboardingSteps[index]
  const next = onboardingSteps[index + 1]

  function field(name, label, placeholder) {
    return (
      <label>
        {label}
        <input
          value={draft[name]}
          placeholder={placeholder}
          onChange={(event) => updateField(name, event.target.value)}
        />
      </label>
    )
  }

  function submitListing() {
    updateField('submittedAt', new Date().toISOString())
  }

  if (draft.submittedAt) {
    return (
      <form className="onboarding-form" onSubmit={(event) => event.preventDefault()}>
        <div className="form-heading">
          <p className="eyebrow">Listing submitted</p>
          <h2>Your listing is now in review</h2>
          <p>A trained verifier reviews every claim against its evidence before the platform attests anything. Until then, buyers see your claims marked as self-declared.</p>
        </div>
        <div className="form-callout">
          <strong>What happens next</strong>
          <span>Your claims have entered the verification queue. You can keep editing this draft — resubmitting replaces the queued version.</span>
        </div>
        <div className="form-actions">
          <span>Draft saved locally</span>
          <button className="button" onClick={() => updateField('submittedAt', null)}>Edit listing</button>
        </div>
      </form>
    )
  }

  return (
    <form className="onboarding-form" onSubmit={(event) => event.preventDefault()}>
      <div className="form-heading">
        <p className="eyebrow">Step {current.number} of {onboardingSteps.length}</p>
        <h2>{current.heading}</h2>
        <p>{current.blurb}</p>
      </div>

      {step === 'organisation' && (
        <>
          {field('legalName', 'Legal organisation name')}
          {field('tradeName', 'Trading name')}
          <div className="form-grid">
            {field('location', 'Primary operating location')}
            {field('website', 'Company website', 'https://')}
          </div>
          <div className="form-callout">
            <strong>Why we ask</strong>
            <span>Buyers need a traceable legal identity before they can evaluate any capability claim. You can save this step and return any time.</span>
          </div>
        </>
      )}

      {step === 'capabilities' && (
        <>
          {field('category', 'Primary category')}
          <div className="form-grid">
            {field('capacity', 'Monthly capacity')}
            {field('moq', 'Minimum order quantity')}
          </div>
          {field('leadTime', 'Typical lead time')}
        </>
      )}

      {step === 'claims' && (
        <>
          {field('gstNumber', 'GST registration number')}
          {field('qualityProcess', 'Quality management process')}
          <div className="form-callout">
            <strong>Assertion, not verification</strong>
            <span>These entries start as self-declared. They become established only after a verifier reviews your evidence and the platform attests.</span>
          </div>
        </>
      )}

      {step === 'review' && (
        <>
          <section className="claims-section">
            <div className="facts-card">
              <div><span>Location</span><strong>{draft.location || 'Not provided yet'}</strong></div>
              <div><span>Category</span><strong>{draft.category || 'Not provided yet'}</strong></div>
              <div><span>Lead time</span><strong>{draft.leadTime || 'Not provided yet'}</strong></div>
            </div>
          </section>
          <section className="claims-section">
            <div className="facts-card">
              <div><span>Capacity</span><strong>{draft.capacity || 'Not provided yet'}</strong></div>
              <div><span>Minimum order</span><strong>{draft.moq || 'Not provided yet'}</strong></div>
              <div><span>GST number</span><strong>{draft.gstNumber || 'Not provided yet'}</strong></div>
            </div>
          </section>
          <section className="claims-section">
            <div className="facts-card">
              <div><span>Quality management process</span><strong>{draft.qualityProcess || 'Not provided yet'}</strong></div>
            </div>
          </section>
        </>
      )}

      <div className="form-actions">
        <span>Draft saved locally</span>
        {next ? (
          <button className="button" onClick={() => navigate(`/supplier/onboarding/${next.key}`)}>Save and continue →</button>
        ) : (
          <button className="button" onClick={submitListing}>Submit listing</button>
        )}
      </div>
    </form>
  )
}
