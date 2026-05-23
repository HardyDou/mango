package io.mango.infra.fileproc.render.service;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.config.ConfigureBuilder;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import io.mango.common.result.Require;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.command.RenderVariableDefinition;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.vo.RenderResultVO;

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
public class DocxRenderProvider implements IRenderProvider {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final PlaceholderRenderEngine extractor;

    public DocxRenderProvider(PlaceholderRenderEngine extractor) {
        this.extractor = extractor;
    }

    @Override
    public boolean supports(RenderFormat sourceFormat, RenderFormat targetFormat) {
        return sourceFormat == RenderFormat.DOCX && targetFormat == RenderFormat.DOCX;
    }

    @Override
    public RenderResultVO render(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        byte[] sourceBytes = readAllBytes(command);
        if (sourceBytes.length == 0) {
            throw new RenderToolException("DOCX 模板文件不能为空");
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(sourceBytes);
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             XWPFTemplate template = XWPFTemplate.compile(input, configure(command.variableDefinitions()))) {
            template.render(command.variables());
            template.write(output);
            return RenderResultVO.builder()
                    .format(RenderFormat.DOCX)
                    .fileName(normalizeBaseName(command.fileName()) + "-rendered.docx")
                    .contentType(CONTENT_TYPE)
                    .content(output.toByteArray())
                    .build();
        } catch (IOException ex) {
            throw new RenderToolException("DOCX 模板渲染失败", ex);
        } catch (RuntimeException ex) {
            throw new RenderToolException("DOCX 模板渲染失败：" + ex.getMessage(), ex);
        }
    }

    @Override
    public List<String> extractVariables(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        return extractor.extract(readOoxmlText(readAllBytes(command)));
    }

    private Configure configure(List<RenderVariableDefinition> definitions) {
        ConfigureBuilder builder = Configure.builder().useSpringEL();
        for (String path : arrayVariablePaths(definitions)) {
            builder.bind(path, new LoopRowTableRenderPolicy(true));
        }
        return builder.build();
    }

    private List<String> arrayVariablePaths(List<RenderVariableDefinition> definitions) {
        List<String> paths = new ArrayList<>();
        collectArrayVariablePaths(definitions, "", paths);
        return paths;
    }

    private void collectArrayVariablePaths(List<RenderVariableDefinition> definitions,
                                           String parentPath,
                                           List<String> paths) {
        if (definitions == null || definitions.isEmpty()) {
            return;
        }
        for (RenderVariableDefinition definition : definitions) {
            String path = variablePath(parentPath, definition.name());
            if (path.isBlank()) {
                continue;
            }
            String type = definition.type() == null ? "" : definition.type().trim().toUpperCase(Locale.ROOT);
            if ("ARRAY".equals(type)) {
                paths.add(path);
            }
            collectArrayVariablePaths(definition.children(), path, paths);
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
            throw new RenderToolException("DOCX 模板文件读取失败", ex);
        }
    }

    private boolean isXmlEntry(String name) {
        return name != null && (name.endsWith(".xml") || name.endsWith(".rels"));
    }

    private byte[] readAllBytes(RenderCommand command) {
        try {
            return command.inputStream().readAllBytes();
        } catch (IOException ex) {
            throw new RenderToolException("读取 DOCX 模板失败", ex);
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
