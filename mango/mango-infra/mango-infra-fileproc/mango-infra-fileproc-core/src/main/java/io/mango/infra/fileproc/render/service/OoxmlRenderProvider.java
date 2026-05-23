package io.mango.infra.fileproc.render.service;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.vo.RenderResultVO;

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
 * Generic OOXML document template renderer.
 */
public class OoxmlRenderProvider implements IRenderProvider {

    private final PlaceholderRenderEngine engine;
    private final RenderFormat sourceFormat;

    public OoxmlRenderProvider(PlaceholderRenderEngine engine, RenderFormat sourceFormat) {
        this.engine = engine;
        this.sourceFormat = sourceFormat;
    }

    @Override
    public boolean supports(RenderFormat sourceFormat, RenderFormat targetFormat) {
        return this.sourceFormat == sourceFormat
                && ((this.sourceFormat == RenderFormat.DOCX && targetFormat == RenderFormat.DOCX)
                || (this.sourceFormat == RenderFormat.XLSX && targetFormat == RenderFormat.XLSX));
    }

    @Override
    public RenderResultVO render(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        byte[] sourceBytes = readAllBytes(command);
        if (sourceBytes.length == 0) {
            throw new RenderToolException("OOXML 模板文件不能为空");
        }
        String suffix = sourceFormat == RenderFormat.DOCX ? ".docx" : ".xlsx";
        String contentType = sourceFormat == RenderFormat.DOCX
                ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return RenderResultVO.builder()
                .format(sourceFormat)
                .fileName(normalizeBaseName(command.fileName()) + "-rendered" + suffix)
                .contentType(contentType)
                .content(renderOoxml(sourceBytes, command.variables()))
                .build();
    }

    @Override
    public List<String> extractVariables(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        return engine.extract(readOoxmlText(readAllBytes(command)));
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
            throw new RenderToolException("OOXML 模板文件渲染失败", ex);
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
            throw new RenderToolException("OOXML 模板文件读取失败", ex);
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

    private byte[] readAllBytes(RenderCommand command) {
        try {
            return command.inputStream().readAllBytes();
        } catch (IOException ex) {
            throw new RenderToolException("读取 OOXML 模板失败", ex);
        }
    }

    private String normalizeBaseName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "template";
        }
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }
}
