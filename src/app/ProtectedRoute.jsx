import { useContext } from 'react'
import { UserRoleContext, roleLabels } from '../entities/user/model/UserRoleContext'

export function ProtectedRoute({ requiredRole, children }) {
  const session = useContext(UserRoleContext)

  if (session.role === requiredRole) return children

  return (
    <main className="page">
      <div className="empty-state">
        <span aria-hidden="true">⊘</span>
        <h2>{roleLabels[requiredRole]} access required</h2>
        <p>You are currently acting as {roleLabels[session.role]}. Switch your active role to open this console.</p>
        <button className="button" onClick={() => session.setRole(requiredRole)}>
          Continue as {roleLabels[requiredRole]}
        </button>
      </div>
    </main>
  )
}
