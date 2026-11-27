package com.supify.app;

import com.supify.dao.FileHelper;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.Supplier;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationMethod;
import com.supify.service.DecayMonitorThread;
import com.supify.service.SupifyException;
import com.supify.service.SupifyPlatform;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Main entry point and console menus for Supify
public class Main {

    public static void main(String[] args) {
        SupifyPlatform platform = new SupifyPlatform();
        platform.loadSampleData();

        DecayMonitorThread monitor = new DecayMonitorThread(platform);
        monitor.start();

        try (Scanner sc = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                showAlerts(platform);
                printHeader("SUPIFY :: Supplier Verification CLI");
                System.out.println("  [1] Buyer Menu");
                System.out.println("  [2] Supplier Menu");
                System.out.println("  [3] Verifier Menu");
                System.out.println("  [4] Admin Menu");
                System.out.println("  [0] Exit");

                int choice = readInt(sc, "Select [0-4]", -1);
                if (choice == -2) {
                    break;
                }
                switch (choice) {
                    case 1:
                        buyerMenu(sc, platform);
                        break;
                    case 2:
                        supplierMenu(sc, platform);
                        break;
                    case 3:
                        verifierMenu(sc, platform);
                        break;
                    case 4:
                        adminMenu(sc, platform, monitor);
                        break;
                    case 0:
                        running = false;
                        monitor.stopMonitor();
                        System.out.println("\nGoodbye!");
                        break;
                    default:
                        System.out.println("  Invalid option.");
                        break;
                }
            }
        } finally {
            monitor.stopMonitor();
        }
    }

    private static void buyerMenu(Scanner sc, SupifyPlatform platform) {
        while (true) {
            showAlerts(platform);
            printHeader("BUYER MENU");
            System.out.println("  [1] List Suppliers");
            System.out.println("  [2] Search & Sort Suppliers");
            System.out.println("  [3] Inspect Supplier Claims");
            System.out.println("  [4] Compare 2 Suppliers");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice [0-4]", -1);
            if (ch == -2 || ch == 0) {
                break;
            }
            if (ch == 1) {
                printSuppliers(platform.search("", null, "score"));
            } else if (ch == 2) {
                System.out.print("Keyword / city / category (ENTER for all): ");
                System.out.flush();
                if (!sc.hasNextLine()) break;
                String kw = sc.nextLine().trim();
                int s = readInt(sc, "Sort [1=Score, 2=Capacity, 3=Name]", 1);
                if (s == -2) break;
                String sort = "score";
                if (s == 2) sort = "capacity";
                else if (s == 3) sort = "name";
                printSuppliers(platform.search(kw, null, sort));
            } else if (ch == 3) {
                inspectSupplier(sc, platform);
            } else if (ch == 4) {
                Supplier a = pickSupplier(sc, platform, "First supplier");
                if (a == null) continue;
                Supplier b = pickSupplier(sc, platform, "Second supplier");
                if (b == null) continue;
                System.out.println("\n--- Comparison ---");
                System.out.println("  " + a);
                System.out.println("  Cap: " + a.getCapacityPerMonth() + "/mo | MOQ: " + a.getMoq() + " | Lead: " + a.getLeadTime());
                System.out.println("  " + b);
                System.out.println("  Cap: " + b.getCapacityPerMonth() + "/mo | MOQ: " + b.getMoq() + " | Lead: " + b.getLeadTime());
            } else {
                System.out.println("  Invalid option.");
            }
        }
    }

    private static void supplierMenu(Scanner sc, SupifyPlatform platform) {
        String myId = "SUP-1";
        while (true) {
            showAlerts(platform);
            printHeader("SUPPLIER MENU (" + myId + ")");
            System.out.println("  [1] My Standing");
            System.out.println("  [2] Submit Claim + Evidence");
            System.out.println("  [3] Active Expiry Timers");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice [0-3]", -1);
            if (ch == -2 || ch == 0) {
                break;
            }
            if (ch == 1) {
                Supplier me = platform.findById(myId);
                if (me != null) {
                    System.out.println("\n  " + me + " | Claims: " + platform.getClaimsForSupplier(myId).size());
                }
            } else if (ch == 2) {
                System.out.print("Claim title (e.g. ISO 14001): ");
                System.out.flush();
                if (!sc.hasNextLine()) break;
                String label = sc.nextLine().trim();
                System.out.print("Pillar [IDENTITY / LEGAL / QUALITY / CAPABILITY]: ");
                System.out.flush();
                if (!sc.hasNextLine()) break;
                String pillar = sc.nextLine().trim().toUpperCase();
                if (pillar.isEmpty()) pillar = "QUALITY";
                System.out.print("Certificate / value: ");
                System.out.flush();
                if (!sc.hasNextLine()) break;
                String val = sc.nextLine().trim();
                try {
                    Claim c = platform.submitClaim(myId, label.toLowerCase().replace(" ", "_"),
                            label, pillar, val, label + " PDF", "PDF", "docs/" + label + ".pdf");
                    System.out.println("  Submitted " + c.getId() + " with PDF evidence for review.");
                } catch (SupifyException e) {
                    System.out.println("  [ERROR] " + e.getMessage());
                }
            } else if (ch == 3) {
                System.out.println("\n  Active Timers:");
                int count = 0;
                for (Claim c : platform.getClaimsForSupplier(myId)) {
                    Attestation a = platform.getActiveAttestationForClaim(c.getId());
                    if (a != null) {
                        System.out.println("  - " + c.getLabel() + " -> " + a.getState()
                                + " via " + a.getMethod() + " (" + a.secondsLeft() + "s left)");
                        count++;
                    }
                }
                if (count == 0) {
                    System.out.println("  (No active attestations found.)");
                }
            } else {
                System.out.println("  Invalid option.");
            }
        }
    }

    private static void verifierMenu(Scanner sc, SupifyPlatform platform) {
        while (true) {
            showAlerts(platform);
            printHeader("VERIFIER MENU");
            System.out.println("  [1] Pending Claims");
            System.out.println("  [2] Approve / Reject Claim");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice [0-2]", -1);
            if (ch == -2 || ch == 0) {
                break;
            }
            List<Claim> pending = platform.getPendingClaims();
            if (ch == 1) {
                if (pending.isEmpty()) {
                    System.out.println("  No pending claims.");
                } else {
                    for (Claim c : pending) {
                        System.out.println("  - " + c + " | Supplier: " + c.getSupplierId());
                        for (Evidence e : platform.getEvidenceForClaim(c.getId())) {
                            System.out.println("      Proof: " + e);
                        }
                    }
                }
            } else if (ch == 2) {
                reviewClaim(sc, platform, pending);
            } else {
                System.out.println("  Invalid option.");
            }
        }
    }

    private static void adminMenu(Scanner sc, SupifyPlatform platform, DecayMonitorThread monitor) {
        while (true) {
            showAlerts(platform);
            printHeader("ADMIN MENU");
            System.out.println("  [1] Thread Status");
            System.out.println("  [2] CSV Export / Import");
            System.out.println("  [3] Audit Log (File I/O)");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice [0-3]", -1);
            if (ch == -2 || ch == 0) {
                break;
            }
            if (ch == 1) {
                monitor.printThreadInfo();
            } else if (ch == 2) {
                File csvFile = new File("data/suppliers.csv");
                try {
                    FileHelper.exportToCsv(csvFile, platform.findAll());
                    List<Supplier> imported = FileHelper.importFromCsv(csvFile);
                    List<String> names = new ArrayList<>();
                    for (Supplier s : imported) {
                        names.add(s.getName());
                    }
                    System.out.println("  Synced " + imported.size() + " suppliers to " + csvFile.getPath() + ": " + names);
                } catch (IOException e) {
                    System.out.println("  [ERROR] CSV error: " + e.getMessage());
                }
            } else if (ch == 3) {
                File logFile = new File("data/audit.log");
                try {
                    FileHelper.appendAuditLog(logFile, "Admin checkpoint saved.");
                    List<String> lines = FileHelper.readAuditLog(logFile);
                    System.out.println("\n--- " + logFile.getPath() + " ---");
                    for (String line : lines) {
                        System.out.println("  " + line);
                    }
                } catch (IOException e) {
                    System.out.println("  [ERROR] Audit log error: " + e.getMessage());
                }
            } else {
                System.out.println("  Invalid option.");
            }
        }
    }

    private static void inspectSupplier(Scanner sc, SupifyPlatform platform) {
        Supplier s = pickSupplier(sc, platform, "Select supplier");
        if (s == null) return;

        System.out.println("\n  " + s + " | Cap: " + s.getCapacityPerMonth()
                + "/mo | MOQ: " + s.getMoq() + " | Lead: " + s.getLeadTime());
        List<Claim> claims = platform.getClaimsForSupplier(s.getId());
        if (claims.isEmpty()) {
            System.out.println("  [UNVERIFIED - No claims submitted yet]");
            return;
        }
        for (Claim c : claims) {
            System.out.println("  - " + c);
            for (Evidence e : platform.getEvidenceForClaim(c.getId())) {
                System.out.println("      Evidence : " + e);
            }
            for (VerificationCheck chk : platform.getChecksForClaim(c.getId())) {
                System.out.println("      Check    : " + chk);
            }
            Attestation a = platform.getActiveAttestationForClaim(c.getId());
            if (a != null) {
                System.out.println("      Attest   : " + a.getId() + " [" + a.getState()
                        + " via " + a.getMethod() + ", " + a.secondsLeft() + "s left]");
            } else {
                System.out.println("      Attest   : [NO ACTIVE ATTESTATION - Claim status: " + c.getStatus() + "]");
            }
        }
    }

    private static void reviewClaim(Scanner sc, SupifyPlatform platform, List<Claim> pending) {
        if (pending.isEmpty()) {
            System.out.println("  No pending claims.");
            return;
        }
        System.out.println();
        for (int i = 0; i < pending.size(); i++) {
            Claim c = pending.get(i);
            System.out.println("  [" + (i + 1) + "] " + c.getId() + " | " + c.getSupplierId() + " | " + c.getLabel());
        }
        int sel = readInt(sc, "Select claim", 1);
        if (sel == -2) return;
        int idx = sel - 1;
        if (idx < 0 || idx >= pending.size()) {
            System.out.println("  Invalid claim number.");
            return;
        }
        Claim target = pending.get(idx);

        int m = readInt(sc, "Method [1=DOC, 2=API, 3=AUDIT, 4=SITE]", 1);
        if (m == -2) return;
        VerificationMethod method = VerificationMethod.DOCUMENT_REVIEW;
        if (m == 2) method = VerificationMethod.THIRD_PARTY_API;
        else if (m == 3) method = VerificationMethod.HUMAN_AUDIT;
        else if (m == 4) method = VerificationMethod.SITE_VISIT;

        System.out.print("Approve? (Y/N) [Y]: ");
        System.out.flush();
        if (!sc.hasNextLine()) return;
        String ans = sc.nextLine().trim();
        boolean approved = ans.isEmpty() || ans.equalsIgnoreCase("Y");

        System.out.print("Note (ENTER for default): ");
        System.out.flush();
        if (!sc.hasNextLine()) return;
        String note = sc.nextLine().trim();
        if (note.isEmpty()) note = "Reviewed uploaded proof.";

        try {
            VerificationCheck check = platform.verifyClaim(target.getId(), "Verifier-01", method, approved, note, 86400);
            System.out.println("  Saved: " + check);
            Supplier updated = platform.findById(target.getSupplierId());
            if (updated != null) {
                System.out.println("  Updated: " + updated);
            }
        } catch (SupifyException e) {
            System.out.println("  [ERROR] " + e.getMessage());
        }
    }

    private static Supplier pickSupplier(Scanner sc, SupifyPlatform platform, String label) {
        List<Supplier> all = platform.findAll();
        all.sort((a, b) -> a.getId().compareTo(b.getId()));
        System.out.println();
        for (int i = 0; i < all.size(); i++) {
            Supplier s = all.get(i);
            System.out.println("  [" + (i + 1) + "] " + s.getId() + " | " + s.getName() + " (" + s.getTrustBand() + ")");
        }
        int sel = readInt(sc, label, 1);
        if (sel == -2) return null;
        int idx = sel - 1;
        if (idx >= 0 && idx < all.size()) {
            return all.get(idx);
        }
        System.out.println("  Invalid selection.");
        return null;
    }

    private static void printSuppliers(List<Supplier> list) {
        if (list.isEmpty()) {
            System.out.println("  No matching suppliers.");
            return;
        }
        System.out.println("\n  # | ID | SUPPLIER | CITY | CATEGORY | BAND (SCORE)");
        System.out.println("  ------------------------------------------------------------------------");
        for (int i = 0; i < list.size(); i++) {
            System.out.println("  " + (i + 1) + " | " + list.get(i));
        }
    }

    private static void printHeader(String title) {
        System.out.println("\n========================================================================");
        System.out.println("  " + title);
        System.out.println("========================================================================");
    }

    private static void showAlerts(SupifyPlatform platform) {
        for (String alert : platform.getAndClearAlerts()) {
            System.out.println("\n  [ALERT] " + alert);
        }
    }

    private static int readInt(Scanner sc, String prompt, int defaultVal) {
        if (defaultVal >= 0) {
            System.out.print(prompt + " [" + defaultVal + "]: ");
        } else {
            System.out.print(prompt + ": ");
        }
        System.out.flush();
        if (!sc.hasNextLine()) {
            return -2;
        }
        String input = sc.nextLine().trim();
        if (input.isEmpty()) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
