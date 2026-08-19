import { useEffect, useState } from 'react'
import { SearchScreen } from '../features/discovery/ui/SearchScreen'
import { SupplierProfile } from '../features/supplier-profile/ui/SupplierProfile'
import { OnboardingScreen } from '../features/supplier-onboarding/ui/OnboardingScreen'
import { VerificationQueue } from '../features/verification-review/ui/VerificationQueue'

function getPath() { return window.location.pathname }

function navigate(path) {
  window.history.pushState({}, '', path)
  window.dispatchEvent(new PopStateEvent('popstate'))
}

export default function App() {
  const [path, setPath] = useState(getPath)
  useEffect(() => {
    const sync = () => setPath(getPath())
    window.addEventListener('popstate', sync)
    return () => window.removeEventListener('popstate', sync)
  }, [])

  let content
  if (path.startsWith('/suppliers/')) content = <SupplierProfile supplierId={path.split('/')[2]} onBack={() => navigate('/search')} />
  else if (path.startsWith('/supplier/onboarding')) content = <OnboardingScreen />
  else if (path.startsWith('/verify')) content = <VerificationQueue />
  else content = <SearchScreen onOpenSupplier={(id) => navigate(`/suppliers/${id}`)} />

  return <><Header path={path} />{content}</>
}

function Header({ path }) {
  const active = path.startsWith('/verify') ? 'verifier' : path.startsWith('/supplier/') ? 'supplier' : 'buyer'
  return (
    <header className="site-header">
      <button className="brand" onClick={() => navigate('/search')}><span>◈</span> supify</button>
      <nav aria-label="Primary navigation">
        <button className={active === 'buyer' ? 'active' : ''} onClick={() => navigate('/search')}>Discover</button>
        <button className={active === 'supplier' ? 'active' : ''} onClick={() => navigate('/supplier/onboarding/organisation')}>Supplier workspace</button>
        <button className={active === 'verifier' ? 'active' : ''} onClick={() => navigate('/verify/queue')}>Verifier queue</button>
      </nav>
      <button className="profile-button" aria-label="Open account menu">VK</button>
    </header>
  )
}
