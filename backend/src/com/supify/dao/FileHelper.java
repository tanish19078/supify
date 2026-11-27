package com.supify.dao;

import com.supify.model.Supplier;
import com.supify.model.TrustBand;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Reads and writes supplier CSV files and the audit log using FileReader/FileWriter
public class FileHelper {

    public static void exportToCsv(File file, List<Supplier> suppliers) throws IOException {
        ensureParentDir(file);
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("id,name,city,category,capacity,moq,leadTime,trustBand,trustScore");
            bw.newLine();
            for (Supplier s : suppliers) {
                double score = Math.round(s.getTrustScore() * 100.0) / 100.0;
                String row = clean(s.getId()) + ","
                        + clean(s.getName()) + ","
                        + clean(s.getCity()) + ","
                        + clean(s.getCategory()) + ","
                        + s.getCapacityPerMonth() + ","
                        + s.getMoq() + ","
                        + clean(s.getLeadTime()) + ","
                        + s.getTrustBand().name() + ","
                        + score;
                bw.write(row);
                bw.newLine();
            }
        }
    }

    public static List<Supplier> importFromCsv(File file) throws IOException {
        List<Supplier> loaded = new ArrayList<>();
        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            br.readLine(); // skip header row
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 9) {
                    continue;
                }
                try {
                    Supplier s = new Supplier(
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim(),
                            parts[3].trim(),
                            Integer.parseInt(parts[4].trim()),
                            Integer.parseInt(parts[5].trim()),
                            parts[6].trim()
                    );
                    s.setTrustBand(TrustBand.valueOf(parts[7].trim()));
                    s.setTrustScore(Double.parseDouble(parts[8].trim()));
                    loaded.add(s);
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping bad CSV row: " + line);
                }
            }
        }
        return loaded;
    }

    // Appends one timestamped line to audit.log (true = append mode)
    public static void appendAuditLog(File file, String message) throws IOException {
        ensureParentDir(file);
        String line = "[" + LocalDateTime.now() + "] " + message;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            bw.write(line);
            bw.newLine();
        }
    }

    public static List<String> readAuditLog(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        if (!file.exists()) {
            return lines;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    // Copies a real proof file (e.g. PDF/image) into data/proofs/ using byte streams
    public static File copyProofFile(File sourceFile, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File target = new File(destDir, sourceFile.getName());
        try (FileInputStream in = new FileInputStream(sourceFile);
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return target;
    }

    private static void ensureParentDir(File file) {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", " ").trim();
    }
}
