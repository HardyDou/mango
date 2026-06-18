package io.mango.resource.core.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 计算资源声明内容 hash。
 */
@RequiredArgsConstructor
public class ResourceContentHasher {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    public String hash(ResourceDeclaration declaration) {
        try {
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("id", declaration.getId());
            normalized.put("version", declaration.getVersion());
            normalized.put("resourceType", declaration.getResourceType());
            normalized.put("moduleCode", declaration.getModuleCode());
            normalized.put("bizKey", declaration.getBizKey());
            normalized.put("name", declaration.getName());
            normalized.put("targetModule", declaration.getTargetModule());
            normalized.put("status", declaration.getStatus());
            normalized.put("fields", normalizeFields(declaration.getFields()));
            return DigestUtils.md5DigestAsHex(objectMapper.writeValueAsBytes(normalized));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Hash resource declaration failed: " + declaration.getBizKey(), e);
        }
    }

    private Map<String, Object> normalizeFields(Map<String, ResourceField> fields) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (fields == null) {
            return normalized;
        }
        for (Map.Entry<String, ResourceField> entry : fields.entrySet()) {
            ResourceField field = entry.getValue();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", field.getType());
            value.put("value", field.getValue());
            value.put("location", field.getLocation());
            value.put("encoding", field.getEncoding());
            value.put("mediaType", field.getMediaType());
            if (field.getType() == ResourceFieldType.FILE) {
                value.put("content", readClasspathContent(field));
            }
            normalized.put(entry.getKey(), value);
        }
        return normalized;
    }

    private String readClasspathContent(ResourceField field) {
        if (!StringUtils.hasText(field.getLocation()) || !field.getLocation().startsWith("classpath:")) {
            throw new IllegalStateException("File resource field only supports classpath location: " + field.getLocation());
        }
        Resource resource = resourceLoader.getResource(field.getLocation());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("Classpath file resource is not readable: " + field.getLocation());
        }
        Charset charset = StringUtils.hasText(field.getEncoding())
                ? Charset.forName(field.getEncoding())
                : StandardCharsets.UTF_8;
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), charset);
        } catch (IOException e) {
            throw new IllegalStateException("Read classpath file resource failed: " + field.getLocation(), e);
        }
    }
}
