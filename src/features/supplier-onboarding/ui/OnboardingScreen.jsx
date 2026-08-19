const steps = [
  ['1', 'Organisation', 'Your core company profile'],
  ['2', 'Capabilities', 'What buyers can discover'],
  ['3', 'Claims & evidence', 'What you can establish'],
  ['4', 'Review', 'Submit your listing'],
]

export function OnboardingScreen() {
  return (
    <main className="page onboarding-page">
      <section className="onboarding-intro"><p className="eyebrow">Supplier console · Onboarding</p><h1>Build a profile buyers can understand.</h1><p>Your listing can be published before every claim is verified. We show what is established honestly.</p></section>
      <section className="onboarding-shell">
        <aside className="onboarding-steps">{steps.map(([number, label, detail], index) => <div className={`onboarding-step ${index === 0 ? 'onboarding-step--active' : ''}`} key={number}><b>{number}</b><div><strong>{label}</strong><span>{detail}</span></div></div>)}</aside>
        <form className="onboarding-form" onSubmit={(event) => event.preventDefault()}>
          <div className="form-heading"><p className="eyebrow">Step 1 of 4</p><h2>Tell buyers who you are</h2><p>These details establish the foundation of your public supplier profile.</p></div>
          <label>Legal organisation name<input defaultValue="Narmada Advanced Custom Fabrication and Integrated Industrial Solutions Private Limited" /></label>
          <label>Trading name<input defaultValue="Narmada Fabrication" /></label>
          <div className="form-grid"><label>Primary operating location<input defaultValue="Indore, Madhya Pradesh" /></label><label>Company website<input placeholder="https://" /></label></div>
          <div className="form-callout"><strong>Why we ask</strong><span>Buyers need a traceable legal identity before they can evaluate any capability claim. You can save this step and return any time.</span></div>
          <div className="form-actions"><span>Draft saved locally</span><button className="button">Save and continue →</button></div>
        </form>
      </section>
    </main>
  )
}
