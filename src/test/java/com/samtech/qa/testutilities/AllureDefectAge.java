package com.samtech.qa.testutilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * 1️⃣ Parses Allure Results and History
 * 2️⃣ Calculates Defect Age (Consecutive Failures)
 * 3️⃣ Generates CSV with Name, Class, Age, Error Message, and Trace
 */
public class AllureDefectAge {

    private static final String ALLURE_RESULTS_DIR = "target/allure-results";
    private static final String HISTORY_JSON = "target/allure-results/history/history.json";
    private static final String OUTPUT_CSV = "target/defect-age-report.csv";

    public static void main(String[] args) throws IOException {
        File resultsDir = new File(ALLURE_RESULTS_DIR);
        if (!resultsDir.exists()) {
            System.err.println("Allure results directory not found!");
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        File[] resultFiles = resultsDir.listFiles((dir, name) -> name.endsWith("-result.json"));
        if (resultFiles == null || resultFiles.length == 0) return;

        // Step 1: Collect all current failures/defects
        Map<String, DefectInfo> currentDefects = new HashMap<>();
        for (File file : resultFiles) {
            JsonNode root = mapper.readTree(file);
            String status = root.path("status").asText("").toLowerCase();

            if (status.equals("failed") || status.equals("broken")) {
                String historyId = root.path("historyId").asText(null);
                if (historyId != null) {
                    currentDefects.put(historyId, new DefectInfo(
                            root.path("fullName").asText("N/A"),
                            root.path("name").asText("N/A"),
                            root.path("statusDetails").path("message").asText("No Error Message"),
                            root.path("statusDetails").path("trace").asText("No Trace")
                    ));
                }
            }
        }

        // Step 2: Read history.json to calculate the "Age"
        File historyFile = new File(HISTORY_JSON);
        JsonNode historyRoot = historyFile.exists() ? mapper.readTree(historyFile) : null;

        // Step 3: Write the CSV Report
        try (FileWriter writer = new FileWriter(OUTPUT_CSV)) {
            writer.append("Class_Name,Test_Name,Defect_Age_Builds,Error_Message,Short_Trace\n");

            for (Map.Entry<String, DefectInfo> entry : currentDefects.entrySet()) {
                String id = entry.getKey();
                DefectInfo info = entry.getValue();
                int age = 1; // Current run is the 1st build in the failure streak

                if (historyRoot != null && historyRoot.has(id)) {
                    List<JsonNode> historyItems = new ArrayList<>();
                    historyRoot.get(id).path("items").forEach(historyItems::add);

                    // Sort by time descending (latest first)
                    historyItems.sort((a, b) -> Long.compare(
                            b.path("time").path("stop").asLong(0),
                            a.path("time").path("stop").asLong(0)
                    ));

                    for (JsonNode item : historyItems) {
                        String histStatus = item.path("status").asText("").toLowerCase();
                        if (histStatus.equals("failed") || histStatus.equals("broken")) {
                            age++;
                        } else {
                            break; // Streak broken by a pass
                        }
                    }
                }

                // Sanitize text for CSV (remove commas and newlines)
                String safeMsg = info.errorMsg.replace(",", ";").replace("\n", " ");
                String safeTrace = info.stackTrace.split("\n")[0].replace(",", ";"); // Get first line of trace

                writer.append(info.className).append(",")
                        .append(info.testName).append(",")
                        .append(String.valueOf(age)).append(",")
                        .append(safeMsg).append(",")
                        .append(safeTrace).append("\n");
            }
        }

        System.out.println("====== DEFECT AGE REPORT GENERATED: " + OUTPUT_CSV + " ======");
    }

    // Helper record to store defect details
    private record DefectInfo(String className, String testName, String errorMsg, String stackTrace) {}
}
