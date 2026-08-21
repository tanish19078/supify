import { SupplierRepository } from './contract'
import { toSupplier, toVerificationTask } from '../../entities/supplier/model/adapter'

const SUPPLIERS_URL = '/data/suppliers.json'
const TASKS_URL = '/data/verification-tasks.json'
const SIMULATED_LATENCY_MS = 450

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function fetchJson(url) {
  await delay(SIMULATED_LATENCY_MS)
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status} for ${url}`)
  }
  return response.json()
}

function findSupplierWire(supplierWires, supplierId) {
  return supplierWires.find((supplier) => supplier.id === supplierId)
}

class FixtureSupplierRepository extends SupplierRepository {
  async search(filters = {}) {
    const supplierWires = await fetchJson(SUPPLIERS_URL)
    const query = (filters.query || '').trim().toLowerCase()
    const band = filters.band || ''
    return supplierWires
      .filter((supplier) => {
        const haystack = `${supplier.legal_name} ${supplier.trade_name} ${supplier.category} ${supplier.location}`.toLowerCase()
        return (!query || haystack.includes(query)) && (!band || supplier.trust.band === band)
      })
      .map(toSupplier)
  }

  async getById(supplierId) {
    const supplierWires = await fetchJson(SUPPLIERS_URL)
    const wire = findSupplierWire(supplierWires, supplierId)
    return wire ? toSupplier(wire) : null
  }

  async getVerificationQueue() {
    const [taskWires, supplierWires] = await Promise.all([
      fetchJson(TASKS_URL),
      fetchJson(SUPPLIERS_URL),
    ])
    return taskWires.map((taskWire) =>
      toVerificationTask(taskWire, findSupplierWire(supplierWires, taskWire.supplier_id)),
    )
  }

  async getVerificationTask(taskId) {
    const [taskWires, supplierWires] = await Promise.all([
      fetchJson(TASKS_URL),
      fetchJson(SUPPLIERS_URL),
    ])
    const taskWire = taskWires.find((task) => task.id === taskId)
    return taskWire ? toVerificationTask(taskWire, findSupplierWire(supplierWires, taskWire.supplier_id)) : null
  }
}

export const supplierRepository = new FixtureSupplierRepository()
