package io.mango.infra.fileproc.render.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FreeMarker rendering engine for text-like templates.
 */
public class FreemarkerRenderEngine {

    private static final Pattern INTERPOLATION = Pattern.compile("\\$\\{([^}]*)}");
    private static final Pattern LIST_DIRECTIVE = Pattern.compile("<#list\\s+(.+?)\\s+as\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*>");
    private static final Pattern IF_DIRECTIVE = Pattern.compile("<#(?:if|elseif)\\s+([^>]+)>");
    private static final Pattern COMMENT = Pattern.compile("<#--[\\s\\S]*?-->");
    private static final Pattern STRING_LITERAL = Pattern.compile("'([^'\\\\]|\\\\.)*'|\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern BUILTIN = Pattern.compile("\\?[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern VARIABLE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*");
    private static final Set<String> RESERVED_WORDS = Set.of(
            "as", "true", "false", "null",
            "and", "or", "not",
            "gt", "gte", "lt", "lte",
            "eq", "ne",
            "if", "elseif", "else", "list", "items", "sep", "assign", "local", "global", "macro", "function",
            "return", "break", "continue", "switch", "case", "default"
    );

    private final Configuration configuration;

    public FreemarkerRenderEngine() {
        this.configuration = new Configuration(Configuration.VERSION_2_3_34);
        this.configuration.setDefaultEncoding("UTF-8");
        this.configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        this.configuration.setLogTemplateExceptions(false);
        this.configuration.setWrapUncheckedExceptions(true);
        this.configuration.setFallbackOnNullLoopVariable(false);
    }

    public String render(String content, Map<String, Object> variables) {
        if (content == null) {
            return "";
        }
        try {
            Template template = new Template("template", new StringReader(content), configuration);
            StringWriter writer = new StringWriter();
            template.process(variables == null ? Map.of() : variables, writer);
            return writer.toString();
        } catch (TemplateException ex) {
            throw new RenderToolException("模板渲染失败：" + ex.getMessageWithoutStackTop(), ex);
        } catch (IOException ex) {
            throw new RenderToolException("模板解析失败", ex);
        }
    }

    public List<String> extract(String content) {
        if (!hasText(content)) {
            return List.of();
        }
        String cleaned = COMMENT.matcher(content).replaceAll("");
        Set<String> variables = new LinkedHashSet<>();
        Set<String> loopAliases = new LinkedHashSet<>();

        Matcher listMatcher = LIST_DIRECTIVE.matcher(cleaned);
        while (listMatcher.find()) {
            addExpressionVariables(listMatcher.group(1), variables, loopAliases);
            loopAliases.add(listMatcher.group(2));
        }

        Matcher ifMatcher = IF_DIRECTIVE.matcher(cleaned);
        while (ifMatcher.find()) {
            addExpressionVariables(ifMatcher.group(1), variables, loopAliases);
        }

        Matcher interpolationMatcher = INTERPOLATION.matcher(cleaned);
        while (interpolationMatcher.find()) {
            addExpressionVariables(interpolationMatcher.group(1), variables, loopAliases);
        }

        return new ArrayList<>(variables);
    }

    private void addExpressionVariables(String expression, Set<String> variables, Set<String> loopAliases) {
        String normalized = sanitizeExpression(expression);
        Matcher matcher = VARIABLE.matcher(normalized);
        while (matcher.find()) {
            String variable = matcher.group();
            if (shouldKeepVariable(variable, loopAliases)) {
                variables.add(variable);
            }
        }
    }

    private String sanitizeExpression(String expression) {
        String withoutStrings = STRING_LITERAL.matcher(expression).replaceAll(" ");
        return BUILTIN.matcher(withoutStrings).replaceAll(" ");
    }

    private boolean shouldKeepVariable(String variable, Set<String> loopAliases) {
        if (!hasText(variable) || RESERVED_WORDS.contains(variable)) {
            return false;
        }
        int dotIndex = variable.indexOf('.');
        String root = dotIndex > -1 ? variable.substring(0, dotIndex) : variable;
        return !loopAliases.contains(root) && !RESERVED_WORDS.contains(root);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
