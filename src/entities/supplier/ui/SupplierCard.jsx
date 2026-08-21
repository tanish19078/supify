import { Link } from 'react-router-dom'
import { TrustBand } from '../../trust/ui/TrustBand'

export function SupplierCard({ supplier }) {
  return (
    <article className="supplier-card">
      <div className="supplier-card__header">
        <div className="supplier-mark">{supplier.tradeName.slice(0, 1)}</div>
        <div>
          <p className="eyebrow">{supplier.category}</p>
          <h3>{supplier.tradeName}</h3>
          <p className="muted">{supplier.location}</p>
        </div>
        <TrustBand trust={supplier.trust} compact />
      </div>
      <div className="supplier-card__facts">
        <span><strong>Capacity</strong>{supplier.capacity}</span>
        <span><strong>MOQ</strong>{supplier.moq}</span>
        <span><strong>Lead time</strong>{supplier.leadTime}</span>
      </div>
      <div className="supplier-card__footer">
        <span>{supplier.claims.filter((claim) => claim.state === 'verified').length} independently established claims</span>
        <Link className="button button--quiet" to={`/suppliers/${supplier.id}`}>View profile <span aria-hidden="true">→</span></Link>
      </div>
    </article>
  )
}
