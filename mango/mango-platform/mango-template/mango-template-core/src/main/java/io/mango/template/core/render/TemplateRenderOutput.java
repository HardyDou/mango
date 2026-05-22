package io.mango.template.core.render;

/**
 * 模板渲染输出。
 *
 * @param content 文本类内容
 * @param fileBytes 文档类字节
 * @param fileName 文件名
 * @param contentType 内容类型
 */
public record TemplateRenderOutput(String content, byte[] fileBytes, String fileName, String contentType) {
}
