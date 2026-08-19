/**
 * This file is the frontend contract boundary. Its wire shapes intentionally
 * mirror a future API/database response; UI code must use entity domain shapes.
 */

/** @typedef {'unverified'|'basic'|'verified'|'audited'} TrustBand */
/** @typedef {'self_declared'|'document_review'|'third_party_api'|'human_audit'|'site_visit'} Method */
/** @typedef {'draft'|'submitted'|'under_review'|'needs_more_info'|'verified'|'expiring_soon'|'expired'|'rejected'|'revoked'} ClaimState */

/** @typedef {{ id: string, legal_name: string, trade_name?: string, location: string, category: string, capacity: string, moq: string, lead_time: string, listing_state: string, trust: object, claims: object[] }} SupplierWire */

/**
 * Future database implementation contract. The fixture repository below is
 * temporary; replacing it must not require edits in entities or features.
 */
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
}
