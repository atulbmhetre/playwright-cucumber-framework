package com.samtech.qa.testutilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class AllureDefectAge {

    private static final String ALLURE_RESULTS_DIR = "target/allure-results";
    private static final String OUTPUT_CSV = "target/defect-age-report.csv";

    public static void main(String[] args) throws IOException {

        File resultsDir = new File(ALLURE_RESULTS_DIR);

        if (!resultsDir.exists()) {
            System.out.println("Allure results directory not found.");
            return;
        }

        Map<String, TestHistory> testHistoryMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        File[] files = resultsDir.listFiles((dir, name) -> name.endsWith("-result.json"));

        if (files == null) {
            System.out.println("No result files found.");
            return;
        }

        for (File file : files) {

            JsonNode root = mapper.readTree(file);

            String historyId = getSafeText(root, "historyId");
            String fullName = getSafeText(root, "fullName");
            String status = getSafeText(root, "status");

            if (historyId == null || fullName == null) {
                continue;
            }

            // Extract class & test name
            String className;
            String testName;

            if (fullName.contains(".")) {
                int lastDot = fullName.lastIndexOf(".");
                className = fullName.substring(0, lastDot);
                testName = fullName.substring(lastDot + 1);
            } else {
                className = "UnknownClass";
                testName = fullName;
            }

            TestHistory testHistory = testHistoryMap.computeIfAbsent(
                    historyId,
                    k -> new TestHistory(className, testName)
            );

            testHistory.incrementTotalRuns();

            if ("failed".equalsIgnoreCase(status) ||
                    "broken".equalsIgnoreCase(status)) {
                testHistory.incrementDefect();
            }
        }

        writeCsv(testHistoryMap);

        System.out.println("Defect age report generated successfully.");
    }

    private static String getSafeText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText()
                : null;
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