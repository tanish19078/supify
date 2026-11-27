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

// Main entry point and user-friendly console menus for Supify
public class Main {
    private static final File CSV_FILE = new File("data/suppliers.csv");
    private static final File LOG_FILE = new File("data/audit.log");

    public static void main(String[] args) {
        SupifyPlatform platform = new SupifyPlatform();
        System.out.println("\n" + platform.loadStartupData(CSV_FILE));

        DecayMonitorThread monitor = new DecayMonitorThread(platform);
        monitor.start();

        try (Scanner sc = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                showAlerts(platform);
                printHeader("SUPIFY :: Supplier Discovery & Verification CLI");
                System.out.println("  [1] Buyer Menu       (Browse, Search, Inspect, Compare)");
                System.out.println("  [2] Supplier Menu    (Add, Update, Delete, Submit Claim)");
                System.out.println("  [3] Verifier Menu    (Review Pending Claims, Approve/Reject)");
                System.out.println("  [4] Files & Monitor  (Expiry Thread, CSV Save/Load, Audit Log, Reset)");
                System.out.println("  [0] Save & Exit");

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
                        saveCsv(platform);
                        monitor.stopMonitor();
                        System.out.println("\nSaved catalog to " + CSV_FILE.getPath() + ". Goodbye!");
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
            System.out.println("  [1] List All Suppliers");
            System.out.println("  [2] Search & Sort Suppliers");
            System.out.println("  [3] Inspect Supplier & Claims");
            System.out.println("  [4] Compare Two Suppliers");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice [0-4]", -1);
            if (ch == -2 || ch == 0) {
                break;
            }
            if (ch == 1) {
                printSuppliers(platform.search("", null, "score"));
            } else if (ch == 2) {
                String kw = readLine(sc, "Keyword / city / category (ENTER for all)");
                if (kw == null) break;
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
        String activeId = resolveActiveSupplier(platform, "SUP-1");
        while (true) {
            showAlerts(platform);
            activeId = resolveActiveSupplier(platform, activeId);
            Supplier active = (activeId == null) ? null : platform.findById(activeId);
            String title = (active != null)
                    ? "SUPPLIER MENU (Active: " + active.getId() + " - " + active.getName() + ")"
                    : "SUPPLIER MENU (Active: None - Add a supplier first)";
            printHeader(title);
            System.out.println("  [1] Switch Active Supplier");
            System.out.println("  [2] Add New Supplier");
            System.out.println("  [3] Update Active Supplier");
            System.out.println("  [4] Delete a Supplier");
            System.out.println("  [5] Submit Claim + Proof Document");
            System.out.println("  [6] View Active Expiry Timers");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice [0-6]", -1);
            if (ch == -2 || ch == 0) {
                break;
            }
            if (ch == 1) {
                Supplier picked = pickSupplier(sc, platform, "Select active supplier");
                if (picked != null) {
                    activeId = picked.getId();
                    System.out.println("  Switched to " + picked.getId() + " (" + picked.getName() + ").");
                }
            } else if (ch == 2) {
                String name = readLine(sc, "Supplier name");
                if (name == null) break;
                String city = readLine(sc, "City");
                if (city == null) break;
                String category = readLine(sc, "Category");
                if (category == null) break;
                int cap = readInt(sc, "Monthly capacity (units)", -1);
                if (cap == -2) break;
                int moq = readInt(sc, "Minimum order qty (MOQ)", -1);
                if (moq == -2) break;
                String lead = readLine(sc, "Lead time [14 days]");
                if (lead == null) break;
                try {
                    Supplier created = platform.addSupplier(name, city, category, cap, moq, lead);
                    activeId = created.getId();
                    saveCsv(platform);
                    logAction("Added supplier " + created.getId() + " (" + created.getName() + ")");
                    System.out.println("  Added " + created.getId() + ": " + created.getName()
                            + " (saved to " + CSV_FILE.getPath() + ").");
                } catch (SupifyException e) {
                    System.out.println("  [ERROR] " + e.getMessage());
                }
            } else if (ch == 3) {
                if (active == null) {
                    System.out.println("  No active supplier selected.");
                    continue;
                }
                String name = readLine(sc, "Name [" + active.getName() + "] (ENTER to keep)");
                if (name == null) break;
                String city = readLine(sc, "City [" + active.getCity() + "] (ENTER to keep)");
                if (city == null) break;
                String cat = readLine(sc, "Category [" + active.getCategory() + "] (ENTER to keep)");
                if (cat == null) break;
                int cap = readIntOrKeep(sc, "Capacity [" + active.getCapacityPerMonth() + "] (ENTER to keep)");
                if (cap == -2) break;
                int moq = readIntOrKeep(sc, "MOQ [" + active.getMoq() + "] (ENTER to keep)");
                if (moq == -2) break;
                String lead = readLine(sc, "Lead time [" + active.getLeadTime() + "] (ENTER to keep)");
                if (lead == null) break;
                try {
                    Supplier updated = platform.updateSupplier(active.getId(), name, city, cat, cap, moq, lead);
                    saveCsv(platform);
                    logAction("Updated supplier " + updated.getId() + " (" + updated.getName() + ")");
                    System.out.println("  Updated: " + updated);
                } catch (SupifyException e) {
                    System.out.println("  [ERROR] " + e.getMessage());
                }
            } else if (ch == 4) {
                Supplier target = pickSupplier(sc, platform, "Select supplier to delete");
                if (target == null) continue;
                String confirm = readLine(sc, "Delete " + target.getId() + " (" + target.getName() + ")? (Y/N) [N]");
                if (confirm == null) break;
                if (confirm.equalsIgnoreCase("Y")) {
                    if (platform.deleteSupplier(target.getId())) {
                        saveCsv(platform);
                        logAction("Deleted supplier " + target.getId() + " (" + target.getName() + ")");
                        System.out.println("  Deleted " + target.getId() + ".");
                        if (target.getId().equals(activeId)) {
                            activeId = null;
                        }
                    }
                } else {
                    System.out.println("  Delete cancelled.");
                }
            } else if (ch == 5) {
                if (active == null) {
                    System.out.println("  Please add or select a supplier first.");
                    continue;
                }
                String label = readLine(sc, "Claim title (e.g. ISO 14001)");
                if (label == null) break;
                String pillar = readLine(sc, "Pillar [IDENTITY / LEGAL / QUALITY / CAPABILITY]");
                if (pillar == null) break;
                pillar = pillar.toUpperCase();
                if (pillar.isEmpty()) pillar = "QUALITY";
                String val = readLine(sc, "Certificate / value");
                if (val == null) break;
                try {
                    Claim c = platform.submitClaim(active.getId(), label.toLowerCase().replace(" ", "_"),
                            label, pillar, val, label + " PDF", "PDF", "docs/" + label + ".pdf");
                    logAction("Submitted claim " + c.getId() + " for " + active.getId());
                    System.out.println("  Submitted " + c.getId() + " with PDF evidence for verifier review.");
                } catch (SupifyException e) {
                    System.out.println("  [ERROR] " + e.getMessage());
                }
            } else if (ch == 6) {
                if (active == null) {
                    System.out.println("  No active supplier selected.");
                    continue;
                }
                System.out.println("\n  Active Timers for " + active.getId() + ":");
                int count = 0;
                for (Claim c : platform.getClaimsForSupplier(active.getId())) {
                    Attestation a = platform.getActiveAttestationForClaim(c.getId());
                    if (a != null) {
                        System.out.println("  - " + c.getLabel() + " -> " + a.getState()
                                + " via " + a.getMethod() + " (verified " + formatTime(a)
                                + ", " + a.secondsLeft() + "s left)");
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
            System.out.println("  [1] View Pending Claims");
            System.out.println("  [2] Approve or Reject Claim");
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
            printHeader("FILES & MONITOR MENU");
            System.out.println("  [1] Background Thread Status");
            System.out.println("  [2] Save & Reload CSV Now");
            System.out.println("  [3] View Audit Log");
            System.out.println("  [4] Reset to 5 Default Sample Suppliers");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice [0-4]", -1);
            if (ch == -2 || ch == 0) {
                break;
            }
            if (ch == 1) {
                monitor.printThreadInfo();
            } else if (ch == 2) {
                try {
                    FileHelper.exportToCsv(CSV_FILE, platform.findAll());
                    List<Supplier> imported = FileHelper.importFromCsv(CSV_FILE);
                    List<String> names = new ArrayList<>();
                    for (Supplier s : imported) {
                        names.add(s.getName());
                    }
                    System.out.println("  Saved & reloaded " + imported.size() + " suppliers from "
                            + CSV_FILE.getPath() + ": " + names);
                } catch (IOException e) {
                    System.out.println("  [ERROR] CSV error: " + e.getMessage());
                }
            } else if (ch == 3) {
                try {
                    List<String> lines = FileHelper.readAuditLog(LOG_FILE);
                    System.out.println("\n--- " + LOG_FILE.getPath() + " ---");
                    if (lines.isEmpty()) {
                        System.out.println("  (Audit log is empty yet.)");
                    } else {
                        for (String line : lines) {
                            System.out.println("  " + line);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("  [ERROR] Audit log error: " + e.getMessage());
                }
            } else if (ch == 4) {
                String confirm = readLine(sc, "Reset all suppliers to default 5? (Y/N) [N]");
                if (confirm == null) break;
                if (confirm.equalsIgnoreCase("Y")) {
                    platform.resetToSampleData(CSV_FILE);
                    logAction("Reset catalog to 5 default sample suppliers.");
                    System.out.println("  Reset complete. Catalog restored to 5 default suppliers.");
                } else {
                    System.out.println("  Reset cancelled.");
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
                        + " via " + a.getMethod() + ", verified " + formatTime(a)
                        + ", " + a.secondsLeft() + "s left]");
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

        String ans = readLine(sc, "Approve? (Y/N) [Y]");
        if (ans == null) return;
        boolean approved = ans.isEmpty() || ans.equalsIgnoreCase("Y");

        String note = readLine(sc, "Note (ENTER for default)");
        if (note == null) return;
        if (note.isEmpty()) note = "Reviewed uploaded proof.";

        try {
            VerificationCheck check = platform.verifyClaim(target.getId(), "Verifier-01", method, approved, note, 86400);
            saveCsv(platform);
            logAction("Verified " + target.getId() + " -> " + check.getOutcome() + " (" + method + ")");
            System.out.println("  Saved: " + check);
            Supplier updated = platform.findById(target.getSupplierId());
            if (updated != null) {
                System.out.println("  Updated: " + updated);
            }
        } catch (SupifyException e) {
            System.out.println("  [ERROR] " + e.getMessage());
        }
    }

    private static String resolveActiveSupplier(SupifyPlatform platform, String currentId) {
        if (currentId != null && platform.findById(currentId) != null) {
            return currentId;
        }
        if (platform.findById("SUP-1") != null) {
            return "SUP-1";
        }
        List<Supplier> all = platform.findAll();
        if (all.isEmpty()) {
            return null;
        }
        all.sort((a, b) -> a.getId().compareTo(b.getId()));
        return all.get(0).getId();
    }

    private static Supplier pickSupplier(Scanner sc, SupifyPlatform platform, String label) {
        List<Supplier> all = platform.findAll();
        if (all.isEmpty()) {
            System.out.println("  No suppliers in catalog yet.");
            return null;
        }
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

    private static void saveCsv(SupifyPlatform platform) {
        try {
            FileHelper.exportToCsv(CSV_FILE, platform.findAll());
        } catch (IOException e) {
            System.out.println("  [ERROR] Could not save CSV: " + e.getMessage());
        }
    }

    private static void logAction(String message) {
        try {
            FileHelper.appendAuditLog(LOG_FILE, message);
        } catch (IOException e) {
            System.out.println("  [ERROR] Could not write log: " + e.getMessage());
        }
    }

    private static String formatTime(Attestation a) {
        return a.getVerifiedAt().toString().substring(0, 19).replace('T', ' ');
    }

    private static String readLine(Scanner sc, String prompt) {
        System.out.print(prompt + ": ");
        System.out.flush();
        if (!sc.hasNextLine()) {
            return null;
        }
        return sc.nextLine().trim();
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

    private static int readIntOrKeep(Scanner sc, String prompt) {
        String input = readLine(sc, prompt);
        if (input == null) {
            return -2;
        }
        if (input.isEmpty()) {
            return 0;
        }
        try {
            int val = Integer.parseInt(input);
            if (val > 0) {
                return val;
            }
        } catch (NumberFormatException ignored) {
        }
        System.out.println("  Invalid positive number; keeping existing value.");
        return 0;
    }
}
