package io.mango.resource.support.declaration;

import java.util.Locale;
import java.util.regex.Pattern;

/** Defines the supported FILE_ASSET source and packaged content locations. */
public final class FileAssetContentLocations {

    public static final String ASSET_CLASSPATH_PREFIX = "classpath:META-INF/mango/assets/";
    public static final String EXTERNAL_ASSET_PREFIX = "asset:";
    public static final String PACKAGED_OBJECT_DIRECTORY = "META-INF/mango/files.bundle/objects";
    public static final String PACKAGED_OBJECT_CLASSPATH_PREFIX = "classpath:" + PACKAGED_OBJECT_DIRECTORY + "/";

    private static final Pattern SHA256_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private FileAssetContentLocations() {
    }

    /** Returns the canonical classpath location for a content-addressed packaged object. */
    public static String packagedObject(String sha256) {
        String normalized = normalizedSha256(sha256);
        if (!isSha256(normalized)) {
            throw new IllegalArgumentException("FILE_ASSET sha256 must contain 64 lowercase hex characters");
        }
        return PACKAGED_OBJECT_CLASSPATH_PREFIX + normalized;
    }

    /** Returns whether a location uses the packaged object namespace. */
    public static boolean isPackagedObject(String location) {
        return location != null && location.startsWith(PACKAGED_OBJECT_CLASSPATH_PREFIX);
    }

    /** Returns whether a packaged location points to the object named by the declared digest. */
    public static boolean matchesPackagedObject(String location, String sha256) {
        String normalized = normalizedSha256(sha256);
        return isSha256(normalized) && (PACKAGED_OBJECT_CLASSPATH_PREFIX + normalized).equals(location);
    }

    /** Returns whether a value is a valid SHA-256 digest accepted by the FILE_ASSET protocol. */
    public static boolean isSha256(String value) {
        return value != null && SHA256_PATTERN.matcher(value).matches();
    }

    private static String normalizedSha256(String sha256) {
        return sha256 == null ? null : sha256.trim().toLowerCase(Locale.ROOT);
    }
}
