import { SupplierRepository } from './contract'
import { supplierWires, verificationTaskWires } from './fixtures'
import { toSupplier } from '../../entities/supplier/model/adapter'

class FixtureSupplierRepository extends SupplierRepository {
  async search(filters = {}) {
    const query = (filters.query || '').trim().toLowerCase()
    const band = filters.band || ''
    const rows = supplierWires.filter((supplier) => {
      const haystack = `${supplier.legal_name} ${supplier.trade_name} ${supplier.category} ${supplier.location}`.toLowerCase()
      return (!query || haystack.includes(query)) && (!band || supplier.trust.band === band)
    })
    return rows.map(toSupplier)
  }

  async getById(supplierId) {
    const wire = supplierWires.find((supplier) => supplier.id === supplierId)
    return wire ? toSupplier(wire) : null
  }

  async getVerificationQueue() {
    return verificationTaskWires.map((task) => ({
      ...task,
      supplier: toSupplier(supplierWires.find((supplier) => supplier.id === task.supplier_id)),
    }))
  }
}

// Replace only this export when the database/API is ready.
export const supplierRepository = new FixtureSupplierRepository()
