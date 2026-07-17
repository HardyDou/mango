package io.mango.template.core.service;

/**
 * 模板模块读取业务域的本地适配口。
 */
public interface ITemplateDomainProvider {

    TemplateDomainInfo findByCode(String domainCode);
}
