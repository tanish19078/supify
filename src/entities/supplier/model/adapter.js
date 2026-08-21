const methodLabels = {
  self_declared: 'Self-declared',
  document_review: 'Document review',
  third_party_api: 'Registry check',
  human_audit: 'Human audit',
  site_visit: 'Site visit',
}

const evidenceKindLabels = {
  document: 'Document',
  photo: 'Photo',
  registry_response: 'Registry check',
  audit_report: 'Audit report',
  reference_contact: 'Reference call',
}

export function toSupplier(wire) {  return {
    id: wire.id,
    legalName: wire.legal_name,
    tradeName: wire.trade_name || wire.legal_name,
    location: wire.location,
    category: wire.category,
    capacity: wire.capacity,
    moq: wire.moq,
    leadTime: wire.lead_time,
    listingState: wire.listing_state,
    trust: {
      band: wire.trust.band,
      method: wire.trust.method,
      methodLabel: methodLabels[wire.trust.method],
      verifiedAt: wire.trust.verified_at,
      validUntil: wire.trust.valid_until,
      lastChecked: wire.trust.last_checked,
      pillars: wire.trust.pillars.map(([name, score]) => ({ name, score })),
      gatesApplied: wire.trust.gates_applied,
    },
    claims: wire.claims.map((claim) => ({
      key: claim.key,
      label: claim.label,
      value: claim.value,
      state: claim.state,
      method: claim.method,
      methodLabel: methodLabels[claim.method],
      verifiedAt: claim.verified_at,
      validUntil: claim.valid_until,
      evidenceCount: claim.evidence_count,
      evidence: (claim.evidence || []).map((item) => ({
        kind: item.kind,
        kindLabel: evidenceKindLabels[item.kind] || item.kind,
        label: item.label,
        issuedOn: item.issued_on,
      })),
    })),
  }
}

export function toVerificationTask(taskWire, supplierWire) {
  return {
    id: taskWire.id,
    supplierId: taskWire.supplier_id,
    claimKey: taskWire.claim_key,
    claimLabel: taskWire.claim_label,
    state: taskWire.state,
    priority: taskWire.priority,
    assignedTo: taskWire.assigned_to,
    sla: taskWire.sla,
    evidenceCount: taskWire.evidence_count || 0,
    requiresDual: taskWire.requires_dual || false,
    supplier: supplierWire ? toSupplier(supplierWire) : null,
  }
}
