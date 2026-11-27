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

// Runs 7 simple checks covering all topics in our Java syllabus
public class TestApp {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Supify Backend Test Suite ===");

        testRepositoryCrud();
        testStreamSearch();
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

    private static void testRepositoryCrud() {
        SupifyPlatform platform = new SupifyPlatform();
        platform.loadSampleData();

        Supplier temp = new Supplier("SUP-TMP", "Temp Vendor", "Delhi", "Packaging", 5000, 100, "7 days");
        platform.save(temp);
        boolean found = platform.findById("SUP-TMP") != null;
        boolean deleted = platform.deleteById("SUP-TMP");

        check("1. Seed Data & Generic Repository", platform.count() == 5 && found && deleted);
    }

    private static void testStreamSearch() {
        SupifyPlatform platform = new SupifyPlatform();
        platform.loadSampleData();

        List<Supplier> puneVerified = platform.search("Pune", TrustBand.VERIFIED, "capacity");
        List<Supplier> byName = platform.search("", null, "name");
        List<String> categories = platform.getAllCategories();

        check("2. Stream Search & Sorting",
                puneVerified.size() == 1 && puneVerified.get(0).getId().equals("SUP-2")
                        && byName.size() == 5 && categories.size() == 5);
    }

    private static void testFourLayerSpine() throws Exception {
        SupifyPlatform platform = new SupifyPlatform();
        Supplier sup = new Supplier("SUP-9", "Apex Precision", "Chennai", "Machining", 20000, 500, "12 days");
        platform.save(sup);

        Claim c1 = platform.submitClaim("SUP-9", "gst", "GST Active", "IDENTITY", "33AAACA1111Z1", "GST PDF", "PDF", "gst.pdf");
        Claim c2 = platform.submitClaim("SUP-9", "legal", "MCA Certificate", "LEGAL", "CIN-991", "MCA PDF", "PDF", "mca.pdf");
        VerificationCheck chk1 = platform.verifyClaim(c1.getId(), "Ver-1", VerificationMethod.THIRD_PARTY_API, true, "GST verified", 3600);
        VerificationCheck chk2 = platform.verifyClaim(c2.getId(), "Ver-1", VerificationMethod.HUMAN_AUDIT, true, "Legal audit passed", 3600);

        check("3. 4-Layer Spine (Claim -> Evidence -> Check -> Attestation)",
                "PASS".equals(chk1.getOutcome()) && "PASS".equals(chk2.getOutcome())
                        && "VERIFIED".equals(c1.getStatus())
                        && platform.getEvidenceForClaim(c1.getId()).size() == 1
                        && platform.getActiveAttestationForClaim(c1.getId()) != null
                        && sup.getTrustBand() == TrustBand.VERIFIED);
    }

    private static void testTrustCapping() throws Exception {
        SupifyPlatform platform = new SupifyPlatform();
        Supplier sup = new Supplier("SUP-CAP", "No-Identity Co", "Surat", "Textiles", 30000, 500, "10 days");
        platform.save(sup);

        Claim q = platform.submitClaim("SUP-CAP", "iso", "ISO 9001", "QUALITY", "ISO-1", "ISO PDF", "PDF", "iso.pdf");
        platform.verifyClaim(q.getId(), "Aud-1", VerificationMethod.SITE_VISIT, true, "Site visit passed", 3600);
        boolean cappedUnverified = sup.getTrustBand() == TrustBand.UNVERIFIED && sup.getTrustScore() <= 0.30;

        Claim idClaim = platform.submitClaim("SUP-CAP", "gst", "GST", "IDENTITY", "24ABC", "GST PDF", "PDF", "gst.pdf");
        platform.verifyClaim(idClaim.getId(), "Ver-1", VerificationMethod.THIRD_PARTY_API, true, "GST ok", 3600);
        boolean cappedBasic = sup.getTrustBand() == TrustBand.BASIC && sup.getTrustScore() <= 0.65;

        Claim bad = platform.submitClaim("SUP-CAP", "legal", "MCA", "LEGAL", "BAD", "Blurry scan", "IMAGE", "bad.png");
        VerificationCheck fail = platform.verifyClaim(bad.getId(), "Ver-1", VerificationMethod.DOCUMENT_REVIEW, false, "Scan unreadable", 3600);
        boolean rejected = "FAIL".equals(fail.getOutcome()) && "REJECTED".equals(bad.getStatus());

        check("4. Trust Band Capping & Rejection", cappedUnverified && cappedBasic && rejected);
    }

    private static void testDaemonDecay() throws Exception {
        SupifyPlatform platform = new SupifyPlatform();
        Supplier sup = new Supplier("SUP-DEC", "Decay Demo Co", "Noida", "Electronics", 10000, 100, "7 days");
        platform.save(sup);

        Claim c = platform.submitClaim("SUP-DEC", "gst", "GST Short", "IDENTITY", "09ABC", "GST PDF", "PDF", "gst.pdf");
        platform.verifyClaim(c.getId(), "Ver-1", VerificationMethod.DOCUMENT_REVIEW, true, "1s expiry test", 1);

        DecayMonitorThread daemon = new DecayMonitorThread(platform, 200);
        boolean isDaemon = daemon.isDaemon();
        daemon.start();
        Thread.sleep(1500);
        daemon.stopMonitor();

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

        boolean missingSup = false;
        boolean blankLabel = false;
        boolean missingClaim = false;
        boolean blankNote = false;

        try { platform.submitClaim("NONE", "k", "Label", "QUALITY", "val", "", "PDF", ""); } catch (SupifyException e) { missingSup = true; }
        try { platform.submitClaim("SUP-1", "k", "   ", "QUALITY", "val", "", "PDF", ""); } catch (SupifyException e) { blankLabel = true; }
        try { platform.verifyClaim("NONE", "Ver", VerificationMethod.DOCUMENT_REVIEW, true, "ok", 60); } catch (SupifyException e) { missingClaim = true; }
        try { platform.verifyClaim(pending.getId(), "Ver", VerificationMethod.DOCUMENT_REVIEW, true, "  ", 60); } catch (SupifyException e) { blankNote = true; }

        check("7. Checked SupifyException Guards", missingSup && blankLabel && missingClaim && blankNote);
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
