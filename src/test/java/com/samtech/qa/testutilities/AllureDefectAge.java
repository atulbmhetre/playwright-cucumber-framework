package com.samtech.qa.testutilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class AllureDefectAge {

    // Default directories
    private static final String ALLURE_RESULTS_DIR = "target/allure-results";
    private static final String OUTPUT_CSV = "target/defect-age-report.csv";

    public static void main(String[] args) throws IOException {

        File resultsDir = new File(ALLURE_RESULTS_DIR);

        if (!resultsDir.exists() || !resultsDir.isDirectory()) {
            System.out.println("Allure results directory not found: " + ALLURE_RESULTS_DIR);
            return;
        }

        // Map<historyId, TestHistory>
        Map<String, TestHistory> testHistoryMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        File[] files = resultsDir.listFiles((dir, name) -> name.endsWith("-result.json"));
        if (files == null || files.length == 0) {
            System.out.println("No result files found in: " + ALLURE_RESULTS_DIR);
            return;
        }

        for (File file : files) {

            JsonNode root = mapper.readTree(file);

            String historyId = getSafeText(root, "historyId");
            String status = getSafeText(root, "status");
            String testName = getSafeText(root, "name"); // scenario/test name
            String fullName = getSafeText(root, "fullName"); // feature or suite name

            if (historyId == null || testName == null) {
                continue; // skip invalid entries
            }

            String className = (fullName != null) ? fullName : "UnknownClass";

            // Aggregate by historyId
            TestHistory testHistory = testHistoryMap.computeIfAbsent(
                    historyId,
                    k -> new TestHistory(className, testName)
            );

            testHistory.incrementTotalRuns();

            if ("failed".equalsIgnoreCase(status) || "broken".equalsIgnoreCase(status)) {
                testHistory.incrementDefect();
            }
        }

        writeCsv(testHistoryMap);
        System.out.println("====== DEFECT AGE REPORT GENERATED ======");
        System.out.println("Defect age report written to: " + OUTPUT_CSV);
    }

    private static String getSafeText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private static void writeCsv(Map<String, TestHistory> map) throws IOException {
        FileWriter writer = new FileWriter(OUTPUT_CSV);
        writer.append("Class Name,Test Name,Defect Count,Total Runs,Defect Age\n");

        for (TestHistory th : map.values()) {
            writer.append(th.getClassName()).append(",")
                    .append(th.getTestName()).append(",")
                    .append(String.valueOf(th.getDefectCount())).append(",")
                    .append(String.valueOf(th.getTotalRuns())).append(",")
                    .append(String.valueOf(th.getDefectAge()))
                    .append("\n");
        }

        writer.flush();
        writer.close();
    }
}


// ================= UPDATED HELPER CLASSES =================

class TestHistory {

    private final String className;
    private final String testName;
    private int defectCount;
    private int totalRuns;

    public TestHistory(String className, String testName) {
        this.className = className;
        this.testName = testName;
        this.defectCount = 0;
        this.totalRuns = 0;
    }

    public void incrementDefect() {
        defectCount++;
    }

    public void incrementTotalRuns() {
        totalRuns++;
    }

    public String getClassName() {
        return className;
    }

    public String getTestName() {
        return testName;
    }

    public int getDefectCount() {
        return defectCount;
    }

    public int getTotalRuns() {
        return totalRuns;
    }

    public int getDefectAge() {
        return defectCount;
    }
}

class StatusEntry {
    private final String status;
    private final long timestamp;

    public StatusEntry(String status, long timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
}

class DefectReport {
    private final String className;
    private final String testName;
    private final int consecutiveFailures;
    private final Date firstFailed;
    private final Date lastFailed;
    private final long ageDays;

    public DefectReport(String className, String testName, int consecutiveFailures,
                        Date firstFailed, Date lastFailed, long ageDays) {
        this.className = className;
        this.testName = testName;
        this.consecutiveFailures = consecutiveFailures;
        this.firstFailed = firstFailed;
        this.lastFailed = lastFailed;
        this.ageDays = ageDays;
    }

    public String getClassName() { return className; }
    public String getTestName() { return testName; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public Date getFirstFailed() { return firstFailed; }
    public Date getLastFailed() { return lastFailed; }
    public long getAgeDays() { return ageDays; }
}