package io.mango.template.core.render;

import io.mango.common.exception.BizException;
import io.mango.template.api.TemplateCode;
import io.mango.template.api.enums.TemplateOutputFormat;
import io.mango.template.api.enums.TemplateSourceFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * OOXML 二进制文档模板渲染器。
 */
public class BinaryTemplateRenderer implements TemplateRenderer {

    private final PlaceholderTemplateEngine engine;
    private final TemplateSourceFormat sourceFormat;

    public BinaryTemplateRenderer(PlaceholderTemplateEngine engine, TemplateSourceFormat sourceFormat) {
        this.engine = engine;
        this.sourceFormat = sourceFormat;
    }

    @Override
    public boolean supports(TemplateSourceFormat sourceFormat) {
        return this.sourceFormat == sourceFormat;
    }

    @Override
    public boolean supportsOutput(TemplateOutputFormat outputFormat) {
        return (TemplateSourceFormat.DOCX == sourceFormat && TemplateOutputFormat.DOCX == outputFormat)
                || (TemplateSourceFormat.XLSX == sourceFormat && TemplateOutputFormat.XLSX == outputFormat);
    }

    @Override
    public TemplateRenderOutput render(TemplateRenderPayload payload) {
        if (!supportsOutput(payload.outputFormat())) {
            throw new BizException(TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getCode(),
                    TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getMessage());
        }
        if (payload.sourceBytes() == null || payload.sourceBytes().length == 0) {
            throw new BizException(TemplateCode.TEMPLATE_FILE_NOT_FOUND.getCode(),
                    TemplateCode.TEMPLATE_FILE_NOT_FOUND.getMessage());
        }
        String suffix = TemplateSourceFormat.DOCX == sourceFormat ? ".docx" : ".xlsx";
        String contentType = TemplateSourceFormat.DOCX == sourceFormat
                ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String fileName = normalizeBaseName(payload.sourceFileName()) + "-rendered" + suffix;
        return new TemplateRenderOutput(null, renderOoxml(payload.sourceBytes(), payload.variables()), fileName, contentType);
    }

    @Override
    public List<String> extractVariables(TemplateRenderPayload payload) {
        if (payload.content() != null) {
            return engine.extract(payload.content());
        }
        if (payload.sourceBytes() != null) {
            return engine.extract(readOoxmlText(payload.sourceBytes()));
        }
        return List.of();
    }

    private byte[] renderOoxml(byte[] sourceBytes, Map<String, Object> variables) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(sourceBytes);
             ZipInputStream zipInput = new ZipInputStream(input);
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                ZipEntry outputEntry = new ZipEntry(entry.getName());
                outputEntry.setTime(entry.getTime());
                zipOutput.putNextEntry(outputEntry);
                byte[] bytes = zipInput.readAllBytes();
                if (!entry.isDirectory() && isXmlEntry(entry.getName())) {
                    String xml = new String(bytes, StandardCharsets.UTF_8);
                    bytes = engine.render(xml, escapeXmlVariables(variables)).getBytes(StandardCharsets.UTF_8);
                }
                zipOutput.write(bytes);
                zipOutput.closeEntry();
                zipInput.closeEntry();
            }
            zipOutput.finish();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BizException(TemplateCode.TEMPLATE_RENDER_FAILED.getCode(), "模板文件渲染失败");
        }
    }

    private String readOoxmlText(byte[] sourceBytes) {
        StringBuilder text = new StringBuilder();
        try (ByteArrayInputStream input = new ByteArrayInputStream(sourceBytes);
             ZipInputStream zipInput = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                if (!entry.isDirectory() && isXmlEntry(entry.getName())) {
                    text.append(new String(zipInput.readAllBytes(), StandardCharsets.UTF_8)).append('\n');
                }
                zipInput.closeEntry();
            }
            return text.toString();
        } catch (IOException ex) {
            throw new BizException(TemplateCode.TEMPLATE_FILE_NOT_FOUND.getCode(), "模板文件读取失败");
        }
    }

    private boolean isXmlEntry(String name) {
        return name != null && (name.endsWith(".xml") || name.endsWith(".rels"));
    }

    private Map<String, Object> escapeXmlVariables(Map<String, Object> variables) {
        Map<String, Object> escaped = new LinkedHashMap<>();
        if (variables == null) {
            return escaped;
        }
        variables.forEach((key, value) -> escaped.put(key, escapeXmlValue(value)));
        return escaped;
    }

    @SuppressWarnings("unchecked")
    private Object escapeXmlValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> escaped = new LinkedHashMap<>();
            ((Map<String, Object>) map).forEach((key, item) -> escaped.put(key, escapeXmlValue(item)));
            return escaped;
        }
        if (value instanceof CharSequence text) {
            return escapeXml(text.toString());
        }
        return value;
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String normalizeBaseName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "template";
        }
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }
}
