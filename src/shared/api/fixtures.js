export const supplierWires = [
  {
    id: 'aravind-fasteners',
    legal_name: 'Aravind Precision Fasteners Private Limited',
    trade_name: 'Aravind Fasteners',
    location: 'Ludhiana, Punjab, IN',
    category: 'Industrial fasteners',
    capacity: '80,000 units / month',
    moq: '2,000 units',
    lead_time: '14 days',
    listing_state: 'listed',
    trust: {
      band: 'verified',
      method: 'third_party_api',
      verified_at: '2026-08-15T09:00:00Z',
      valid_until: '2026-11-15T09:00:00Z',
      last_checked: '4 days ago',
      pillars: [
        ['Identity', 92], ['Legal', 86], ['Quality', 68], ['Capability', 58],
      ],
      gates_applied: [],
    },
    claims: [
      { key: 'gst_registration', label: 'GST registration', value: '03AAECA9082G1ZP', state: 'verified', method: 'third_party_api', verified_at: '2026-08-15T09:00:00Z', valid_until: '2026-11-15T09:00:00Z', evidence_count: 2 },
      { key: 'iso_9001', label: 'ISO 9001:2015', value: 'Certificate uploaded', state: 'verified', method: 'document_review', verified_at: '2026-07-28T09:00:00Z', valid_until: '2027-03-30T09:00:00Z', evidence_count: 3 },
      { key: 'annual_capacity', label: 'Stated production capacity', value: '80,000 units / month', state: 'under_review', method: 'self_declared', verified_at: null, valid_until: null, evidence_count: 1 },
    ],
  },
  {
    id: 'monarch-industrial',
    legal_name: 'Monarch Industrial Components LLP',
    trade_name: 'Monarch Industrial',
    location: 'Rajkot, Gujarat, IN',
    category: 'Precision machined parts',
    capacity: '35,000 components / month',
    moq: '500 units',
    lead_time: '21 days',
    listing_state: 'listed',
    trust: {
      band: 'basic',
      method: 'document_review',
      verified_at: '2026-07-02T09:00:00Z',
      valid_until: '2026-10-02T09:00:00Z',
      last_checked: '7 weeks ago',
      pillars: [
        ['Identity', 74], ['Legal', 57], ['Quality', 18], ['Capability', 26],
      ],
      gates_applied: ['Quality evidence is incomplete'],
    },
    claims: [
      { key: 'gst_registration', label: 'GST registration', value: '24AAXFM9104Q1ZN', state: 'verified', method: 'third_party_api', verified_at: '2026-07-02T09:00:00Z', valid_until: '2026-10-02T09:00:00Z', evidence_count: 2 },
      { key: 'quality_management', label: 'Quality management process', value: 'Self-declared process', state: 'under_review', method: 'self_declared', verified_at: null, valid_until: null, evidence_count: 1 },
    ],
  },
  {
    id: 'narmada-fabrication',
    legal_name: 'Narmada Advanced Custom Fabrication and Integrated Industrial Solutions Private Limited',
    trade_name: 'Narmada Fabrication',
    location: 'Indore, Madhya Pradesh, IN',
    category: 'Sheet-metal fabrication',
    capacity: 'On request',
    moq: '100 units',
    lead_time: '28 days',
    listing_state: 'listed',
    trust: {
      band: 'unverified',
      method: 'self_declared',
      verified_at: '2026-08-18T09:00:00Z',
      valid_until: null,
      last_checked: 'Supplier assertion, 1 day ago',
      pillars: [
        ['Identity', 12], ['Legal', 0], ['Quality', 15], ['Capability', 22],
      ],
      gates_applied: ['Identity has not been independently established'],
    },
    claims: [
      { key: 'gst_registration', label: 'GST registration', value: 'Supplier-submitted identifier', state: 'under_review', method: 'self_declared', verified_at: null, valid_until: null, evidence_count: 1 },
      { key: 'factory_capacity', label: 'Factory capacity', value: 'On request', state: 'submitted', method: 'self_declared', verified_at: null, valid_until: null, evidence_count: 1 },
    ],
  },
]

export const verificationTaskWires = [
  { id: 'VT-1084', supplier_id: 'aravind-fasteners', claim_key: 'annual_capacity', claim_label: 'Stated production capacity', state: 'in_review', priority: 'High', assigned_to: 'You', sla: 'Due in 2h 18m', evidence_count: 1 },
  { id: 'VT-1081', supplier_id: 'monarch-industrial', claim_key: 'quality_management', claim_label: 'Quality management process', state: 'awaiting_supplier', priority: 'Normal', assigned_to: 'R. Kapoor', sla: 'Awaiting evidence' },
  { id: 'VT-1077', supplier_id: 'narmada-fabrication', claim_key: 'gst_registration', claim_label: 'GST registration', state: 'queued', priority: 'High', assigned_to: 'Unassigned', sla: 'Due today', evidence_count: 1 },
]
