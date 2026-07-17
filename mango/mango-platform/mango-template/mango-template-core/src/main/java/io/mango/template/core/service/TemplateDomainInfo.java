package io.mango.template.core.service;

/**
 * 模板模块所需的最小业务域信息。
 */
public record TemplateDomainInfo(String domainCode, String domainName, Integer status) {
}
