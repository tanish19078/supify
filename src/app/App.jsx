import { useContext } from 'react'
import { Link, Navigate, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { SearchScreen } from '../features/discovery/ui/SearchScreen'
import { SupplierProfile } from '../features/supplier-profile/ui/SupplierProfile'
import { OnboardingScreen } from '../features/supplier-onboarding/ui/OnboardingScreen'
import { OnboardingStep } from '../features/supplier-onboarding/ui/OnboardingStep'
import { VerificationQueue } from '../features/verification-review/ui/VerificationQueue'
import { VerificationTaskDetail } from '../features/verification-review/ui/VerificationTaskDetail'
import { ProtectedRoute } from './ProtectedRoute'
import { NotFound } from './NotFound'
import { UserRoleContext, roleLabels } from '../entities/user/model/UserRoleContext'

export default function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/" element={<Navigate to="/search" replace />} />
        <Route path="/search" element={<SearchScreen />} />
        <Route path="/suppliers/:supplierId" element={<SupplierProfile />} />
        <Route path="/supplier/onboarding" element={<ProtectedRoute requiredRole="supplier"><OnboardingScreen /></ProtectedRoute>}>
          <Route index element={<Navigate to="organisation" replace />} />
          <Route path=":step" element={<OnboardingStep />} />
        </Route>
        <Route path="/verify" element={<ProtectedRoute requiredRole="verifier"><Outlet /></ProtectedRoute>}>
          <Route index element={<Navigate to="queue" replace />} />
          <Route path="queue" element={<VerificationQueue />} />
          <Route path="tasks/:taskId" element={<VerificationTaskDetail />} />
        </Route>
        <Route path="*" element={<NotFound />} />
      </Routes>
    </>
  )
}

function Header() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const session = useContext(UserRoleContext)

  const active = pathname.startsWith('/verify')
    ? 'verifier'
    : pathname.startsWith('/supplier')
      ? 'supplier'
      : 'buyer'

  return (
    <header className="site-header">
      <Link className="brand" to="/search"><span aria-hidden="true">◈</span> supify</Link>
      <nav aria-label="Primary navigation">
        <button className={active === 'buyer' ? 'active' : ''} onClick={() => navigate('/search')}>Discover</button>
        <button className={active === 'supplier' ? 'active' : ''} onClick={() => navigate('/supplier/onboarding')}>Supplier workspace</button>
        <button className={active === 'verifier' ? 'active' : ''} onClick={() => navigate('/verify/queue')}>Verifier queue</button>
      </nav>
      <button
        className="profile-button"
        onClick={session.cycleRole}
        title={`Active role: ${roleLabels[session.role]} — click to switch`}
        aria-label={`Active role: ${roleLabels[session.role]}. Click to switch role.`}
      >
        {session.role.slice(0, 2).toUpperCase()}
      </button>
    </header>
  )
}
