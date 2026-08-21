import { useEffect, useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { onboardingSteps } from '../model/steps'

const DRAFT_KEY = 'supify-onboarding-draft'

const initialDraft = {
  legalName: 'Narmada Advanced Custom Fabrication and Integrated Industrial Solutions Private Limited',
  tradeName: 'Narmada Fabrication',
  location: 'Indore, Madhya Pradesh',
  website: '',
  category: 'Sheet-metal fabrication',
  capacity: '',
  moq: '',
  leadTime: '',
  gstNumber: '',
  qualityProcess: '',
  submittedAt: null,
}

function loadDraft() {
  try {
    const stored = localStorage.getItem(DRAFT_KEY)
    return stored ? { ...initialDraft, ...JSON.parse(stored) } : initialDraft
  } catch {
    return initialDraft
  }
}

export function OnboardingScreen() {
  const [draft, setDraft] = useState(loadDraft)

  useEffect(() => {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
  }, [draft])

  function updateField(name, value) {
    setDraft((current) => ({ ...current, [name]: value }))
  }

  return (
    <main className="page onboarding-page">
      <section className="onboarding-intro">
        <p className="eyebrow">Supplier console · Onboarding</p>
        <h1>Build a profile buyers can understand.</h1>
        <p>Your listing can be published before every claim is verified. We show what is established honestly.</p>
      </section>
      <section className="onboarding-shell">
        <aside className="onboarding-steps">
          {onboardingSteps.map((step) => (
            <NavLink
              key={step.key}
              to={step.key}
              className={({ isActive }) => `onboarding-step${isActive ? ' onboarding-step--active' : ''}`}
            >
              <b>{step.number}</b>
              <div>
                <strong>{step.label}</strong>
                <span>{step.detail}</span>
              </div>
            </NavLink>
          ))}
        </aside>
        <Outlet context={{ draft, updateField }} />
      </section>
    </main>
  )
}
