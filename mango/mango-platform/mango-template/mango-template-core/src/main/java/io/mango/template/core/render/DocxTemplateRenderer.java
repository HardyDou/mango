package io.mango.template.core.render;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.config.ConfigureBuilder;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import io.mango.common.exception.BizException;
import io.mango.template.api.TemplateCode;
import io.mango.template.api.command.TemplateVariableDefinition;
import io.mango.template.api.enums.TemplateOutputFormat;
import io.mango.template.api.enums.TemplateSourceFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * DOCX template renderer based on poi-tl.
 */
public class DocxTemplateRenderer implements TemplateRenderer {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final PlaceholderTemplateEngine extractor;

    public DocxTemplateRenderer(PlaceholderTemplateEngine extractor) {
        this.extractor = extractor;
    }

    @Override
    public boolean supports(TemplateSourceFormat sourceFormat) {
        return TemplateSourceFormat.DOCX == sourceFormat;
    }

    @Override
    public boolean supportsOutput(TemplateOutputFormat outputFormat) {
        return TemplateOutputFormat.DOCX == outputFormat;
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
        try (ByteArrayInputStream input = new ByteArrayInputStream(payload.sourceBytes());
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             XWPFTemplate template = XWPFTemplate.compile(input, configure(payload.variableDefinitions()))) {
            template.render(payload.variables());
            template.write(output);
            return new TemplateRenderOutput(null, output.toByteArray(),
                    normalizeBaseName(payload.sourceFileName()) + "-rendered.docx", CONTENT_TYPE);
        } catch (IOException ex) {
            throw new BizException(TemplateCode.TEMPLATE_RENDER_FAILED.getCode(), "DOCX 模板渲染失败", ex);
        } catch (RuntimeException ex) {
            throw new BizException(TemplateCode.TEMPLATE_RENDER_FAILED.getCode(), "DOCX 模板渲染失败：" + ex.getMessage(), ex);
        }
    }

    @Override
    public List<String> extractVariables(TemplateRenderPayload payload) {
        if (payload.content() != null) {
            return extractor.extract(payload.content());
        }
        if (payload.sourceBytes() != null) {
            return extractor.extract(readOoxmlText(payload.sourceBytes()));
        }
        return List.of();
    }

    private Configure configure(List<TemplateVariableDefinition> definitions) {
        ConfigureBuilder builder = Configure.builder().useSpringEL();
        for (String path : arrayVariablePaths(definitions)) {
            builder.bind(path, new LoopRowTableRenderPolicy(true));
        }
        return builder.build();
    }

    private List<String> arrayVariablePaths(List<TemplateVariableDefinition> definitions) {
        List<String> paths = new ArrayList<>();
        collectArrayVariablePaths(definitions, "", paths);
        return paths;
    }

    private void collectArrayVariablePaths(List<TemplateVariableDefinition> definitions, String parentPath, List<String> paths) {
        if (definitions == null || definitions.isEmpty()) {
            return;
        }
        for (TemplateVariableDefinition definition : definitions) {
            String path = variablePath(parentPath, definition.getName());
            if (path.isBlank()) {
                continue;
            }
            String type = definition.getType() == null ? "" : definition.getType().trim().toUpperCase(Locale.ROOT);
            if ("ARRAY".equals(type)) {
                paths.add(path);
            }
            collectArrayVariablePaths(definition.getChildren(), path, paths);
        }
    }

    private String variablePath(String parentPath, String name) {
        String current = name == null ? "" : name.trim();
        if (parentPath == null || parentPath.isBlank()) {
            return current;
        }
        if (current.isBlank()) {
            return parentPath;
        }
        if (current.startsWith(parentPath + ".")) {
            return current;
        }
        return parentPath + "." + current;
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
            throw new BizException(TemplateCode.TEMPLATE_FILE_NOT_FOUND.getCode(), "DOCX 模板文件读取失败", ex);
        }
    }

    private boolean isXmlEntry(String name) {
        return name != null && (name.endsWith(".xml") || name.endsWith(".rels"));
    }

    private String normalizeBaseName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "template";
        }
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }
}
