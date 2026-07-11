package io.mango.architecture;

import java.util.Objects;

/** A stable architecture violation emitted by one of the local engines. */
public record ArchitectureIssue(String ruleId, String subject, String message) {

    public ArchitectureIssue {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(message, "message");
    }
}
