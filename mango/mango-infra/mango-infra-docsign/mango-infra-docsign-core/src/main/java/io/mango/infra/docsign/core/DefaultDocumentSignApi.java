package io.mango.infra.docsign.core;

import io.mango.common.result.Require;
import io.mango.infra.docsign.DocumentSignApi;
import io.mango.infra.docsign.command.DocumentSignCommand;
import io.mango.infra.docsign.command.DocumentVerifyCommand;
import io.mango.infra.docsign.enums.DocumentSignFormat;
import io.mango.infra.docsign.spi.IDocumentSignProvider;
import io.mango.infra.docsign.vo.DocumentSignResultVO;
import io.mango.infra.docsign.vo.DocumentSignStreamResultVO;
import io.mango.infra.docsign.vo.DocumentVerifyResultVO;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Default local document signature API routing calls by document format.
 */
public final class DefaultDocumentSignApi implements DocumentSignApi {

    private final List<IDocumentSignProvider> providers;

    public DefaultDocumentSignApi(List<IDocumentSignProvider> providers) {
        Require.isTrue(providers != null && !providers.isEmpty(), "文档签章 provider 不能为空");
        this.providers = List.copyOf(providers);
    }

    @Override
    public DocumentSignResultVO sign(DocumentSignCommand command) {
        Require.notNull(command, "签章命令不能为空");
        Require.isTrue(command.hasContent(), "待签文档不能为空");
        return provider(command.format()).sign(command);
    }

    @Override
    public DocumentSignStreamResultVO sign(DocumentSignCommand command,
                                           InputStream document,
                                           OutputStream signedDocument) {
        Require.notNull(command, "签章命令不能为空");
        Require.notNull(document, "待签文档流不能为空");
        Require.notNull(signedDocument, "签名结果流不能为空");
        return provider(command.format()).sign(command, document, signedDocument);
    }

    @Override
    public DocumentVerifyResultVO verify(DocumentVerifyCommand command) {
        Require.notNull(command, "验签命令不能为空");
        Require.isTrue(command.hasContent(), "验签文档不能为空");
        return provider(command.format()).verify(command);
    }

    @Override
    public DocumentVerifyResultVO verify(DocumentVerifyCommand command, InputStream document) {
        Require.notNull(command, "验签命令不能为空");
        Require.notNull(document, "验签文档流不能为空");
        return provider(command.format()).verify(command, document);
    }

    @Override
    public Set<DocumentSignFormat> supportedFormats() {
        EnumSet<DocumentSignFormat> formats = EnumSet.noneOf(DocumentSignFormat.class);
        for (DocumentSignFormat format : DocumentSignFormat.values()) {
            if (providers.stream().anyMatch(provider -> provider.supports(format))) {
                formats.add(format);
            }
        }
        return Collections.unmodifiableSet(formats);
    }

    private IDocumentSignProvider provider(DocumentSignFormat format) {
        return providers.stream()
                .filter(provider -> provider.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的文档签章格式: " + format));
    }
}
