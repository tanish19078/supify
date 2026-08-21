export class SupplierRepository {
  async search(_filters) {
    throw new Error('SupplierRepository.search must be implemented')
  }

  async getById(_supplierId) {
    throw new Error('SupplierRepository.getById must be implemented')
  }

  async getVerificationQueue() {
    throw new Error('SupplierRepository.getVerificationQueue must be implemented')
  }

  async getVerificationTask(_taskId) {
    throw new Error('SupplierRepository.getVerificationTask must be implemented')
  }
}
