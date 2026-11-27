package com.supify.app;

import com.supify.dao.FileHelper;
import com.supify.model.Claim;
import com.supify.model.Supplier;
import com.supify.model.TrustBand;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationMethod;
import com.supify.service.DecayMonitorThread;
import com.supify.service.SupifyException;
import com.supify.service.SupifyPlatform;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

// Runs 7 simple checks covering all core Java features
public class TestApp {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Supify Backend Test Suite ===");

        testCrudAndCsvPersistence();
        testSearchAndSort();
        testFourLayerSpine();
        testTrustCapping();
        testDaemonDecay();
        testFileHelper();
        testExceptionGuards();

        System.out.println("\n=== TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testCrudAndCsvPersistence() throws Exception {
        SupifyPlatform platform = new SupifyPlatform();
        platform.loadSampleData();

        Supplier added = platform.addSupplier("Apex Castings", "Surat", "Castings", 30000, 500, "12 days");
        platform.updateSupplier(added.getId(), "", "Vadodara", "", 35000, 0, "");

        File tempCsv = new File("data/test_startup_crud.csv");
        try {
            FileHelper.exportToCsv(tempCsv, platform.findAll());

            SupifyPlatform restarted = new SupifyPlatform();
            restarted.loadStartupData(tempCsv);
            Supplier reloaded = restarted.findById(added.getId());
            boolean persisted = reloaded != null
                    && "Vadodara".equals(reloaded.getCity())
                    && reloaded.getCapacityPerMonth() == 35000;
            boolean deleted = restarted.deleteSupplier(added.getId());

            check("1. Supplier CRUD & CSV Restart Persistence",
                    added.getId().equals("SUP-6") && persisted && deleted && restarted.count() == 5);
        } finally {
            tempCsv.delete();
        }
    }

    private static void testSearchAndSort() {
        SupifyPlatform platform = new SupifyPlatform();
        platform.loadSampleData();

        List<Supplier> puneVerified = platform.search("Pune", TrustBand.VERIFIED, "capacity");
        List<Supplier> byName = platform.search("", null, "name");
        List<String> categories = platform.getAllCategories();

        check("2. Search & Comparator Sorting",
                puneVerified.size() == 1 && puneVerified.get(0).getId().equals("SUP-2")
                        && byName.size() == 5 && categories.size() == 5);
    }

    private static void testFourLayerSpine() throws Exception {
        SupifyPlatform platform = new SupifyPlatform();
        Supplier sup = new Supplier("SUP-T1", "Alpha Metals", "Mumbai", "Steel", 10000, 100, "7 days");
        platform.save(sup);

        Claim c1 = platform.submitClaim("SUP-T1", "gst", "GST", "IDENTITY", "27AAA", "GST PDF", "PDF", "gst.pdf");
        Claim c2 = platform.submitClaim("SUP-T1", "lic", "License", "LEGAL", "MH-1", "Lic PDF", "PDF", "lic.pdf");

        boolean pendingFirst = "SUBMITTED".equals(c1.getStatus())
                && sup.getTrustBand() == TrustBand.UNVERIFIED
                && platform.getPendingClaims().size() == 2;

        VerificationCheck chk1 = platform.verifyClaim(c1.getId(), "Verifier", VerificationMethod.THIRD_PARTY_API, true, "GST ok", 86400);
        VerificationCheck chk2 = platform.verifyClaim(c2.getId(), "Verifier", VerificationMethod.DOCUMENT_REVIEW, true, "Lic ok", 86400);

        check("3. 4-Layer Spine (Claim -> Evidence -> Check -> Attestation)",
                pendingFirst && "PASS".equals(chk1.getOutcome()) && "PASS".equals(chk2.getOutcome())
                        && sup.getTrustBand() == TrustBand.VERIFIED
                        && platform.getEvidenceForClaim(c1.getId()).size() == 1
                        && platform.getChecksForClaim(c1.getId()).size() == 1);
    }

    private static void testTrustCapping() throws Exception {
        SupifyPlatform platform = new SupifyPlatform();
        Supplier sup = new Supplier("SUP-T2", "Beta Plastics", "Pune", "Polymers", 5000, 50, "5 days");
        platform.save(sup);

        Claim q = platform.submitClaim("SUP-T2", "iso", "ISO", "QUALITY", "ISO-1", "ISO", "PDF", "iso.pdf");
        platform.verifyClaim(q.getId(), "Auditor", VerificationMethod.SITE_VISIT, true, "Good", 86400);
        boolean cappedNoIdentity = sup.getTrustBand() == TrustBand.UNVERIFIED && sup.getTrustScore() <= 0.30;

        Claim id = platform.submitClaim("SUP-T2", "gst", "GST", "IDENTITY", "27BBB", "GST", "PDF", "gst.pdf");
        platform.verifyClaim(id.getId(), "Verifier", VerificationMethod.THIRD_PARTY_API, true, "Ok", 86400);
        boolean cappedNoLegal = sup.getTrustBand() == TrustBand.BASIC && sup.getTrustScore() <= 0.65;

        Claim bad = platform.submitClaim("SUP-T2", "bad", "Fake", "LEGAL", "NONE", "Doc", "PDF", "bad.pdf");
        platform.verifyClaim(bad.getId(), "Verifier", VerificationMethod.DOCUMENT_REVIEW, false, "Mismatch", 86400);

        check("4. Trust Band Capping & Rejection",
                cappedNoIdentity && cappedNoLegal && "REJECTED".equals(bad.getStatus())
                        && platform.getActiveAttestationForClaim(bad.getId()) == null);
    }

    private static void testDaemonDecay() throws Exception {
        SupifyPlatform platform = new SupifyPlatform();
        Supplier sup = new Supplier("SUP-T3", "Gamma Textiles", "Surat", "Textiles", 20000, 200, "10 days");
        platform.save(sup);

        Claim c = platform.submitClaim("SUP-T3", "gst", "GST", "IDENTITY", "24CCC", "GST", "PDF", "gst.pdf");
        platform.verifyClaim(c.getId(), "Verifier", VerificationMethod.DOCUMENT_REVIEW, true, "Verified", 1);

        DecayMonitorThread monitor = new DecayMonitorThread(platform, 200);
        boolean isDaemon = monitor.isDaemon();
        monitor.start();
        Thread.sleep(1500);
        monitor.stopMonitor();

        check("5. Daemon Auto-Decay & Alert",
                isDaemon && "EXPIRED".equals(c.getStatus())
                        && platform.getActiveAttestationForClaim(c.getId()) == null
                        && sup.getTrustBand() == TrustBand.UNVERIFIED
                        && !platform.getAndClearAlerts().isEmpty());
    }

    private static void testFileHelper() throws Exception {
        SupifyPlatform platform = new SupifyPlatform();
        platform.loadSampleData();

        File csvFile = new File("data/test_suppliers.csv");
        File logFile = new File("data/test_audit.log");
        try {
            FileHelper.exportToCsv(csvFile, platform.findAll());
            try (FileWriter fw = new FileWriter(csvFile, true)) {
                fw.write("BAD,ROW,ONLY,4,COLS\n");
            }
            List<Supplier> imported = FileHelper.importFromCsv(csvFile);
            FileHelper.appendAuditLog(logFile, "test audit entry");
            List<String> lines = FileHelper.readAuditLog(logFile);

            check("6. CSV Export/Import & Audit Log (File I/O)",
                    imported.size() == 5 && lines.size() == 1 && lines.get(0).contains("test audit entry"));
        } finally {
            csvFile.delete();
            logFile.delete();
        }
    }

    private static void testExceptionGuards() throws Exception {
        SupifyPlatform platform = new SupifyPlatform();
        platform.loadSampleData();
        Claim pending = platform.submitClaim("SUP-1", "test", "Test Claim", "QUALITY", "VAL", "Doc", "PDF", "doc.pdf");

        boolean badSupplier = false;
        boolean missingUpdate = false;
        boolean missingSup = false;
        boolean blankLabel = false;
        boolean missingClaim = false;
        boolean blankNote = false;

        try { platform.addSupplier("  ", "Pune", "Steel", 100, 10, "5 days"); } catch (SupifyException e) { badSupplier = true; }
        try { platform.updateSupplier("NONE", "X", "", "", 0, 0, ""); } catch (SupifyException e) { missingUpdate = true; }
        try { platform.submitClaim("NONE", "k", "Label", "QUALITY", "val", "", "PDF", ""); } catch (SupifyException e) { missingSup = true; }
        try { platform.submitClaim("SUP-1", "k", "   ", "QUALITY", "val", "", "PDF", ""); } catch (SupifyException e) { blankLabel = true; }
        try { platform.verifyClaim("NONE", "Ver", VerificationMethod.DOCUMENT_REVIEW, true, "ok", 60); } catch (SupifyException e) { missingClaim = true; }
        try { platform.verifyClaim(pending.getId(), "Ver", VerificationMethod.DOCUMENT_REVIEW, true, "  ", 60); } catch (SupifyException e) { blankNote = true; }

        check("7. Checked SupifyException Guards",
                badSupplier && missingUpdate && missingSup && blankLabel && missingClaim && blankNote);
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            failed++;
            System.err.println("  [FAIL] " + name);
        }
    }
}
