package com.supify.service;

import com.supify.dao.FileHelper;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.Supplier;
import com.supify.model.TrustBand;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationMethod;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Shared platform holding suppliers, 4-layer verification records, search, and trust rules
public class SupifyPlatform {
    private final Map<String, Supplier> suppliers = new HashMap<>();
    private final Map<String, Claim> claims = new HashMap<>();
    private final List<Evidence> evidences = new ArrayList<>();
    private final List<VerificationCheck> checks = new ArrayList<>();
    private final Map<String, Attestation> attestations = new HashMap<>();
    private final List<String> alerts = new ArrayList<>();
    private int idCounter = 100;
    private int supplierCounter = 5;

    public synchronized String nextId(String prefix) {
        idCounter++;
        return prefix + "-" + idCounter;
    }

    public synchronized String nextSupplierId() {
        supplierCounter++;
        return "SUP-" + supplierCounter;
    }

    public synchronized Supplier save(Supplier supplier) {
        suppliers.put(supplier.getId(), supplier);
        if (supplier.getId() != null && supplier.getId().startsWith("SUP-")) {
            try {
                int num = Integer.parseInt(supplier.getId().substring(4));
                if (num > supplierCounter) {
                    supplierCounter = num;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return supplier;
    }

    public synchronized Supplier findById(String id) {
        return suppliers.get(id);
    }

    public synchronized List<Supplier> findAll() {
        return new ArrayList<>(suppliers.values());
    }

    public synchronized long count() {
        return suppliers.size();
    }

    // Supplier CRUD methods (add, update, delete)
    public synchronized Supplier addSupplier(String name, String city, String category,
                                             int capacityPerMonth, int moq, String leadTime) throws SupifyException {
        if (name == null || name.trim().isEmpty()) {
            throw new SupifyException("Supplier name cannot be empty.");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new SupifyException("City cannot be empty.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new SupifyException("Category cannot be empty.");
        }
        if (capacityPerMonth <= 0) {
            throw new SupifyException("Monthly capacity must be greater than 0.");
        }
        if (moq <= 0) {
            throw new SupifyException("MOQ must be greater than 0.");
        }

        String cleanLead = (leadTime == null || leadTime.trim().isEmpty()) ? "14 days" : leadTime.trim();
        Supplier supplier = new Supplier(
                nextSupplierId(), name.trim(), city.trim(), category.trim(),
                capacityPerMonth, moq, cleanLead);
        return save(supplier);
    }

    public synchronized Supplier updateSupplier(String id, String name, String city, String category,
                                                int capacityPerMonth, int moq, String leadTime) throws SupifyException {
        Supplier s = suppliers.get(id);
        if (s == null) {
            throw new SupifyException("Supplier not found: " + id);
        }
        if (name != null && !name.trim().isEmpty()) {
            s.setName(name.trim());
        }
        if (city != null && !city.trim().isEmpty()) {
            s.setCity(city.trim());
        }
        if (category != null && !category.trim().isEmpty()) {
            s.setCategory(category.trim());
        }
        if (capacityPerMonth > 0) {
            s.setCapacityPerMonth(capacityPerMonth);
        }
        if (moq > 0) {
            s.setMoq(moq);
        }
        if (leadTime != null && !leadTime.trim().isEmpty()) {
            s.setLeadTime(leadTime.trim());
        }
        return s;
    }

    public synchronized boolean deleteSupplier(String id) {
        if (suppliers.remove(id) == null) {
            return false;
        }

        List<String> deadClaimIds = new ArrayList<>();
        for (Claim c : claims.values()) {
            if (c.getSupplierId().equals(id)) {
                deadClaimIds.add(c.getId());
            }
        }
        for (String claimId : deadClaimIds) {
            claims.remove(claimId);
        }
        for (int i = evidences.size() - 1; i >= 0; i--) {
            if (deadClaimIds.contains(evidences.get(i).getClaimId())) {
                evidences.remove(i);
            }
        }
        for (int i = checks.size() - 1; i >= 0; i--) {
            if (deadClaimIds.contains(checks.get(i).getClaimId())) {
                checks.remove(i);
            }
        }

        List<String> deadAttIds = new ArrayList<>();
        for (Attestation a : attestations.values()) {
            if (deadClaimIds.contains(a.getClaimId())) {
                deadAttIds.add(a.getId());
            }
        }
        for (String attId : deadAttIds) {
            attestations.remove(attId);
        }
        return true;
    }

    // Loads saved CSV suppliers on startup (or seeds sample data on first run)
    public synchronized String loadStartupData(File csvFile) {
        if (csvFile == null || !csvFile.exists()) {
            loadSampleData();
            if (csvFile != null) {
                try {
                    FileHelper.exportToCsv(csvFile, findAll());
                } catch (IOException ignored) {
                }
            }
            return "Loaded " + count() + " sample suppliers (new catalog created).";
        }

        try {
            List<Supplier> saved = FileHelper.importFromCsv(csvFile);
            loadSampleData();

            List<String> savedIds = new ArrayList<>();
            for (Supplier s : saved) {
                savedIds.add(s.getId());
            }

            for (Supplier existing : findAll()) {
                if (!savedIds.contains(existing.getId())) {
                    deleteSupplier(existing.getId());
                }
            }

            for (Supplier s : saved) {
                save(s);
                if (!getClaimsForSupplier(s.getId()).isEmpty()) {
                    recalculateTrust(s.getId());
                }
            }
            return "Loaded " + count() + " suppliers from " + csvFile.getPath() + ".";
        } catch (IOException e) {
            loadSampleData();
            return "CSV read failed (" + e.getMessage() + "). Loaded " + count() + " sample suppliers.";
        }
    }

    // Resets memory and CSV back to the 5 default sample suppliers
    public synchronized void resetToSampleData(File csvFile) {
        suppliers.clear();
        claims.clear();
        evidences.clear();
        checks.clear();
        attestations.clear();
        alerts.clear();
        idCounter = 100;
        supplierCounter = 5;

        loadSampleData();
        if (csvFile != null) {
            try {
                FileHelper.exportToCsv(csvFile, findAll());
            } catch (IOException ignored) {
            }
        }
    }

    public synchronized List<Claim> getClaimsForSupplier(String supplierId) {
        List<Claim> result = new ArrayList<>();
        for (Claim c : claims.values()) {
            if (c.getSupplierId().equals(supplierId)) {
                result.add(c);
            }
        }
        return result;
    }

    public synchronized List<Claim> getPendingClaims() {
        List<Claim> result = new ArrayList<>();
        for (Claim c : claims.values()) {
            if ("SUBMITTED".equals(c.getStatus())) {
                result.add(c);
            }
        }
        return result;
    }

    public synchronized List<Evidence> getEvidenceForClaim(String claimId) {
        List<Evidence> result = new ArrayList<>();
        for (Evidence e : evidences) {
            if (e.getClaimId().equals(claimId)) {
                result.add(e);
            }
        }
        return result;
    }

    public synchronized List<VerificationCheck> getChecksForClaim(String claimId) {
        List<VerificationCheck> result = new ArrayList<>();
        for (VerificationCheck c : checks) {
            if (c.getClaimId().equals(claimId)) {
                result.add(c);
            }
        }
        return result;
    }

    public synchronized Attestation getActiveAttestationForClaim(String claimId) {
        for (Attestation a : attestations.values()) {
            if (a.getClaimId().equals(claimId) && "ACTIVE".equals(a.getState())) {
                return a;
            }
        }
        return null;
    }

    // Loads 5 sample suppliers, claims, proof files, and a 30s demo timer
    public void loadSampleData() {
        save(new Supplier("SUP-1", "Aravind Fasteners Pvt Ltd", "Coimbatore", "Fasteners", 50000, 1000, "14 days"));
        save(new Supplier("SUP-2", "Monarch Industrial Assemblies", "Pune", "Sheet Metal", 12000, 200, "21 days"));
        save(new Supplier("SUP-3", "Kavach Safety Systems", "Ahmedabad", "Safety Equipment", 25000, 500, "10 days"));
        save(new Supplier("SUP-4", "Suryodaya Solar Tech", "Jaipur", "Solar Structures", 80000, 2500, "18 days"));
        save(new Supplier("SUP-5", "Bharat Forgings Co", "Rajkot", "Forgings", 15000, 300, "15 days"));

        try {
            Claim c1 = submitClaim("SUP-1", "gst", "GSTIN Active", "IDENTITY", "33AABCA1234F1Z8", "GST Cert", "PDF", "gst.pdf");
            Claim c2 = submitClaim("SUP-1", "incorp", "MCA Corporate Filing", "LEGAL", "CIN-U2899TN", "COI Cert", "PDF", "coi.pdf");
            Claim c3 = submitClaim("SUP-1", "iso_9001", "ISO 9001 Quality", "QUALITY", "QA-98421", "ISO PDF", "PDF", "iso.pdf");
            Claim c4 = submitClaim("SUP-1", "capacity_cap", "50T/mo Cold Heading Capacity", "CAPABILITY", "50000 kg/mo", "Factory Report", "SITE_REPORT", "site.pdf");

            verifyClaim(c1.getId(), "API-Adapter", VerificationMethod.THIRD_PARTY_API, true, "GST portal active", 86400);
            verifyClaim(c2.getId(), "Verifier-01", VerificationMethod.DOCUMENT_REVIEW, true, "MCA verified", 86400);
            verifyClaim(c3.getId(), "Auditor-01", VerificationMethod.HUMAN_AUDIT, true, "ISO audit passed", 86400);
            verifyClaim(c4.getId(), "Auditor-Chennai", VerificationMethod.SITE_VISIT, true, "Inspected 4 headers", 30);

            Claim c5 = submitClaim("SUP-2", "gst", "GSTIN Active", "IDENTITY", "27AABCM9911K1Z1", "GST Cert", "PDF", "m_gst.pdf");
            Claim c6 = submitClaim("SUP-2", "incorp", "Factory License", "LEGAL", "MH-FAC-441", "License PDF", "PDF", "m_lic.pdf");
            Claim c7 = submitClaim("SUP-2", "quality_audit", "AS9100 Audit", "QUALITY", "AS9100D", "Audit Report", "SITE_REPORT", "m_as.pdf");
            verifyClaim(c5.getId(), "API-Adapter", VerificationMethod.THIRD_PARTY_API, true, "GST active", 86400);
            verifyClaim(c6.getId(), "Auditor-02", VerificationMethod.HUMAN_AUDIT, true, "Factory license valid", 86400);
            verifyClaim(c7.getId(), "Auditor-Pune", VerificationMethod.SITE_VISIT, true, "On-site audit passed", 86400);
            submitClaim("SUP-2", "iso_14001", "ISO 14001 Environmental", "COMPLIANCE", "ENV-2026", "ISO 14001 PDF", "PDF", "iso14001.pdf");

            Claim c8 = submitClaim("SUP-3", "gst", "GSTIN Active", "IDENTITY", "24AABCK1102M1Z3", "GST PDF", "PDF", "k_gst.pdf");
            Claim c9 = submitClaim("SUP-3", "legal_reg", "Trade License", "LEGAL", "GJ-2026-88", "License PDF", "PDF", "k_lic.pdf");
            verifyClaim(c8.getId(), "Verifier-01", VerificationMethod.DOCUMENT_REVIEW, true, "GST verified", 86400);
            verifyClaim(c9.getId(), "Verifier-01", VerificationMethod.THIRD_PARTY_API, true, "Trade registry matched", 86400);

            Claim c10 = submitClaim("SUP-4", "gst", "GSTIN Active", "IDENTITY", "08AABCS8821D1Z9", "GST PDF", "PDF", "s_gst.pdf");
            verifyClaim(c10.getId(), "Verifier-01", VerificationMethod.DOCUMENT_REVIEW, true, "GST valid", 86400);

            submitClaim("SUP-5", "self_cap", "15T Forging Claim", "QUALITY", "Self Declared", "Brochure", "PDF", "b_brochure.pdf");
        } catch (SupifyException e) {
            System.err.println("Seed error: " + e.getMessage());
        }
    }

    // Searches and sorts suppliers using a loop and lambda Comparator
    public List<Supplier> search(String keyword, TrustBand minBand, String sortBy) {
        String q = (keyword == null) ? "" : keyword.trim().toLowerCase();
        List<Supplier> result = new ArrayList<>();

        for (Supplier s : findAll()) {
            boolean matchesKeyword = q.isEmpty()
                    || s.getName().toLowerCase().contains(q)
                    || s.getCity().toLowerCase().contains(q)
                    || s.getCategory().toLowerCase().contains(q);
            boolean matchesBand = minBand == null || s.getTrustBand().getLevel() >= minBand.getLevel();
            if (matchesKeyword && matchesBand) {
                result.add(s);
            }
        }

        result.sort(getComparator(sortBy));
        return result;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        for (Supplier s : findAll()) {
            if (!categories.contains(s.getCategory())) {
                categories.add(s.getCategory());
            }
        }
        Collections.sort(categories);
        return categories;
    }

    private Comparator<Supplier> getComparator(String sortBy) {
        if ("capacity".equalsIgnoreCase(sortBy)) {
            return (a, b) -> Integer.compare(b.getCapacityPerMonth(), a.getCapacityPerMonth());
        }
        if ("name".equalsIgnoreCase(sortBy)) {
            return (a, b) -> a.getName().compareToIgnoreCase(b.getName());
        }
        return (a, b) -> Double.compare(b.getTrustScore(), a.getTrustScore());
    }

    // Supplier action: creates a SUBMITTED claim and optional evidence (no attestation)
    public synchronized Claim submitClaim(String supplierId, String claimTypeKey, String label, String pillar,
                                          String value, String docTitle, String docType, String filePath) throws SupifyException {
        if (findById(supplierId) == null) {
            throw new SupifyException("Supplier not found: " + supplierId);
        }
        if (label == null || label.trim().isEmpty()) {
            throw new SupifyException("Claim label cannot be empty.");
        }

        Claim claim = new Claim(nextId("CLM"), supplierId, claimTypeKey, label, pillar, value);
        claims.put(claim.getId(), claim);

        if (docTitle != null && !docTitle.trim().isEmpty()) {
            Evidence evidence = new Evidence(nextId("EVD"), claim.getId(), docTitle, docType, filePath);
            evidences.add(evidence);
        }
        return claim;
    }

    // Verifier action: records VerificationCheck, creates Attestation if approved, updates trust
    public synchronized VerificationCheck verifyClaim(String claimId, String verifierName,
                                                      VerificationMethod method, boolean approved,
                                                      String note, int validSeconds) throws SupifyException {
        Claim claim = claims.get(claimId);
        if (claim == null) {
            throw new SupifyException("Claim not found: " + claimId);
        }
        if (note == null || note.trim().isEmpty()) {
            throw new SupifyException("Verifier note is required.");
        }

        String outcome = approved ? "PASS" : "FAIL";
        VerificationCheck check = new VerificationCheck(
                nextId("CHK"), claimId, method, outcome, verifierName, note);
        checks.add(check);

        if (approved) {
            Attestation attestation = new Attestation(nextId("ATT"), claimId, check.getId(), method, validSeconds);
            attestations.put(attestation.getId(), attestation);
            claim.setStatus("VERIFIED");
        } else {
            claim.setStatus("REJECTED");
        }

        recalculateTrust(claim.getSupplierId());
        return check;
    }

    // Recalculates trust band and score using only active attestations
    public synchronized void recalculateTrust(String supplierId) {
        Supplier supplier = suppliers.get(supplierId);
        if (supplier == null) {
            return;
        }

        double total = 0.0;
        int activeCount = 0;
        boolean hasIdentity = false;
        boolean hasLegal = false;
        boolean hasAudit = false;

        for (Claim claim : getClaimsForSupplier(supplierId)) {
            Attestation att = getActiveAttestationForClaim(claim.getId());
            if (att != null) {
                total += att.getMethod().getScore();
                activeCount++;

                if ("IDENTITY".equalsIgnoreCase(claim.getPillar())) {
                    hasIdentity = true;
                }
                if ("LEGAL".equalsIgnoreCase(claim.getPillar())) {
                    hasLegal = true;
                }
                String methodName = att.getMethod().name();
                if ("HUMAN_AUDIT".equals(methodName) || "SITE_VISIT".equals(methodName)) {
                    hasAudit = true;
                }
            }
        }

        double avgScore = 0.0;
        if (activeCount > 0) {
            avgScore = Math.min(1.0, total / Math.max(3, activeCount));
        }

        TrustBand band;
        if (!hasIdentity) {
            band = TrustBand.UNVERIFIED;
            avgScore = Math.min(avgScore, 0.30);
        } else if (!hasLegal) {
            band = TrustBand.BASIC;
            avgScore = Math.min(avgScore, 0.65);
        } else if (hasAudit && avgScore >= 0.85) {
            band = TrustBand.AUDITED;
        } else {
            band = TrustBand.VERIFIED;
        }

        supplier.setTrustBand(band);
        supplier.setTrustScore(avgScore);
    }

    // Expires overdue badges, updates claim status, recalculates trust, and saves alerts
    public synchronized void checkExpiredAttestations() {
        for (Attestation att : attestations.values()) {
            if (att.expireIfNeeded()) {
                Claim claim = claims.get(att.getClaimId());
                if (claim != null) {
                    claim.setStatus("EXPIRED");
                    recalculateTrust(claim.getSupplierId());
                    alerts.add("Attestation " + att.getId() + " expired for '"
                            + claim.getLabel() + "' (" + claim.getSupplierId() + ")");
                }
            }
        }
    }

    public synchronized List<String> getAndClearAlerts() {
        List<String> copy = new ArrayList<>(alerts);
        alerts.clear();
        return copy;
    }
}
