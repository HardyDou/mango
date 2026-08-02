package io.mango.infra.bootstrap.starter;

record BootstrapStableReceipt(
        String environmentKey,
        String databaseName,
        String releaseId,
        String buildRevision,
        long stableGeneration,
        String stableFingerprint,
        String state) {
}
