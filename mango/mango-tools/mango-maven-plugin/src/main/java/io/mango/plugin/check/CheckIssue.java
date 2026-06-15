package io.mango.plugin.check;

public class CheckIssue {
    public String type;
    public String severity;
    public String file;
    public int line;
    public String description;
    public String rule;
    public String reference;
    public String source;
    public String fingerprint;
    public boolean inChangedFiles;
    public boolean baseline;

    public CheckIssue() {
    }
}
