import { Link } from 'react-router-dom'

export function NotFound() {
  return (
    <main className="page">
      <div className="empty-state">
        <span aria-hidden="true">⊘</span>
        <h2>Page not found</h2>
        <p>This address does not match anything in Supify.</p>
        <Link className="button" to="/search">Go to discovery</Link>
      </div>
    </main>
  )
}
