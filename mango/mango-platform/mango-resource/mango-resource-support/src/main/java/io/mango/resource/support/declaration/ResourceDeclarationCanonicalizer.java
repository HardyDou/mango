package io.mango.resource.support.declaration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Produces deployment-topology-independent bytes for resource declaration identity checks.
 */
public final class ResourceDeclarationCanonicalizer {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public ResourceDeclarationCanonicalizer(ObjectMapper objectMapper) {
        this(objectMapper, new DefaultResourceLoader());
    }

    ResourceDeclarationCanonicalizer(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    public byte[] canonicalBytes(ResourceDeclaration declaration) {
        try {
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("id", declaration.getId());
            normalized.put("version", declaration.getVersion());
            normalized.put("resourceType", declaration.getResourceType());
            normalized.put("moduleCode", declaration.getModuleCode());
            normalized.put("bizKey", declaration.getBizKey());
            normalized.put("name", declaration.getName());
            normalized.put("targetModule", declaration.getTargetModule());
            normalized.put("syncMode", declaration.getSyncMode());
            normalized.put("executionPhase", declaration.getExecutionPhase());
            normalized.put("status", declaration.getStatus());
            normalized.put("fields", normalizeFields(declaration.getFields()));
            return objectMapper.writeValueAsBytes(normalized);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Canonicalize resource declaration failed: " + declaration.getBizKey(), exception);
        }
    }

    public String fingerprint(ResourceDeclaration declaration) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonicalBytes(declaration)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private Map<String, Object> normalizeFields(Map<String, ResourceField> fields) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (fields == null) {
            return normalized;
        }
        for (Map.Entry<String, ResourceField> entry : new TreeMap<>(fields).entrySet()) {
            ResourceField field = entry.getValue();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", field.getType());
            value.put("value", field.getValue());
            value.put("location", field.getLocation());
            value.put("encoding", field.getEncoding());
            value.put("mediaType", field.getMediaType());
            if (field.getType() == ResourceFieldType.FILE) {
                value.put("content", readClasspathContentFingerprint(field));
            }
            normalized.put(entry.getKey(), value);
        }
        return normalized;
    }

    private Map<String, Object> readClasspathContentFingerprint(ResourceField field) {
        if (!StringUtils.hasText(field.getLocation()) || !field.getLocation().startsWith("classpath:")) {
            throw new IllegalStateException(
                    "File resource field only supports classpath location: " + field.getLocation());
        }
        Resource resource = resourceLoader.getResource(field.getLocation());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("Classpath file resource is not readable: " + field.getLocation());
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = resource.getInputStream();
                 DigestInputStream digestInput = new DigestInputStream(inputStream, digest)) {
                long size = digestInput.transferTo(OutputStream.nullOutputStream());
                return Map.of("sha256", HexFormat.of().formatHex(digest.digest()), "size", size);
            }
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Fingerprint classpath file resource failed: " + field.getLocation(), exception);
        }
    }
}
