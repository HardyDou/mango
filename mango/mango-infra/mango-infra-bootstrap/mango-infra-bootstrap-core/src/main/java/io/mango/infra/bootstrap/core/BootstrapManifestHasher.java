package io.mango.infra.bootstrap.core;

import io.mango.infra.bootstrap.api.BootstrapStep;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

public final class BootstrapManifestHasher {

    public String fingerprint(String releaseId, String buildRevision, List<BootstrapStep> steps) {
        MessageDigest digest = sha256();
        update(digest, releaseId);
        update(digest, buildRevision);
        steps.stream()
                .sorted(Comparator.comparing(BootstrapStep::code))
                .forEach(step -> {
                    update(digest, step.code());
                    update(digest, step.phase().name());
                    step.dependencies().stream().sorted().forEach(value -> update(digest, value));
                    step.optionalDependencies().stream().sorted()
                            .forEach(value -> update(digest, "optional:" + value));
                    update(digest, step.fingerprintMaterial());
                });
        return HexFormat.of().formatHex(digest.digest());
    }

    public String stepFingerprint(BootstrapStep step) {
        return fingerprint("step:" + step.code(), step.phase().name(), List.of(step));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
