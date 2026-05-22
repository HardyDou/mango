package io.mango.template.core.render;

import io.mango.template.api.command.TemplateVariableDefinition;
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
        List<TemplateVariableDefinition> variableDefinitions) {

    public TemplateRenderPayload {
        variables = variables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(variables);
        variableDefinitions = variableDefinitions == null ? new ArrayList<>() : new ArrayList<>(variableDefinitions);
    }

    public TemplateRenderPayload(TemplateSourceFormat sourceFormat,
                                 TemplateOutputFormat outputFormat,
                                 String content,
                                 byte[] sourceBytes,
                                 String sourceFileName,
                                 Map<String, Object> variables) {
        this(sourceFormat, outputFormat, content, sourceBytes, sourceFileName, variables, List.of());
    }
}
