package io.mango.plugin.check;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CheckResult {
    public boolean passed = true;
    public String gate = "all";
    public String gateStatus = "PASS";
    public String staticFailurePolicy = "block";
    public int totalIssueCount;
    public int newIssueCount;
    public int baselineIssueCount;
    public int toolFailureCount;
    public List<String> changedFiles = new ArrayList<>();
    public List<String> gateMessages = new ArrayList<>();
    public List<CheckIssue> issues = new ArrayList<>();
    public List<CheckIssue> newIssues = new ArrayList<>();
    public List<CheckIssue> baselineIssues = new ArrayList<>();
    public List<ToolFailure> toolFailures = new ArrayList<>();
    public Map<String, Integer> issuesBySource = new LinkedHashMap<>();
    public Map<String, Integer> issuesByRule = new LinkedHashMap<>();

    void addIssue(String type, String severity, String file, int line, String description,
                  String rule, String reference, String source) {
        CheckIssue issue = new CheckIssue();
        issue.type = type;
        issue.severity = severity;
        issue.file = file;
        issue.line = line;
        issue.description = description;
        issue.rule = rule;
        issue.reference = reference;
        issue.source = source;
        issues.add(issue);
        passed = false;
    }

    void addToolFailure(String goal, String message) {
        ToolFailure failure = new ToolFailure();
        failure.goal = goal;
        failure.message = message;
        toolFailures.add(failure);
    }

    void addGateMessage(String message) {
        gateMessages.add(message);
    }
}
