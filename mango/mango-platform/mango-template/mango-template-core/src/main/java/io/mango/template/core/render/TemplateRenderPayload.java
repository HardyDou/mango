package io.mango.template.core.render;

import io.mango.template.api.command.TemplateVariableCommand;
import io.mango.template.api.enums.TemplateOutputFormat;
import io.mango.template.api.enums.TemplateSourceFormat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板渲染入参。
 */
public record TemplateRenderPayload(
        TemplateSourceFormat sourceFormat,
        TemplateOutputFormat outputFormat,
        String content,
        byte[] sourceBytes,
        String sourceFileName,
        Map<String, Object> variables,
        List<TemplateVariableCommand> variableDefinitions) {

    public TemplateRenderPayload {
        if (sourceBytes != null) {
            sourceBytes = sourceBytes.clone();
        }
        if (variables == null) {
            variables = new LinkedHashMap<>();
        } else {
            variables = new LinkedHashMap<>(variables);
        }
        if (variableDefinitions == null) {
            variableDefinitions = new ArrayList<>();
        } else {
            variableDefinitions = new ArrayList<>(variableDefinitions);
        }
    }

    public TemplateRenderPayload(TemplateSourceFormat sourceFormat,
                                 TemplateOutputFormat outputFormat,
                                 String content,
                                 byte[] sourceBytes,
                                 String sourceFileName,
                                 Map<String, Object> variables) {
        this(sourceFormat, outputFormat, content, sourceBytes, sourceFileName, variables, List.of());
    }

    @Override
    public byte[] sourceBytes() {
        if (sourceBytes == null) {
            return null;
        }
        return sourceBytes.clone();
    }

    @Override
    public Map<String, Object> variables() {
        return new LinkedHashMap<>(variables);
    }

    @Override
    public List<TemplateVariableCommand> variableDefinitions() {
        return new ArrayList<>(variableDefinitions);
    }
}
